/**
 * Integration 테스트 - 출발 전 브리핑 생성 및 수신 (BRIF, MNTR)
 *
 * 설계 근거:
 *   - 시퀀스: docs/design/sequence/outer/07-출발전브리핑.puml
 *   - API 명세: docs/design/api/briefing-service-api.yaml
 *                docs/design/api/monitor-service-api.yaml
 *
 * 흐름 요약:
 *   스케줄러 -> BRIF 서비스 (멱등성 키 확인)
 *     -> MNTR 서비스 (최신 수집 데이터 조회)
 *     -> 구독 티어 확인 -> 안심/주의 분기 판단 + 총평 생성
 *     -> FCM Push 알림 발송 -> 모바일 -> BRIF 서비스 (브리핑 상세 조회)
 */
import request from 'supertest';

const BRIF_URL = process.env.BRIF_SERVICE_URL || 'http://localhost:8085';

describe('출발 전 브리핑 생성 및 수신 (BRIF, MNTR)', () => {
  describe('출발 전 브리핑 트리거 (15~30분 전)', () => {
    /**
     * 동일한 place_id + departure_time 해시로 이미 생성된 브리핑이 존재하면
     * 중복 생성을 방지하고 기존 브리핑 ID를 반환한다 (P21).
     */
    it('이미 생성된 브리핑 존재 (P21) 시 기존 브리핑 ID 반환', async () => {
      const res = await request(BRIF_URL)
        .post('/api/v1/briefings/generate')
        .set('X-Internal-Service-Key', 'test-internal-key')
        .send({
          schedule_item_id: 'si_01HX_test',
          place_id: 'place_abc123',
          user_id: 'usr_01HX_test',
          departure_time: '2026-03-16T12:00:00+09:00',
          triggered_at: '2026-03-16T11:40:00+09:00',
        })
        .expect(200);

      // GenerateBriefingResponse 스키마 검증 (기존 브리핑)
      expect(res.body).toHaveProperty('briefing_id');
      expect(typeof res.body.briefing_id).toBe('string');

      expect(res.body).toHaveProperty('status');
      expect(res.body.status).toBe('EXISTING');

      expect(res.body).toHaveProperty('type');
      expect(['SAFE', 'WARNING']).toContain(res.body.type);
    });

    describe('신규 브리핑 생성', () => {
      describe('MNTR 데이터 조회', () => {
        /**
         * 캐시 데이터가 5분 이내이면 캐시에서 즉시 반환한다.
         */
        it('캐시 데이터 5분 이내 시 신규 브리핑 생성 성공', async () => {
          const res = await request(BRIF_URL)
            .post('/api/v1/briefings/generate')
            .send({
              schedule_item_id: 'si_02HX_test',
              place_id: 'place_def456',
              user_id: 'usr_01HX_test',
              departure_time: '2026-03-16T15:00:00+09:00',
              triggered_at: '2026-03-16T14:30:00+09:00',
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
         * 캐시 데이터가 5분을 초과하면 비동기 데이터 수집 후 반환한다.
         */
        it('캐시 데이터 5분 초과 시 최신 데이터 수집 후 브리핑 생성 성공', async () => {
          const res = await request(BRIF_URL)
            .post('/api/v1/briefings/generate')
            .send({
              schedule_item_id: 'si_03HX_test',
              place_id: 'place_ghi789',
              user_id: 'usr_01HX_test',
              departure_time: '2026-03-16T18:00:00+09:00',
              triggered_at: '2026-03-16T17:30:00+09:00',
            })
            .expect(201);

          expect(res.body).toHaveProperty('briefing_id');
          expect(res.body).toHaveProperty('status');
          expect(res.body.status).toBe('CREATED');
          expect(res.body).toHaveProperty('type');
        });
      });

      describe('구독 티어 확인', () => {
        /**
         * Free 티어 한도 초과 (P19) 시 브리핑 생성을 스킵한다.
         */
        it('Free 티어 한도 초과 (P19) 시 422 SKIPPED 반환', async () => {
          const res = await request(BRIF_URL)
            .post('/api/v1/briefings/generate')
            .send({
              schedule_item_id: 'si_04HX_test',
              place_id: 'place_limit_exceed',
              user_id: 'usr_free_limit',
              departure_time: '2026-03-16T20:00:00+09:00',
              triggered_at: '2026-03-16T19:30:00+09:00',
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

        describe('한도 이내 또는 유료 티어', () => {
          describe('FCM Push 알림 발송', () => {
            /**
             * FCM Circuit Breaker가 CLOSED 상태이면
             * Push 알림이 정상 발송되고 브리핑이 생성된다.
             */
            it('FCM Circuit Breaker CLOSED 시 브리핑 생성 및 Push 발송 성공', async () => {
              const res = await request(BRIF_URL)
                .post('/api/v1/briefings/generate')
                .send({
                  schedule_item_id: 'si_05HX_test',
                  place_id: 'place_fcm_ok',
                  user_id: 'usr_paid_user',
                  departure_time: '2026-03-17T12:00:00+09:00',
                  triggered_at: '2026-03-17T11:30:00+09:00',
                })
                .expect(201);

              expect(res.body).toHaveProperty('briefing_id');
              expect(res.body).toHaveProperty('status');
              expect(res.body.status).toBe('CREATED');
              expect(res.body).toHaveProperty('type');
              expect(['SAFE', 'WARNING']).toContain(res.body.type);
            });

            /**
             * FCM Circuit Breaker가 OPEN 상태이면
             * 인앱 알림 Fallback으로 전환되지만 브리핑 자체는 생성된다.
             */
            it('FCM Circuit Breaker OPEN (Fallback) 시 브리핑 생성 성공 (인앱 폴백)', async () => {
              const res = await request(BRIF_URL)
                .post('/api/v1/briefings/generate')
                .send({
                  schedule_item_id: 'si_06HX_test',
                  place_id: 'place_fcm_fail',
                  user_id: 'usr_paid_user',
                  departure_time: '2026-03-17T15:00:00+09:00',
                  triggered_at: '2026-03-17T14:30:00+09:00',
                })
                .expect(201);

              expect(res.body).toHaveProperty('briefing_id');
              expect(res.body).toHaveProperty('status');
              expect(res.body.status).toBe('CREATED');
            });
          });
        });
      });
    });
  });

  describe('브리핑 상세 조회', () => {
    /**
     * Push 알림 탭 후 브리핑 상세를 조회하면
     * 영업상태, 혼잡도, 날씨, 이동시간, 총평이 포함된 응답을 받는다.
     */
    it('브리핑 상세 조회 시 200 OK 상세 정보 반환', async () => {
      const briefingId = 'brif_01HX_test';
      const res = await request(BRIF_URL)
        .get(`/api/v1/briefings/${briefingId}`)
        .set('Authorization', 'Bearer valid_access_token')
        .expect(200);

      // BriefingDetailResponse 스키마 검증
      expect(res.body).toHaveProperty('briefing_id');
      expect(typeof res.body.briefing_id).toBe('string');

      expect(res.body).toHaveProperty('type');
      expect(['SAFE', 'WARNING']).toContain(res.body.type);

      expect(res.body).toHaveProperty('place_id');
      expect(res.body).toHaveProperty('place_name');
      expect(res.body).toHaveProperty('departure_time');
      expect(res.body).toHaveProperty('created_at');

      expect(res.body).toHaveProperty('expired');
      expect(typeof res.body.expired).toBe('boolean');

      expect(res.body).toHaveProperty('content');
      expect(res.body.content).toHaveProperty('business_status');
      expect(res.body.content).toHaveProperty('congestion');
      expect(res.body.content).toHaveProperty('weather');
      expect(res.body.content).toHaveProperty('travel_time');
      expect(res.body.content.travel_time).toHaveProperty('walking_minutes');
      expect(res.body.content.travel_time).toHaveProperty('distance_m');
      expect(res.body.content).toHaveProperty('summary');
      expect(typeof res.body.content.summary).toBe('string');
    });
  });
});
