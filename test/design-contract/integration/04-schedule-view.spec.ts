/**
 * Integration 테스트 - 일정표 조회 (SCHD, MNTR)
 *
 * 설계 근거:
 *   - 시퀀스: docs/design/sequence/outer/04-일정표조회.puml
 *   - API 명세: docs/design/api/schedule-service-api.yaml
 *                docs/design/api/monitor-service-api.yaml
 *
 * 흐름 요약:
 *   모바일 -> SCHD 서비스 (일정표 조회) -> MNTR 서비스 (배지 일괄 조회)
 *     -> 배지 탭 시 MNTR 서비스 (상태 상세 조회)
 */
import request from 'supertest';

const SCHD_URL = process.env.SCHD_SERVICE_URL || 'http://localhost:8082';
const MNTR_URL = process.env.MNTR_SERVICE_URL || 'http://localhost:8084';

describe('일정표 조회 - 상태 배지 포함 (SCHD, MNTR)', () => {
  const tripId = 'trip_01HX_test';

  describe('일정표 조회', () => {
    /**
     * 일정표 화면 진입 시 SCHD 서비스에서 일정 장소 목록을 조회한다.
     */
    it('일정표 조회 시 일정 장소 목록 200 OK 반환', async () => {
      const res = await request(SCHD_URL)
        .get(`/api/v1/trips/${tripId}/schedule`)
        .set('Authorization', 'Bearer valid_access_token')
        .expect(200);

      // ScheduleResponse 스키마 검증
      expect(res.body).toHaveProperty('trip_id');
      expect(res.body).toHaveProperty('name');
      expect(res.body).toHaveProperty('city');
      expect(res.body).toHaveProperty('schedule_items');
      expect(Array.isArray(res.body.schedule_items)).toBe(true);

      if (res.body.schedule_items.length > 0) {
        const item = res.body.schedule_items[0];
        expect(item).toHaveProperty('schedule_item_id');
        expect(item).toHaveProperty('place_id');
        expect(item).toHaveProperty('place_name');
        expect(item).toHaveProperty('visit_datetime');
        expect(item).toHaveProperty('timezone');
        expect(item).toHaveProperty('order');
        expect(item).toHaveProperty('outside_business_hours');
      }
    });
  });

  describe('상태 배지 일괄 조회', () => {
    /**
     * 배지 상태 캐시가 TTL 10분 이내로 존재하면
     * 캐시에서 즉시 배지 상태를 반환한다.
     */
    it('캐시 HIT (TTL 10분 이내) 시 배지 상태 목록 반환', async () => {
      const res = await request(MNTR_URL)
        .get('/api/v1/badges')
        .query({ place_ids: 'place_abc123,place_def456,place_xyz789' })
        .set('Authorization', 'Bearer valid_access_token')
        .expect(200);

      // BadgeListResponse 스키마 검증
      expect(res.body).toHaveProperty('badges');
      expect(Array.isArray(res.body.badges)).toBe(true);

      if (res.body.badges.length > 0) {
        const badge = res.body.badges[0];
        expect(badge).toHaveProperty('place_id');
        expect(typeof badge.place_id).toBe('string');

        expect(badge).toHaveProperty('status');
        expect(['GREEN', 'YELLOW', 'RED', 'GREY']).toContain(badge.status);

        expect(badge).toHaveProperty('icon');
        expect(['CHECK', 'EXCLAMATION', 'X', 'QUESTION']).toContain(badge.icon);

        expect(badge).toHaveProperty('color_hex');
        expect(typeof badge.color_hex).toBe('string');

        expect(badge).toHaveProperty('updated_at');
        expect(typeof badge.updated_at).toBe('string');
      }
    });

    /**
     * 캐시가 만료되었거나 없는 경우
     * DB에서 직접 조회 후 캐시를 갱신하여 반환한다.
     */
    it('캐시 MISS 시 DB 조회 후 배지 상태 목록 반환', async () => {
      const res = await request(MNTR_URL)
        .get('/api/v1/badges')
        .query({ place_ids: 'place_abc123,place_def456' })
        .set('Authorization', 'Bearer valid_access_token')
        .expect(200);

      expect(res.body).toHaveProperty('badges');
      expect(Array.isArray(res.body.badges)).toBe(true);
    });
  });

  describe('배지 탭 - 상태 상세 조회', () => {
    /**
     * 노랑/빨강 배지를 탭하면
     * 영업상태, 날씨, 혼잡도, 이동시간, 판정사유를 포함한
     * 상태 상세 정보를 반환한다.
     */
    it('노랑/빨강 배지 탭 시 상태 상세 정보 반환', async () => {
      const placeId = 'place_def456';
      const res = await request(MNTR_URL)
        .get(`/api/v1/badges/${placeId}/detail`)
        .set('Authorization', 'Bearer valid_access_token')
        .expect(200);

      // StatusDetailResponse 스키마 검증
      expect(res.body).toHaveProperty('place_id');
      expect(typeof res.body.place_id).toBe('string');

      expect(res.body).toHaveProperty('place_name');
      expect(typeof res.body.place_name).toBe('string');

      expect(res.body).toHaveProperty('overall_status');
      expect(['GREEN', 'YELLOW', 'RED', 'GREY']).toContain(res.body.overall_status);

      expect(res.body).toHaveProperty('details');
      expect(res.body.details).toHaveProperty('business_status');
      expect(res.body.details).toHaveProperty('congestion');
      expect(res.body.details).toHaveProperty('weather');
      expect(res.body.details).toHaveProperty('travel_time');

      expect(res.body).toHaveProperty('reason');
      expect(typeof res.body.reason).toBe('string');

      expect(res.body).toHaveProperty('show_alternative_button');
      expect(typeof res.body.show_alternative_button).toBe('boolean');

      expect(res.body).toHaveProperty('updated_at');
      expect(typeof res.body.updated_at).toBe('string');
    });
  });
});
