/**
 * Integration 테스트 - 상태 배지 조회 (MNTR)
 *
 * 설계 근거:
 *   - 시퀀스: docs/design/sequence/outer/06-상태배지조회.puml
 *   - API 명세: docs/design/api/monitor-service-api.yaml
 *
 * 흐름 요약:
 *   모바일 -> MNTR 서비스 (배지 목록 조회, 캐시 기반)
 *     -> 배지 탭 시 MNTR 서비스 (상태 상세 조회)
 */
import request from 'supertest';

const MNTR_URL = process.env.MNTR_SERVICE_URL || 'http://localhost:8084';

describe('상태 배지 조회 (MNTR)', () => {
  describe('상태 배지 목록 조회', () => {
    /**
     * 배지 캐시가 TTL 10분 이내로 존재하면
     * 초록+체크/노랑+느낌표/빨강+X/회색+물음표 형태로 반환한다.
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
     * 캐시가 만료되었으면 DB에서 최신 상태를 조회 후
     * 캐시를 갱신하고 배지 상태를 반환한다.
     */
    it('캐시 MISS 시 DB 조회 및 캐시 갱신 후 배지 상태 목록 반환', async () => {
      const res = await request(MNTR_URL)
        .get('/api/v1/badges')
        .query({ place_ids: 'place_abc123,place_def456' })
        .set('Authorization', 'Bearer valid_access_token')
        .expect(200);

      expect(res.body).toHaveProperty('badges');
      expect(Array.isArray(res.body.badges)).toBe(true);
    });
  });

  describe('상태 상세 조회', () => {
    /**
     * 노랑 또는 빨강 배지를 탭하면
     * 영업상태, 혼잡도, 날씨, 이동시간(도보/대중교통), 판정사유를
     * 포함한 상세 정보를 반환하며 "대안 보기" 버튼이 포함된다.
     */
    it('배지 탭 시 상태 상세 정보 및 대안 보기 버튼 포함 반환', async () => {
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

      // StatusDetails 스키마 검증
      expect(res.body).toHaveProperty('details');

      expect(res.body.details).toHaveProperty('business_status');
      expect(res.body.details.business_status).toHaveProperty('status');
      expect(['NORMAL', 'WARNING', 'DANGER']).toContain(res.body.details.business_status.status);
      expect(res.body.details.business_status).toHaveProperty('value');

      expect(res.body.details).toHaveProperty('congestion');
      expect(res.body.details.congestion).toHaveProperty('status');
      expect(res.body.details.congestion).toHaveProperty('value');
      expect(res.body.details.congestion).toHaveProperty('is_unknown');

      expect(res.body.details).toHaveProperty('weather');
      expect(res.body.details.weather).toHaveProperty('status');
      expect(res.body.details.weather).toHaveProperty('value');
      expect(res.body.details.weather).toHaveProperty('precipitation_prob');

      expect(res.body.details).toHaveProperty('travel_time');
      expect(res.body.details.travel_time).toHaveProperty('status');
      expect(res.body.details.travel_time).toHaveProperty('walking_minutes');
      expect(res.body.details.travel_time).toHaveProperty('distance_m');

      expect(res.body).toHaveProperty('reason');
      expect(typeof res.body.reason).toBe('string');

      expect(res.body).toHaveProperty('show_alternative_button');
      expect(typeof res.body.show_alternative_button).toBe('boolean');

      expect(res.body).toHaveProperty('updated_at');
    });
  });
});
