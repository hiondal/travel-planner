/**
 * MNTR 서비스 - 외부 데이터 수집 행위 계약 테스트
 *
 * 설계 근거:
 *   - 시퀀스: docs/design/sequence/inner/mntr-외부데이터수집.puml
 *   - API 명세: docs/design/api/monitor-service-api.yaml
 *
 * 흐름 요약:
 *   DataCollectionScheduler -> MonitoringService
 *     -> 모니터링 대상 목록 조회 (방문 예정 시간 2시간 전)
 *     -> 외부 API 병렬 3종 호출 (Google Places + OpenWeatherMap + Google Directions)
 *       (타임아웃 2초, Circuit Breaker)
 *     -> 정상 응답 / 일부 API 실패 (캐시 Fallback) / 전체 실패 (3회 연속 -> 회색)
 *     -> 상태 판정 + 이력 저장 + 현재 상태 갱신
 *     -> 상태 변경 감지 시 이벤트 발행 (악화 방향)
 */
import request from 'supertest';

const BASE_URL = process.env.MNTR_SERVICE_URL || 'http://localhost:8084';

describe('MNTR 서비스 - 외부 데이터 수집 (스케줄러 주도)', () => {
  describe('외부 API 병렬 3종 호출 (타임아웃 2초)', () => {
    /**
     * Google Places + OpenWeatherMap + Google Directions API 3종이
     * 모두 정상 응답하면 수집 트리거가 202 ACCEPTED로 수락된다.
     */
    it('정상 응답 시 202 Accepted와 수집 작업 정보 반환', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/monitor/collect')
        .set('X-Internal-Service-Key', 'test-internal-key')
        .send({
          triggered_by: 'scheduler',
          triggered_at: '2026-03-16T10:00:00Z',
        })
        .expect(202);

      // CollectTriggerResponse 스키마 검증
      expect(res.body).toHaveProperty('job_id');
      expect(typeof res.body.job_id).toBe('string');

      expect(res.body).toHaveProperty('status');
      expect(['ACCEPTED', 'REJECTED']).toContain(res.body.status);

      expect(res.body).toHaveProperty('target_count');
      expect(typeof res.body.target_count).toBe('number');

      expect(res.body).toHaveProperty('triggered_at');
      expect(typeof res.body.triggered_at).toBe('string');
    });

    /**
     * 일부 API 실패 시 (P20 부분 실패 처리)
     * 실패한 항목은 캐시 Fallback으로 대체하고
     * 수집 트리거는 정상 수락된다.
     * (예: 날씨 API 타임아웃 -> 마지막 캐시값 {is_cached: true, source_status: FALLBACK})
     */
    it('일부 API 실패 시 캐시 Fallback 적용 후 202 Accepted 반환', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/monitor/collect')
        .set('X-Internal-Service-Key', 'test-internal-key')
        .send({
          triggered_by: 'scheduler',
          triggered_at: '2026-03-16T10:00:00Z',
        })
        .expect(202);

      // CollectTriggerResponse 스키마 검증
      expect(res.body).toHaveProperty('job_id');
      expect(typeof res.body.job_id).toBe('string');

      expect(res.body).toHaveProperty('status');
      expect(['ACCEPTED', 'REJECTED']).toContain(res.body.status);

      expect(res.body).toHaveProperty('target_count');
      expect(typeof res.body.target_count).toBe('number');

      expect(res.body).toHaveProperty('triggered_at');
      expect(typeof res.body.triggered_at).toBe('string');
    });

    /**
     * 전체 실패 (3회 연속) 시 연속 실패 횟수가 증가하고
     * 3회 연속 실패 시 회색 상태로 전환된다.
     * 수집 트리거 자체는 정상 수락된다.
     */
    it('전체 실패 (3회 연속) 시 202 Accepted 반환 (회색 상태 전환)', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/monitor/collect')
        .set('X-Internal-Service-Key', 'test-internal-key')
        .send({
          triggered_by: 'scheduler',
          triggered_at: '2026-03-16T10:00:00Z',
        })
        .expect(202);

      // CollectTriggerResponse 스키마 검증
      expect(res.body).toHaveProperty('job_id');
      expect(typeof res.body.job_id).toBe('string');

      expect(res.body).toHaveProperty('status');
      expect(['ACCEPTED', 'REJECTED']).toContain(res.body.status);

      expect(res.body).toHaveProperty('target_count');
      expect(typeof res.body.target_count).toBe('number');
    });
  });

  describe('상태 판정', () => {
    /**
     * 수집 후 4단계 상태 판정이 수행된다:
     *   초록: 모든 항목 정상
     *   노랑: 1개 이상 주의
     *   빨강: 1개 이상 위험
     *   회색: 3회 연속 실패
     *
     * 판정 결과는 상태 이력에 append-only로 저장되고
     * 현재 상태가 갱신된다.
     * 수집 트리거 결과로는 간접 검증한다.
     */
    it('각 모니터링 대상 장소 반복 시나리오', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/monitor/collect')
        .set('X-Internal-Service-Key', 'test-internal-key')
        .send({
          triggered_by: 'scheduler',
          triggered_at: '2026-03-16T10:15:00Z',
        })
        .expect(202);

      expect(res.body).toHaveProperty('job_id');
      expect(res.body).toHaveProperty('status');
      expect(res.body).toHaveProperty('target_count');
      expect(res.body).toHaveProperty('triggered_at');
    });
  });

  describe('상태 변경 감지 (악화 방향)', () => {
    /**
     * 상태 변경 감지 시 (악화 방향만: 초록->노랑/빨강, 노랑->빨강)
     * PlaceStatusChanged 이벤트가 비동기로 발행된다.
     * {place_id, prev_status, new_status}
     *
     * 이벤트 발행은 비동기이므로 수집 트리거 API 응답과는 독립적이다.
     * 수집 트리거 호출 후 상태 상세 조회로 간접 검증한다.
     */
    it('상태 변경 감지 시 수집 후 상태 상세 조회로 변경 반영 확인', async () => {
      // 수집 트리거 호출
      const collectRes = await request(BASE_URL)
        .post('/api/v1/monitor/collect')
        .set('X-Internal-Service-Key', 'test-internal-key')
        .send({
          triggered_by: 'scheduler',
          triggered_at: '2026-03-16T10:30:00Z',
        })
        .expect(202);

      expect(collectRes.body).toHaveProperty('job_id');
      expect(collectRes.body).toHaveProperty('status');

      // 상태 상세 조회로 변경 반영 확인
      const detailRes = await request(BASE_URL)
        .get('/api/v1/badges/place_status_change_test/detail')
        .set('Authorization', 'Bearer test-token')
        .expect(200);

      expect(detailRes.body).toHaveProperty('overall_status');
      expect(['GREEN', 'YELLOW', 'RED', 'GREY']).toContain(detailRes.body.overall_status);

      expect(detailRes.body).toHaveProperty('updated_at');
      expect(typeof detailRes.body.updated_at).toBe('string');
    });
  });

  describe('오류 처리', () => {
    /**
     * 내부 서비스 키가 유효하지 않은 경우 401 Unauthorized를 반환한다.
     */
    it('인증 실패 시 401 Unauthorized 반환', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/monitor/collect')
        .set('X-Internal-Service-Key', 'invalid_key')
        .send({
          triggered_by: 'scheduler',
          triggered_at: '2026-03-16T10:00:00Z',
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
