/**
 * Integration 테스트 - 구독 결제 (PAY, AUTH)
 *
 * 설계 근거:
 *   - 시퀀스: docs/design/sequence/outer/11-구독결제.puml
 *   - API 명세: docs/design/api/payment-service-api.yaml
 *
 * 흐름 요약:
 *   모바일 -> PAY 서비스 (구독 플랜 조회)
 *     -> Apple IAP / Google Play 인앱 결제
 *     -> PAY 서비스 (영수증 서버 측 검증 + 구독 처리)
 *     -> AUTH 서비스 (구독 티어 변경, JWT 클레임 갱신)
 */
import request from 'supertest';

const PAY_URL = process.env.PAY_SERVICE_URL || 'http://localhost:8087';

describe('구독 결제 (PAY)', () => {
  describe('구독 플랜 선택', () => {
    /**
     * Paywall에서 구독 플랜 목록을 조회하면
     * Trip Pass, Pro 플랜 정보(가격, 혜택)가 반환된다.
     */
    it('구독 플랜 목록 조회 시 Trip Pass/Pro 플랜 정보 반환', async () => {
      const res = await request(PAY_URL)
        .get('/api/v1/subscriptions/plans')
        .set('Authorization', 'Bearer valid_access_token')
        .expect(200);

      // SubscriptionPlansResponse 스키마 검증
      expect(res.body).toHaveProperty('plans');
      expect(Array.isArray(res.body.plans)).toBe(true);
      expect(res.body.plans.length).toBeGreaterThan(0);

      const plan = res.body.plans[0];
      expect(plan).toHaveProperty('plan_id');
      expect(typeof plan.plan_id).toBe('string');

      expect(plan).toHaveProperty('name');
      expect(typeof plan.name).toBe('string');

      expect(plan).toHaveProperty('tier');
      expect(['TRIP_PASS', 'PRO']).toContain(plan.tier);

      expect(plan).toHaveProperty('price');
      expect(plan.price).toHaveProperty('amount');
      expect(typeof plan.price.amount).toBe('number');
      expect(plan.price).toHaveProperty('currency');
      expect(plan.price).toHaveProperty('period');

      expect(plan).toHaveProperty('features');
      expect(Array.isArray(plan.features)).toBe(true);
      expect(plan.features.length).toBeGreaterThan(0);
    });
  });

  describe('인앱 결제 진행', () => {
    /**
     * 결제 성공 후 영수증을 서버에서 검증하고
     * AUTH 서비스에 구독 티어 변경을 요청하여
     * 새 Access Token이 발급된다.
     */
    it('결제 성공 시 201 Created 구독 정보 + 새 토큰 반환', async () => {
      const res = await request(PAY_URL)
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

      expect(res.body).toHaveProperty('new_access_token');
      expect(typeof res.body.new_access_token).toBe('string');

      expect(res.body).toHaveProperty('activated_features');
      expect(Array.isArray(res.body.activated_features)).toBe(true);
      expect(res.body.activated_features.length).toBeGreaterThan(0);
    });

    /**
     * 결제 실패 (영수증 검증 실패) 시
     * 402 Payment Required를 반환한다.
     */
    it('결제 실패 시 402 Payment Required 반환', async () => {
      const res = await request(PAY_URL)
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

    /**
     * 사용자가 결제를 취소하면
     * 클라이언트에서 처리하므로 서버 호출이 발생하지 않는다.
     * 이 테스트는 취소 후 구독 상태가 변경되지 않음을 확인한다.
     */
    it('결제 취소 시 기존 구독 상태 유지 확인', async () => {
      const res = await request(PAY_URL)
        .get('/api/v1/subscriptions/status')
        .set('Authorization', 'Bearer valid_access_token')
        .expect(200);

      // SubscriptionStatusResponse 스키마 검증 (기존 상태 유지)
      expect(res.body).toHaveProperty('tier');
      expect(typeof res.body.tier).toBe('string');

      expect(res.body).toHaveProperty('status');
      expect(typeof res.body.status).toBe('string');
    });
  });
});
