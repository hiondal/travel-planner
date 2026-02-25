/**
 * Integration 테스트 - 상태 변경 알림 (MNTR, BRIF)
 *
 * 설계 근거:
 *   - 시퀀스: docs/design/sequence/outer/08-상태변경알림.puml
 *   - API 명세: docs/design/api/monitor-service-api.yaml
 *                docs/design/api/briefing-service-api.yaml
 *
 * 흐름 요약:
 *   MNTR 서비스 (상태 변경 감지) -> 이벤트 버스 (PlaceStatusChanged)
 *     -> BRIF 서비스 (브리핑 생성 조건 판단)
 *     -> FCM Push 알림 -> 모바일 -> MNTR 서비스 (상태 상세 조회)
 */
import request from 'supertest';

const MNTR_URL = process.env.MNTR_SERVICE_URL || 'http://localhost:8084';
const BRIF_URL = process.env.BRIF_SERVICE_URL || 'http://localhost:8085';

describe('상태 변경 알림 (MNTR)', () => {
  describe('상태 변경 감지 및 알림 발행', () => {
    /**
     * 브리핑 생성 조건에 해당하면 (방문 예정 15~30분 전)
     * Push 알림이 발송되고 브리핑이 생성된다.
     */
    it('브리핑 생성 조건 해당 시 브리핑 생성 및 Push 발송', async () => {
      const res = await request(BRIF_URL)
        .post('/api/v1/briefings/generate')
        .set('X-Internal-Service-Key', 'test-internal-key')
        .send({
          schedule_item_id: 'si_alert_test',
          place_id: 'place_status_changed',
          user_id: 'usr_01HX_test',
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

    /**
     * 브리핑 생성 조건에 미해당하면
     * 상태 변경 이력만 기록하고 브리핑을 생성하지 않는다.
     * (이 경우 MNTR 서비스의 수집 트리거 응답으로 확인)
     */
    it('브리핑 생성 조건 미해당 시 수집 트리거만 수락', async () => {
      const res = await request(MNTR_URL)
        .post('/api/v1/monitor/collect')
        .set('X-Internal-Service-Key', 'test-internal-key')
        .send({
          triggered_by: 'scheduler',
        })
        .expect(202);

      expect(res.body).toHaveProperty('job_id');
      expect(res.body).toHaveProperty('status');
      expect(res.body.status).toBe('ACCEPTED');
    });
  });

  describe('여행자 알림 확인', () => {
    /**
     * Push 알림 탭 후 상태 상세를 조회하면
     * 영업상태, 혼잡도, 날씨, 이동시간 정보와
     * "대안 보기" 버튼이 포함된 응답을 받는다.
     */
    it('Push 알림 탭 후 상태 상세 조회 시 대안 보기 버튼 포함 반환', async () => {
      const placeId = 'place_status_changed';
      const res = await request(MNTR_URL)
        .get(`/api/v1/badges/${placeId}/detail`)
        .set('Authorization', 'Bearer valid_access_token')
        .expect(200);

      // StatusDetailResponse 스키마 검증
      expect(res.body).toHaveProperty('place_id');
      expect(res.body).toHaveProperty('place_name');
      expect(res.body).toHaveProperty('overall_status');
      expect(['GREEN', 'YELLOW', 'RED', 'GREY']).toContain(res.body.overall_status);

      expect(res.body).toHaveProperty('details');
      expect(res.body.details).toHaveProperty('business_status');
      expect(res.body.details).toHaveProperty('congestion');
      expect(res.body.details).toHaveProperty('weather');
      expect(res.body.details).toHaveProperty('travel_time');

      expect(res.body).toHaveProperty('reason');
      expect(res.body).toHaveProperty('show_alternative_button');
      expect(typeof res.body.show_alternative_button).toBe('boolean');

      expect(res.body).toHaveProperty('updated_at');
    });
  });
});
