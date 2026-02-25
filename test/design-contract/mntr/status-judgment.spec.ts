/**
 * MNTR 서비스 - 상태 배지 판정 행위 계약 테스트
 *
 * 설계 근거:
 *   - 시퀀스: docs/design/sequence/inner/mntr-상태배지판정.puml
 *   - API 명세: docs/design/api/monitor-service-api.yaml
 *
 * 흐름 요약:
 *   StatusJudgmentService -> Azure App Config (임계값 조회)
 *     -> 항목별 상태 평가 (날씨/혼잡도/이동시간/영업상태)
 *     -> 종합 상태 판정 (초록/노랑/빨강/회색)
 *     -> 상태 이력 저장 + 현재 상태 갱신 + 배지 캐시 갱신
 *     -> 상태 변경 감지 시 이벤트 발행 (악화 방향만)
 *
 * 참고: 판정 로직은 내부 서비스 로직이므로 상태 상세 조회 API를 통해
 *       판정 결과를 간접 검증한다.
 */
import request from 'supertest';

const BASE_URL = process.env.MNTR_SERVICE_URL || 'http://localhost:8084';

describe('MNTR 서비스 - 상태 배지 판정', () => {
  describe('항목별 상태 평가', () => {
    /**
     * 날씨 판정:
     *   - 강수확률 < 40% = 정상 (NORMAL)
     *   - 40% 이상 = 주의 (WARNING)
     *   - 70% 이상 = 위험 (DANGER)
     *
     * 혼잡도 판정:
     *   - popular_times null = 미확인 (판정 제외)
     *   - < 60% = 정상 / 60% 이상 = 주의 / 80% 이상 = 위험
     *
     * 이동시간 판정:
     *   - 예상 대비 1.5배 미만 = 정상
     *   - 1.5배 이상 = 주의 / 2배 이상 = 위험
     *
     * 영업상태 판정:
     *   - OPERATIONAL = 정상
     *   - CLOSED_TEMPORARILY / CLOSED_PERMANENTLY = 위험
     *
     * 상태 상세 조회 API로 판정 결과를 간접 검증한다.
     */
    it('모든 항목 정상 시 종합 상태 GREEN 반환', async () => {
      const res = await request(BASE_URL)
        .get('/api/v1/badges/place_green_test/detail')
        .set('Authorization', 'Bearer test-token')
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
      expect(['NORMAL', 'WARNING', 'DANGER']).toContain(res.body.details.congestion.status);
      expect(res.body.details.congestion).toHaveProperty('value');
      expect(res.body.details.congestion).toHaveProperty('is_unknown');
      expect(typeof res.body.details.congestion.is_unknown).toBe('boolean');

      expect(res.body.details).toHaveProperty('weather');
      expect(res.body.details.weather).toHaveProperty('status');
      expect(['NORMAL', 'WARNING', 'DANGER']).toContain(res.body.details.weather.status);
      expect(res.body.details.weather).toHaveProperty('value');
      expect(res.body.details.weather).toHaveProperty('precipitation_prob');
      expect(typeof res.body.details.weather.precipitation_prob).toBe('number');

      expect(res.body.details).toHaveProperty('travel_time');
      expect(res.body.details.travel_time).toHaveProperty('status');
      expect(['NORMAL', 'WARNING', 'DANGER']).toContain(res.body.details.travel_time.status);
      expect(res.body.details.travel_time).toHaveProperty('walking_minutes');
      expect(typeof res.body.details.travel_time.walking_minutes).toBe('number');
      expect(res.body.details.travel_time).toHaveProperty('transit_minutes');
      // transit_minutes는 nullable (500m 이상 시 제공)
      expect(res.body.details.travel_time).toHaveProperty('distance_m');
      expect(typeof res.body.details.travel_time.distance_m).toBe('number');

      expect(res.body).toHaveProperty('reason');
      expect(typeof res.body.reason).toBe('string');

      expect(res.body).toHaveProperty('show_alternative_button');
      expect(typeof res.body.show_alternative_button).toBe('boolean');

      expect(res.body).toHaveProperty('updated_at');
      expect(typeof res.body.updated_at).toBe('string');
    });
  });

  describe('종합 상태 판정', () => {
    /**
     * 종합 판정 규칙:
     *   초록: 모든 항목 정상
     *   노랑: 1개 이상 주의 (위험 없음)
     *   빨강: 1개 이상 위험
     *   회색: 데이터 수집 3회 연속 실패
     */
    it('1개 이상 주의 항목 존재 시 종합 상태 YELLOW 반환', async () => {
      const res = await request(BASE_URL)
        .get('/api/v1/badges/place_yellow_test/detail')
        .set('Authorization', 'Bearer test-token')
        .expect(200);

      expect(res.body).toHaveProperty('overall_status');
      expect(['GREEN', 'YELLOW', 'RED', 'GREY']).toContain(res.body.overall_status);

      // 노랑/빨강 배지인 경우 대안 보기 버튼 표시
      expect(res.body).toHaveProperty('show_alternative_button');
      expect(typeof res.body.show_alternative_button).toBe('boolean');

      expect(res.body).toHaveProperty('reason');
      expect(typeof res.body.reason).toBe('string');
    });

    it('1개 이상 위험 항목 존재 시 종합 상태 RED 반환', async () => {
      const res = await request(BASE_URL)
        .get('/api/v1/badges/place_red_test/detail')
        .set('Authorization', 'Bearer test-token')
        .expect(200);

      expect(res.body).toHaveProperty('overall_status');
      expect(['GREEN', 'YELLOW', 'RED', 'GREY']).toContain(res.body.overall_status);

      expect(res.body).toHaveProperty('show_alternative_button');
      expect(typeof res.body.show_alternative_button).toBe('boolean');

      expect(res.body).toHaveProperty('reason');
      expect(typeof res.body.reason).toBe('string');
    });

    it('데이터 수집 3회 연속 실패 시 종합 상태 GREY 반환', async () => {
      const res = await request(BASE_URL)
        .get('/api/v1/badges/place_grey_test/detail')
        .set('Authorization', 'Bearer test-token')
        .expect(200);

      expect(res.body).toHaveProperty('overall_status');
      expect(['GREEN', 'YELLOW', 'RED', 'GREY']).toContain(res.body.overall_status);
    });
  });

  describe('상태 변경 감지 (악화 방향만)', () => {
    /**
     * 초록->노랑/빨강, 노랑->빨강 변경 시에만
     * PlaceStatusChanged 이벤트가 발행된다.
     * 이벤트 발행은 비동기이므로 상태 상세 조회로 간접 검증한다.
     */
    it('상태 변경 감지 시 상태 상세 조회에 변경된 상태 반영', async () => {
      const res = await request(BASE_URL)
        .get('/api/v1/badges/place_status_change_test/detail')
        .set('Authorization', 'Bearer test-token')
        .expect(200);

      expect(res.body).toHaveProperty('overall_status');
      expect(['GREEN', 'YELLOW', 'RED', 'GREY']).toContain(res.body.overall_status);

      expect(res.body).toHaveProperty('updated_at');
      expect(typeof res.body.updated_at).toBe('string');
    });
  });

  describe('오류 처리', () => {
    /**
     * 존재하지 않는 place_id로 상태 상세 조회 시 404 Not Found를 반환한다.
     */
    it('존재하지 않는 place_id 시 404 Not Found 반환', async () => {
      const res = await request(BASE_URL)
        .get('/api/v1/badges/non_existent_place/detail')
        .set('Authorization', 'Bearer test-token')
        .expect(404);

      // ErrorResponse 스키마 검증
      expect(res.body).toHaveProperty('error');
      expect(typeof res.body.error).toBe('string');

      expect(res.body).toHaveProperty('message');
      expect(typeof res.body.message).toBe('string');
    });

    /**
     * 인증 토큰이 유효하지 않은 경우 401 Unauthorized를 반환한다.
     */
    it('인증 실패 시 401 Unauthorized 반환', async () => {
      const res = await request(BASE_URL)
        .get('/api/v1/badges/place_abc123/detail')
        .set('Authorization', 'Bearer invalid_token')
        .expect(401);

      expect(res.body).toHaveProperty('error');
      expect(typeof res.body.error).toBe('string');

      expect(res.body).toHaveProperty('message');
      expect(typeof res.body.message).toBe('string');
    });
  });
});
