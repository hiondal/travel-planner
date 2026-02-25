/**
 * Integration 테스트 - 대안 장소 검색 (ALTN, PLCE, MNTR)
 *
 * 설계 근거:
 *   - 시퀀스: docs/design/sequence/outer/09-대안장소검색.puml
 *   - API 명세: docs/design/api/alternative-service-api.yaml
 *
 * 흐름 요약:
 *   모바일 -> ALTN 서비스 (구독 티어 확인)
 *     -> Redis 캐시 조회 -> PLCE 서비스 (반경 단계 확장 검색)
 *     -> MNTR 서비스 (배지 상태 필터링) -> 합산 점수 정렬 -> 캐시 저장
 */
import request from 'supertest';

const ALTN_URL = process.env.ALTN_SERVICE_URL || 'http://localhost:8086';

describe('대안 장소 검색 (ALTN, PLCE, MNTR)', () => {
  describe('Free 티어 Paywall 확인', () => {
    /**
     * Free 티어 사용자 (P19) 는 대안 카드 기능을 사용할 수 없으며
     * 402 Payment Required와 Paywall 정보를 반환한다.
     */
    it('Free 티어 (P19) 시 402 Payment Required Paywall 반환', async () => {
      const res = await request(ALTN_URL)
        .post('/api/v1/alternatives/search')
        .send({
          place_id: 'place_abc123',
          category: '라멘',
          location: {
            lat: 35.6595,
            lng: 139.7004,
          },
        })
        .expect(402);

      // PaywallResponse 스키마 검증
      expect(res.body).toHaveProperty('paywall');
      expect(res.body.paywall).toBe(true);

      expect(res.body).toHaveProperty('message');
      expect(typeof res.body.message).toBe('string');

      expect(res.body).toHaveProperty('upgrade_url');
      expect(typeof res.body.upgrade_url).toBe('string');
    });

    describe('유료 티어 (Trip Pass / Pro)', () => {
      describe('대안 카드 캐시 확인', () => {
        /**
         * 캐시에 대안 카드가 TTL 5분 이내로 존재하면
         * 캐시에서 즉시 대안 카드 목록을 반환한다.
         */
        it('캐시 HIT (TTL 5분) 시 대안 카드 목록 즉시 반환', async () => {
          const res = await request(ALTN_URL)
            .post('/api/v1/alternatives/search')
            .set('Authorization', 'Bearer valid_paid_token')
            .send({
              place_id: 'place_abc123',
              category: '라멘',
              location: {
                lat: 35.6595,
                lng: 139.7004,
              },
            })
            .expect(200);

          // AlternativeSearchResponse 스키마 검증
          expect(res.body).toHaveProperty('original_place_id');
          expect(res.body).toHaveProperty('cards');
          expect(Array.isArray(res.body.cards)).toBe(true);
          expect(res.body.cards.length).toBeLessThanOrEqual(3);
          expect(res.body).toHaveProperty('radius_used');
        });

        describe('캐시 MISS', () => {
          describe('주변 장소 검색 (반경 단계 확장)', () => {
            /**
             * 반경 1km 검색 결과가 충분하면
             * 확장 없이 대안 카드를 반환한다.
             */
            it('반경 1km 검색 결과 충분 시 대안 카드 반환', async () => {
              const res = await request(ALTN_URL)
                .post('/api/v1/alternatives/search')
                .set('Authorization', 'Bearer valid_paid_token')
                .send({
                  place_id: 'place_abc123',
                  category: '라멘',
                  location: {
                    lat: 35.6595,
                    lng: 139.7004,
                  },
                })
                .expect(200);

              expect(res.body).toHaveProperty('cards');
              expect(Array.isArray(res.body.cards)).toBe(true);
              expect(res.body.cards.length).toBeLessThanOrEqual(3);

              if (res.body.cards.length > 0) {
                const card = res.body.cards[0];
                expect(card).toHaveProperty('alt_id');
                expect(card).toHaveProperty('place_id');
                expect(card).toHaveProperty('name');
                expect(card).toHaveProperty('distance_m');
                expect(card).toHaveProperty('rating');
                expect(card).toHaveProperty('congestion');
                expect(card).toHaveProperty('reason');
                expect(card).toHaveProperty('status_label');
                expect(card).toHaveProperty('coordinates');
                expect(card).toHaveProperty('travel_time');
              }
            });

            /**
             * 결과 3장 미만 (P22) 이면 반경 2km로 확장 재검색한다.
             */
            it('결과 3장 미만 (P22) 시 반경 2km 확장 재검색', async () => {
              const res = await request(ALTN_URL)
                .post('/api/v1/alternatives/search')
                .set('Authorization', 'Bearer valid_paid_token')
                .send({
                  place_id: 'place_sparse_area',
                  category: '라멘',
                  location: {
                    lat: 35.6595,
                    lng: 139.7004,
                  },
                })
                .expect(200);

              expect(res.body).toHaveProperty('cards');
              expect(Array.isArray(res.body.cards)).toBe(true);
              expect(res.body).toHaveProperty('radius_used');
            });

            /**
             * 반경 2km에서도 3장 미만이면 반경 3km로 최종 확장 재검색한다.
             */
            it('여전히 3장 미만 시 반경 3km 최종 확장 재검색', async () => {
              const res = await request(ALTN_URL)
                .post('/api/v1/alternatives/search')
                .set('Authorization', 'Bearer valid_paid_token')
                .send({
                  place_id: 'place_very_sparse',
                  category: '라멘',
                  location: {
                    lat: 35.6595,
                    lng: 139.7004,
                  },
                })
                .expect(200);

              expect(res.body).toHaveProperty('cards');
              expect(Array.isArray(res.body.cards)).toBe(true);
              expect(res.body).toHaveProperty('radius_used');
            });
          });

          describe('상태 배지 필터링 및 정렬', () => {
            /**
             * 후보 장소 배지 상태를 조회하여 빨강 장소를 제외하고
             * 합산 점수(w1=0.5거리 + w2=0.3평점 + w3=0.2혼잡도)로
             * 정렬하여 상위 3장을 선택한다.
             */
            it('빨강 장소 제외 및 합산 점수 정렬 시 상위 3장 반환', async () => {
              const res = await request(ALTN_URL)
                .post('/api/v1/alternatives/search')
                .set('Authorization', 'Bearer valid_paid_token')
                .send({
                  place_id: 'place_abc123',
                  category: '라멘',
                  location: {
                    lat: 35.6595,
                    lng: 139.7004,
                  },
                })
                .expect(200);

              expect(res.body).toHaveProperty('cards');
              expect(Array.isArray(res.body.cards)).toBe(true);
              expect(res.body.cards.length).toBeLessThanOrEqual(3);

              // 각 카드의 필수 필드 검증
              res.body.cards.forEach((card: {
                alt_id: string;
                name: string;
                distance_m: number;
                rating: number;
                congestion: string;
                reason: string;
              }) => {
                expect(card).toHaveProperty('alt_id');
                expect(card).toHaveProperty('name');
                expect(card).toHaveProperty('distance_m');
                expect(card).toHaveProperty('rating');
                expect(card).toHaveProperty('congestion');
                expect(card).toHaveProperty('reason');
              });
            });
          });
        });
      });
    });
  });
});
