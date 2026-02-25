/**
 * PAY 서비스 - 구독 구매 행위 계약 테스트
 *
 * 설계 근거:
 *   - 시퀀스: docs/design/sequence/inner/pay-구독구매.puml
 *   - API 명세: docs/design/api/payment-service-api.yaml
 *
 * 흐름 요약:
 *   Client -> PayController (요청 검증) -> SubscriptionService
 *     -> Apple IAP / Google Play 영수증 검증 (Circuit Breaker + Retry)
 *     -> 구독 저장 + 결제 이력 저장
 *     -> AuthService JWT 클레임 갱신 (기존 세션 무효화 + 새 토큰 발급)
 */
import request from 'supertest';

const BASE_URL = process.env.PAY_SERVICE_URL || 'http://localhost:8087';

describe('PAY 서비스 - 구독 구매', () => {
  describe('영수증 서버 측 검증 (Circuit Breaker + Retry)', () => {
    describe('영수증 검증 성공', () => {
      describe('구독 상태 저장', () => {
        /**
         * 영수증 검증 성공 후 구독 정보(user_id, plan, status=ACTIVE, start_at, expires_at)가
         * subscriptions 테이블에 저장/갱신되고, 결제 이력(transaction_id, amount, provider)이
         * payment_records 테이블에 삽입된다.
         */
        it('영수증 검증 성공 시 구독 정보와 결제 이력 저장 후 201 Created 반환', async () => {
          const res = await request(BASE_URL)
            .post('/api/v1/subscriptions/purchase')
            .set('Authorization', 'Bearer valid_access_token')
            .send({
              plan_id: 'plan_trip_pass',
              receipt: 'MIIT3QYJKoZIhvcNAQcCoIIT...',
              provider: 'apple',
            })
            .expect(201);

          // PurchaseResponse 스키마 검증
          expect(res.body).toHaveProperty('subscription_id');
          expect(typeof res.body.subscription_id).toBe('string');

          expect(res.body).toHaveProperty('tier');
          expect(['TRIP_PASS', 'PRO']).toContain(res.body.tier);

          expect(res.body).toHaveProperty('status');
          expect(['ACTIVE', 'CANCELLED', 'CANCELLING']).toContain(res.body.status);

          expect(res.body).toHaveProperty('started_at');
          expect(typeof res.body.started_at).toBe('string');

          expect(res.body).toHaveProperty('expires_at');
          // expires_at은 nullable (Trip Pass 1회성의 경우 null)
        });
      });

      describe('JWT 클레임 갱신 (구독 티어 즉시 반영)', () => {
        /**
         * 구독 구매 완료 후 AuthService에 구독 티어 변경을 요청하여
         * 기존 세션 토큰을 즉시 무효화하고 새 Access Token이 발급된다.
         * 응답에 new_access_token과 activated_features가 포함된다.
         */
        it('구독 티어 변경 시 새 Access Token과 활성화 기능 목록 반환', async () => {
          const res = await request(BASE_URL)
            .post('/api/v1/subscriptions/purchase')
            .set('Authorization', 'Bearer valid_access_token')
            .send({
              plan_id: 'plan_pro',
              receipt: 'eyJwdXJjaGFzZVRva2VuIjoiQUV1aHA0Li4uIn0=',
              provider: 'google',
            })
            .expect(201);

          // 새 토큰 발급 검증
          expect(res.body).toHaveProperty('new_access_token');
          expect(typeof res.body.new_access_token).toBe('string');

          // 활성화된 기능 목록 검증
          expect(res.body).toHaveProperty('activated_features');
          expect(Array.isArray(res.body.activated_features)).toBe(true);
          expect(res.body.activated_features.length).toBeGreaterThan(0);
          res.body.activated_features.forEach((feature: unknown) => {
            expect(typeof feature).toBe('string');
          });
        });
      });
    });

    describe('영수증 검증 실패', () => {
      /**
       * Apple IAP / Google Play 영수증 검증이 실패하면
       * 402 Payment Required를 반환한다.
       */
      it('영수증 검증 실패 시 402 Payment Required 반환', async () => {
        const res = await request(BASE_URL)
          .post('/api/v1/subscriptions/purchase')
          .set('Authorization', 'Bearer valid_access_token')
          .send({
            plan_id: 'plan_trip_pass',
            receipt: 'invalid_receipt_data',
            provider: 'apple',
          })
          .expect(402);

        // ErrorResponse 스키마 검증
        expect(res.body).toHaveProperty('error');
        expect(typeof res.body.error).toBe('string');

        expect(res.body).toHaveProperty('message');
        expect(typeof res.body.message).toBe('string');
      });
    });

    describe('결제 Gateway 장애 (Circuit Breaker OPEN)', () => {
      /**
       * 결제 Gateway 장애로 Circuit Breaker가 OPEN 상태일 때
       * 503 Service Unavailable을 반환한다.
       */
      it('결제 Gateway 장애 시 503 Service Unavailable 반환', async () => {
        const res = await request(BASE_URL)
          .post('/api/v1/subscriptions/purchase')
          .set('Authorization', 'Bearer valid_access_token')
          .send({
            plan_id: 'plan_trip_pass',
            receipt: 'MIIT3QYJKoZIhvcNAQcCoIIT...',
            provider: 'apple',
          })
          .expect(503);

        // ErrorResponse 스키마 검증
        expect(res.body).toHaveProperty('error');
        expect(typeof res.body.error).toBe('string');

        expect(res.body).toHaveProperty('message');
        expect(typeof res.body.message).toBe('string');
      });
    });
  });
});
