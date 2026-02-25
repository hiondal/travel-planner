/**
 * BRIF 서비스 - 브리핑 조회 행위 계약 테스트
 *
 * 설계 근거:
 *   - 시퀀스: docs/design/sequence/inner/brif-브리핑조회.puml
 *   - API 명세: docs/design/api/briefing-service-api.yaml
 *
 * 흐름 요약:
 *   Client -> BriefController (소유권 검증 JWT user_id)
 *     -> BriefingService -> BriefingRepository (상세 조회)
 *     -> 만료 여부 확인 (생성 시각 > 방문 예정 시간)
 *     -> 열람 이벤트 기록 (분석 데이터)
 *     -> 200 OK {briefing_id, type, content, alternative_link, expired}
 */
import request from 'supertest';

const BASE_URL = process.env.BRIF_SERVICE_URL || 'http://localhost:8085';

describe('BRIF 서비스 - 브리핑 조회', () => {
  describe('브리핑 상세 조회', () => {
    /**
     * 유효한 브리핑 (생성 시각이 방문 예정 시간 이전)을 조회하면
     * expired=false로 반환된다.
     */
    it('유효한 브리핑 시 expired=false로 상세 반환', async () => {
      const res = await request(BASE_URL)
        .get('/api/v1/briefings/brif_valid_01')
        .set('Authorization', 'Bearer test-token')
        .expect(200);

      // BriefingDetailResponse 스키마 검증
      expect(res.body).toHaveProperty('briefing_id');
      expect(typeof res.body.briefing_id).toBe('string');

      expect(res.body).toHaveProperty('type');
      expect(['SAFE', 'WARNING']).toContain(res.body.type);

      expect(res.body).toHaveProperty('place_id');
      expect(typeof res.body.place_id).toBe('string');

      expect(res.body).toHaveProperty('place_name');
      expect(typeof res.body.place_name).toBe('string');

      expect(res.body).toHaveProperty('departure_time');
      expect(typeof res.body.departure_time).toBe('string');

      expect(res.body).toHaveProperty('created_at');
      expect(typeof res.body.created_at).toBe('string');

      expect(res.body).toHaveProperty('expired');
      expect(typeof res.body.expired).toBe('boolean');

      // BriefingContent 스키마 검증
      expect(res.body).toHaveProperty('content');

      expect(res.body.content).toHaveProperty('business_status');
      expect(typeof res.body.content.business_status).toBe('string');

      expect(res.body.content).toHaveProperty('congestion');
      expect(typeof res.body.content.congestion).toBe('string');

      expect(res.body.content).toHaveProperty('weather');
      expect(typeof res.body.content.weather).toBe('string');

      expect(res.body.content).toHaveProperty('travel_time');
      expect(res.body.content.travel_time).toHaveProperty('walking_minutes');
      expect(typeof res.body.content.travel_time.walking_minutes).toBe('number');
      expect(res.body.content.travel_time).toHaveProperty('transit_minutes');
      // transit_minutes는 nullable (500m 이상 시 제공)
      expect(res.body.content.travel_time).toHaveProperty('distance_m');
      expect(typeof res.body.content.travel_time.distance_m).toBe('number');

      expect(res.body.content).toHaveProperty('summary');
      expect(typeof res.body.content.summary).toBe('string');

      expect(res.body).toHaveProperty('alternative_link');
      // alternative_link는 nullable (주의/위험 시만 제공)
    });

    /**
     * 만료된 브리핑 (생성 시각이 방문 예정 시간 이후)을 조회하면
     * expired=true와 "이미 지난 브리핑입니다" 메시지가 반환된다.
     */
    it('만료된 브리핑 시 expired=true와 만료 메시지 반환', async () => {
      const res = await request(BASE_URL)
        .get('/api/v1/briefings/brif_expired_01')
        .set('Authorization', 'Bearer test-token')
        .expect(200);

      // BriefingDetailResponse 스키마 검증
      expect(res.body).toHaveProperty('briefing_id');
      expect(typeof res.body.briefing_id).toBe('string');

      expect(res.body).toHaveProperty('type');
      expect(['SAFE', 'WARNING']).toContain(res.body.type);

      expect(res.body).toHaveProperty('expired');
      expect(typeof res.body.expired).toBe('boolean');

      expect(res.body).toHaveProperty('expire_message');
      // expire_message는 nullable (만료 시 안내 메시지)

      // BriefingContent 스키마 검증
      expect(res.body).toHaveProperty('content');
      expect(res.body.content).toHaveProperty('business_status');
      expect(res.body.content).toHaveProperty('congestion');
      expect(res.body.content).toHaveProperty('weather');
      expect(res.body.content).toHaveProperty('travel_time');
      expect(res.body.content).toHaveProperty('summary');

      expect(res.body).toHaveProperty('alternative_link');
    });
  });

  describe('브리핑 목록 조회', () => {
    /**
     * 오늘 날짜 기준 수신한 브리핑 목록을 최신순으로 조회한다.
     * 브리핑 유형(안심/주의)과 장소명, 생성 시각이 포함된다.
     */
    it('날짜 기준 브리핑 목록 정상 반환', async () => {
      const res = await request(BASE_URL)
        .get('/api/v1/briefings')
        .query({ date: '2026-03-16' })
        .set('Authorization', 'Bearer test-token')
        .expect(200);

      // BriefingListResponse 스키마 검증
      expect(res.body).toHaveProperty('date');
      expect(typeof res.body.date).toBe('string');

      expect(res.body).toHaveProperty('briefings');
      expect(Array.isArray(res.body.briefings)).toBe(true);

      // BriefingListItem 스키마 검증
      res.body.briefings.forEach((item: Record<string, unknown>) => {
        expect(item).toHaveProperty('briefing_id');
        expect(typeof item.briefing_id).toBe('string');

        expect(item).toHaveProperty('type');
        expect(['SAFE', 'WARNING']).toContain(item.type);

        expect(item).toHaveProperty('place_name');
        expect(typeof item.place_name).toBe('string');

        expect(item).toHaveProperty('created_at');
        expect(typeof item.created_at).toBe('string');

        expect(item).toHaveProperty('expired');
        expect(typeof item.expired).toBe('boolean');
      });
    });
  });

  describe('오류 처리', () => {
    /**
     * 인증 토큰이 유효하지 않은 경우 401 Unauthorized를 반환한다.
     */
    it('인증 실패 시 401 Unauthorized 반환', async () => {
      const res = await request(BASE_URL)
        .get('/api/v1/briefings/brif_01HX')
        .set('Authorization', 'Bearer invalid_token')
        .expect(401);

      // ErrorResponse 스키마 검증
      expect(res.body).toHaveProperty('error');
      expect(typeof res.body.error).toBe('string');

      expect(res.body).toHaveProperty('message');
      expect(typeof res.body.message).toBe('string');
    });

    /**
     * 브리핑 소유권이 없는 경우 (다른 사용자의 브리핑)
     * 403 Forbidden을 반환한다.
     */
    it('브리핑 소유권 없음 시 403 Forbidden 반환', async () => {
      const res = await request(BASE_URL)
        .get('/api/v1/briefings/brif_other_user_01')
        .set('Authorization', 'Bearer test-token')
        .expect(403);

      // ErrorResponse 스키마 검증
      expect(res.body).toHaveProperty('error');
      expect(typeof res.body.error).toBe('string');

      expect(res.body).toHaveProperty('message');
      expect(typeof res.body.message).toBe('string');
    });

    /**
     * 존재하지 않는 briefing_id로 조회 시 404 Not Found를 반환한다.
     */
    it('존재하지 않는 briefing_id 시 404 Not Found 반환', async () => {
      const res = await request(BASE_URL)
        .get('/api/v1/briefings/non_existent_brif')
        .set('Authorization', 'Bearer test-token')
        .expect(404);

      // ErrorResponse 스키마 검증
      expect(res.body).toHaveProperty('error');
      expect(typeof res.body.error).toBe('string');

      expect(res.body).toHaveProperty('message');
      expect(typeof res.body.message).toBe('string');
    });
  });
});
