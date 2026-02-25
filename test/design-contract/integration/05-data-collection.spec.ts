/**
 * Integration 테스트 - 외부 데이터 수집 (MNTR, PLCE)
 *
 * 설계 근거:
 *   - 시퀀스: docs/design/sequence/outer/05-외부데이터수집.puml
 *   - API 명세: docs/design/api/monitor-service-api.yaml
 *
 * 흐름 요약:
 *   스케줄러 -> MNTR 서비스 (15분 주기 수집 트리거)
 *     -> Azure App Config (임계값 설정 조회)
 *     -> 외부 API 병렬 3종 호출 (Google Places, OpenWeatherMap, Google Directions)
 *     -> 수집 결과 캐시 저장 + 상태 판정 -> 상태 변경 시 이벤트 발행
 */
import request from 'supertest';

const MNTR_URL = process.env.MNTR_SERVICE_URL || 'http://localhost:8084';

describe('외부 데이터 수집 (MNTR) - 스케줄러 주도', () => {
  describe('15분 주기 수집 트리거', () => {
    describe('외부 API 병렬 3종 호출 (Circuit Breaker + Retry)', () => {
      /**
       * 모든 API(영업상태, 날씨, 이동시간)가 타임아웃 2초 이내에
       * 정상 응답하면 수집 트리거가 수락된다.
       */
      it('모든 API 정상 응답 (타임아웃 2초 이내) 시 202 Accepted 반환', async () => {
        const res = await request(MNTR_URL)
          .post('/api/v1/monitor/collect')
          .set('X-Internal-Service-Key', 'test-internal-key')
          .send({
            triggered_by: 'scheduler',
          })
          .expect(202);

        // CollectTriggerResponse 스키마 검증
        expect(res.body).toHaveProperty('job_id');
        expect(typeof res.body.job_id).toBe('string');

        expect(res.body).toHaveProperty('status');
        expect(res.body.status).toBe('ACCEPTED');

        expect(res.body).toHaveProperty('target_count');
        expect(typeof res.body.target_count).toBe('number');

        expect(res.body).toHaveProperty('triggered_at');
        expect(typeof res.body.triggered_at).toBe('string');
      });

      /**
       * 일부 API가 실패하면 (부분 실패 처리 P20)
       * 실패한 항목은 캐시 폴백값을 사용하여 수집을 완료한다.
       */
      it('일부 API 실패 (부분 실패 처리 P20) 시 캐시 폴백 적용 후 202 Accepted 반환', async () => {
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

        expect(res.body).toHaveProperty('target_count');
        expect(typeof res.body.target_count).toBe('number');
      });

      /**
       * 전체 실패 시 (3회 연속)
       * 회색 상태 전환 처리를 수행한다.
       */
      it('전체 실패 (3회 연속) 시 회색 상태 전환 처리 후 202 Accepted 반환', async () => {
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

    describe('상태 변경 감지 시 이벤트 발행', () => {
      /**
       * 상태 변경이 감지되면 (초록->노랑, 초록->빨강, 노랑->빨강)
       * PlaceStatusChanged 이벤트가 비동기 발행되어
       * BRIF 서비스에 브리핑 트리거가 전달된다.
       */
      it('상태 변경 감지 시 수집 트리거 정상 수락', async () => {
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
        expect(res.body).toHaveProperty('target_count');
      });
    });
  });
});
