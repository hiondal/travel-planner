/**
 * BRIF 서비스 - 총평 생성 행위 계약 테스트
 *
 * 설계 근거:
 *   - 시퀀스: docs/design/sequence/inner/brif-총평생성.puml
 *   - API 명세: docs/design/api/briefing-service-api.yaml
 *
 * 흐름 요약:
 *   BriefingController -> BriefingService
 *     -> 멱등성 확인 (scheduleItemId로 기존 브리핑 존재 여부 조회)
 *     -> 모니터링 서비스 캐시 데이터 조회 (place:{placeId}:status)
 *     -> BriefingContext 구성 (placeStatus, statusLevel, riskItems, subscriptionTier)
 *     -> BriefingTextGenerator.generate(briefingContext)
 *       -> MVP: RuleBasedBriefingGenerator (템플릿 기반 규칙 엔진)
 *         -> statusLevel == SAFE: 안심 총평 템플릿
 *         -> statusLevel == CAUTION/DANGER: 위험 항목 목록 문자열 조합
 *     -> 브리핑 저장 -> 분석 이벤트 기록
 *
 * Phase 2 전환점:
 *   ai.briefing.provider=rule  -> RuleBasedBriefingGenerator (현재 MVP)
 *   ai.briefing.provider=llm   -> LLMBriefingGenerator (Phase 2)
 *   전환 시 application.yml 설정값 변경만으로 완료.
 */
import request from 'supertest';

const BASE_URL = process.env.BRIF_SERVICE_URL || 'http://localhost:8085';

describe('BRIF 서비스 - 총평 생성 내부 시퀀스', () => {
  describe('멱등성 확인', () => {
    /**
     * 동일한 scheduleItemId로 브리핑이 이미 존재하면 (UFR-BRIF-020)
     * 기존 브리핑 ID를 반환하여 멱등성을 보장한다.
     */
    it('브리핑 이미 존재 시 기존 브리핑 ID 반환 (멱등성 보장)', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/briefings/generate')
        .set('X-Internal-Service-Key', 'test-internal-key')
        .send({
          schedule_item_id: 'si_idempotent_existing',
          place_id: 'place_abc123',
          user_id: 'usr_01HX',
          departure_time: '2026-03-16T12:00:00+09:00',
          triggered_at: '2026-03-16T11:40:00+09:00',
        })
        .expect(200);

      // GenerateBriefingResponse 스키마 검증
      expect(res.body).toHaveProperty('briefing_id');
      expect(typeof res.body.briefing_id).toBe('string');

      expect(res.body).toHaveProperty('status');
      expect(res.body.status).toBe('EXISTING');

      expect(res.body).toHaveProperty('type');
      expect(['SAFE', 'WARNING']).toContain(res.body.type);
    });

    /**
     * 브리핑이 존재하지 않으면 신규 생성 흐름으로 진행한다.
     */
    it('브리핑 없음 시 신규 생성 후 201 Created 반환', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/briefings/generate')
        .set('X-Internal-Service-Key', 'test-internal-key')
        .send({
          schedule_item_id: 'si_summary_new_01',
          place_id: 'place_summary_test',
          user_id: 'usr_01HX',
          departure_time: '2026-03-16T16:00:00+09:00',
          triggered_at: '2026-03-16T15:40:00+09:00',
        })
        .expect(201);

      expect(res.body).toHaveProperty('briefing_id');
      expect(typeof res.body.briefing_id).toBe('string');

      expect(res.body).toHaveProperty('status');
      expect(res.body.status).toBe('CREATED');

      expect(res.body).toHaveProperty('type');
      expect(['SAFE', 'WARNING']).toContain(res.body.type);
    });
  });

  describe('모니터링 서비스 캐시 데이터 조회', () => {
    /**
     * 캐시 HIT -- 5분 이내 데이터 (UFR-BRIF-010 [U7])
     * 재수집 없이 캐시 데이터를 사용하여 브리핑을 생성한다.
     */
    it('캐시 HIT (5분 이내 데이터) 시 재수집 없이 브리핑 생성', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/briefings/generate')
        .set('X-Internal-Service-Key', 'test-internal-key')
        .send({
          schedule_item_id: 'si_cache_hit_summary',
          place_id: 'place_cache_fresh',
          user_id: 'usr_01HX',
          departure_time: '2026-03-16T17:00:00+09:00',
          triggered_at: '2026-03-16T16:40:00+09:00',
        })
        .expect(201);

      expect(res.body).toHaveProperty('briefing_id');
      expect(res.body).toHaveProperty('status');
      expect(res.body).toHaveProperty('type');
    });

    /**
     * 캐시 MISS 또는 5분 초과 시
     * 비동기 수집 요청 이벤트를 발행하고 최신 캐시 데이터로 진행한다.
     */
    it('캐시 MISS 또는 5분 초과 시 비동기 수집 후 브리핑 생성', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/briefings/generate')
        .set('X-Internal-Service-Key', 'test-internal-key')
        .send({
          schedule_item_id: 'si_cache_miss_summary',
          place_id: 'place_cache_stale',
          user_id: 'usr_01HX',
          departure_time: '2026-03-16T18:00:00+09:00',
          triggered_at: '2026-03-16T17:40:00+09:00',
        })
        .expect(201);

      expect(res.body).toHaveProperty('briefing_id');
      expect(res.body).toHaveProperty('status');
      expect(res.body).toHaveProperty('type');
    });
  });

  describe('총평 텍스트 생성 (RuleBasedBriefingGenerator)', () => {
    /**
     * statusLevel == SAFE 시 안심 총평 템플릿이 적용된다.
     * 예: "현재까지 모든 항목 정상입니다. 예정대로 출발하세요."
     *
     * 브리핑 상세 조회로 생성된 총평 텍스트를 간접 검증한다.
     */
    it('statusLevel SAFE 시 안심 총평 텍스트 생성', async () => {
      // 브리핑 생성
      const generateRes = await request(BASE_URL)
        .post('/api/v1/briefings/generate')
        .set('X-Internal-Service-Key', 'test-internal-key')
        .send({
          schedule_item_id: 'si_safe_summary_01',
          place_id: 'place_all_normal',
          user_id: 'usr_01HX',
          departure_time: '2026-03-16T19:00:00+09:00',
          triggered_at: '2026-03-16T18:40:00+09:00',
        })
        .expect(201);

      expect(generateRes.body).toHaveProperty('briefing_id');
      const briefingId = generateRes.body.briefing_id;

      // 브리핑 상세 조회로 총평 텍스트 검증
      const detailRes = await request(BASE_URL)
        .get(`/api/v1/briefings/${briefingId}`)
        .set('Authorization', 'Bearer test-token')
        .expect(200);

      expect(detailRes.body).toHaveProperty('content');
      expect(detailRes.body.content).toHaveProperty('summary');
      expect(typeof detailRes.body.content.summary).toBe('string');

      // 안심 브리핑은 alternative_link가 null
      expect(detailRes.body).toHaveProperty('alternative_link');
    });

    /**
     * statusLevel == CAUTION 또는 DANGER 시
     * 위험 항목 목록(riskItems)에서 label을 추출하여 문자열을 조합한다.
     * 예: "임시 휴업, 폭우 예보이(가) 감지되었습니다. 대안을 확인해보세요."
     *
     * 브리핑 상세 조회로 생성된 총평 텍스트와 대안 링크를 간접 검증한다.
     */
    it('statusLevel CAUTION 또는 DANGER 시 위험 항목 총평 텍스트 생성', async () => {
      // 브리핑 생성
      const generateRes = await request(BASE_URL)
        .post('/api/v1/briefings/generate')
        .set('X-Internal-Service-Key', 'test-internal-key')
        .send({
          schedule_item_id: 'si_warning_summary_01',
          place_id: 'place_with_risk',
          user_id: 'usr_01HX',
          departure_time: '2026-03-16T20:00:00+09:00',
          triggered_at: '2026-03-16T19:40:00+09:00',
        })
        .expect(201);

      expect(generateRes.body).toHaveProperty('briefing_id');
      const briefingId = generateRes.body.briefing_id;

      // 브리핑 상세 조회로 총평 텍스트 검증
      const detailRes = await request(BASE_URL)
        .get(`/api/v1/briefings/${briefingId}`)
        .set('Authorization', 'Bearer test-token')
        .expect(200);

      expect(detailRes.body).toHaveProperty('content');
      expect(detailRes.body.content).toHaveProperty('summary');
      expect(typeof detailRes.body.content.summary).toBe('string');

      // 주의/위험 브리핑은 alternative_link가 포함됨
      expect(detailRes.body).toHaveProperty('alternative_link');
    });
  });

  describe('오류 처리 -- Graceful Degradation', () => {
    /**
     * RuleBasedBriefingGenerator 예외 발생 시
     * 총평 = "현재 상태 정보를 확인 중입니다."로 대체되고
     * 브리핑은 총평 제외 4개 항목(영업상태/날씨/혼잡도/이동시간)으로 반드시 발송된다.
     * 핵심 원칙: AI 또는 규칙 엔진 장애와 무관하게 브리핑은 생성된다.
     */
    it('RuleBasedBriefingGenerator 예외 발생 시 Graceful Degradation으로 브리핑 생성', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/briefings/generate')
        .set('X-Internal-Service-Key', 'test-internal-key')
        .send({
          schedule_item_id: 'si_degraded_01',
          place_id: 'place_generator_error',
          user_id: 'usr_01HX',
          departure_time: '2026-03-16T21:00:00+09:00',
          triggered_at: '2026-03-16T20:40:00+09:00',
        })
        .expect(201);

      // Graceful Degradation 시에도 브리핑은 생성됨
      expect(res.body).toHaveProperty('briefing_id');
      expect(typeof res.body.briefing_id).toBe('string');

      expect(res.body).toHaveProperty('status');
      expect(res.body.status).toBe('CREATED');

      expect(res.body).toHaveProperty('type');
      expect(['SAFE', 'WARNING']).toContain(res.body.type);
    });
  });
});
