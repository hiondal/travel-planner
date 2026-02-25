/**
 * BRIF 서비스 - 브리핑 생성 행위 계약 테스트
 *
 * 설계 근거:
 *   - 시퀀스: docs/design/sequence/inner/brif-브리핑생성.puml
 *   - API 명세: docs/design/api/briefing-service-api.yaml
 *
 * 흐름 요약:
 *   BriefingScheduler -> BriefingService
 *     -> 멱등성 확인 (place_id + departure_time 해시)
 *     -> 구독 티어 확인 (Free: 1일 1회)
 *     -> 최신 수집 데이터 조회 (캐시 Read-Through)
 *     -> 안심/주의 분기 판단 -> 총평 생성 (BriefingTextGenerator)
 *     -> 브리핑 저장 -> BriefingCreated 이벤트 발행 (비동기)
 */
import request from 'supertest';

const BASE_URL = process.env.BRIF_SERVICE_URL || 'http://localhost:8085';

describe('BRIF 서비스 - 브리핑 생성', () => {
  describe('멱등성 확인', () => {
    /**
     * 동일한 place_id + departure_time 해시로 이미 생성된 브리핑이 존재하면
     * 기존 briefing_id를 200 OK로 반환한다 (P21 멱등성 보장).
     */
    it('이미 생성된 브리핑 존재 시 200 OK와 기존 briefing_id 반환', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/briefings/generate')
        .set('Authorization', 'Bearer test-token')
        .set('X-Internal-Service-Key', 'test-internal-key')
        .send({
          schedule_item_id: 'si_existing_01',
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
  });

  describe('구독 티어 확인 (Free: 1일 1회)', () => {
    /**
     * Free 티어 사용자가 일일 한도를 초과하면 (P19)
     * briefing_logs에 SKIPPED 상태로 기록되고
     * 422 Unprocessable Entity와 FREE_TIER_LIMIT_EXCEEDED 사유를 반환한다.
     */
    it('Free 티어 한도 초과 시 422와 SKIPPED 상태 반환', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/briefings/generate')
        .set('Authorization', 'Bearer test-token')
        .set('X-Internal-Service-Key', 'test-internal-key')
        .send({
          schedule_item_id: 'si_free_limit_02',
          place_id: 'place_free_limit',
          user_id: 'usr_free_tier',
          departure_time: '2026-03-16T14:00:00+09:00',
          triggered_at: '2026-03-16T13:40:00+09:00',
        })
        .expect(422);

      // BriefingSkippedResponse 스키마 검증
      expect(res.body).toHaveProperty('status');
      expect(res.body.status).toBe('SKIPPED');

      expect(res.body).toHaveProperty('reason');
      expect(res.body.reason).toBe('FREE_TIER_LIMIT_EXCEEDED');

      expect(res.body).toHaveProperty('message');
      expect(typeof res.body.message).toBe('string');
    });

    /**
     * 생성 가능 (한도 이내 또는 유료 티어) 시
     * 브리핑이 정상 생성되어 201 Created를 반환한다.
     */
    it('생성 가능 (한도 이내 또는 유료 티어) 시 201 Created 반환', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/briefings/generate')
        .set('Authorization', 'Bearer test-token')
        .set('X-Internal-Service-Key', 'test-internal-key')
        .send({
          schedule_item_id: 'si_new_01',
          place_id: 'place_abc123',
          user_id: 'usr_pro_tier',
          departure_time: '2026-03-16T12:00:00+09:00',
          triggered_at: '2026-03-16T11:40:00+09:00',
        })
        .expect(201);

      // GenerateBriefingResponse 스키마 검증
      expect(res.body).toHaveProperty('briefing_id');
      expect(typeof res.body.briefing_id).toBe('string');

      expect(res.body).toHaveProperty('status');
      expect(res.body.status).toBe('CREATED');

      expect(res.body).toHaveProperty('type');
      expect(['SAFE', 'WARNING']).toContain(res.body.type);
    });
  });

  describe('최신 수집 데이터 조회 (캐시 Read-Through)', () => {
    /**
     * 캐시 5분 이내 데이터가 존재하면 수집 데이터가 바로 반환되고
     * 브리핑이 정상 생성된다.
     */
    it('캐시 5분 이내 시 수집 데이터로 브리핑 생성 후 201 Created 반환', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/briefings/generate')
        .set('Authorization', 'Bearer test-token')
        .set('X-Internal-Service-Key', 'test-internal-key')
        .send({
          schedule_item_id: 'si_cache_hit_01',
          place_id: 'place_cache_hit',
          user_id: 'usr_01HX',
          departure_time: '2026-03-16T13:00:00+09:00',
          triggered_at: '2026-03-16T12:40:00+09:00',
        })
        .expect(201);

      expect(res.body).toHaveProperty('briefing_id');
      expect(res.body).toHaveProperty('status');
      expect(res.body).toHaveProperty('type');
    });

    /**
     * 캐시 5분 초과 시 비동기 수집 후 반환되며
     * 브리핑은 정상 생성된다.
     */
    it('캐시 5분 초과 시 비동기 수집 후 브리핑 생성 후 201 Created 반환', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/briefings/generate')
        .set('Authorization', 'Bearer test-token')
        .set('X-Internal-Service-Key', 'test-internal-key')
        .send({
          schedule_item_id: 'si_cache_miss_01',
          place_id: 'place_cache_miss',
          user_id: 'usr_01HX',
          departure_time: '2026-03-16T15:00:00+09:00',
          triggered_at: '2026-03-16T14:40:00+09:00',
        })
        .expect(201);

      expect(res.body).toHaveProperty('briefing_id');
      expect(res.body).toHaveProperty('status');
      expect(res.body).toHaveProperty('type');
    });
  });

  describe('오류 처리', () => {
    /**
     * 내부 서비스 키가 유효하지 않은 경우 401 Unauthorized를 반환한다.
     */
    it('인증 실패 시 401 Unauthorized 반환', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/briefings/generate')
        .set('X-Internal-Service-Key', 'invalid_key')
        .send({
          schedule_item_id: 'si_01HX',
          place_id: 'place_abc123',
          user_id: 'usr_01HX',
          departure_time: '2026-03-16T12:00:00+09:00',
          triggered_at: '2026-03-16T11:40:00+09:00',
        })
        .expect(401);

      // ErrorResponse 스키마 검증
      expect(res.body).toHaveProperty('error');
      expect(typeof res.body.error).toBe('string');

      expect(res.body).toHaveProperty('message');
      expect(typeof res.body.message).toBe('string');
    });
  });
});
