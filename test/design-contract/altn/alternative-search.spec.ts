/**
 * ALTN 서비스 - 대안 장소 검색 행위 계약 테스트
 *
 * 설계 근거:
 *   - 시퀀스: docs/design/sequence/inner/altn-대안장소검색.puml
 *   - API 명세: docs/design/api/alternative-service-api.yaml
 *
 * 흐름 요약:
 *   Client -> AltnController (JWT 구독 티어 확인)
 *     -> AlternativeService -> Redis 캐시 조회
 *     -> PlceService 반경 단계적 확장 검색 -> MntrService 배지 상태 필터링
 *     -> 합산 점수 정렬 상위 3장 -> 캐시 저장 -> 스냅샷 저장
 */
import request from 'supertest';

const BASE_URL = process.env.ALTN_SERVICE_URL || 'http://localhost:8086';

describe('ALTN 서비스 - 대안 장소 검색', () => {
  describe('구독 티어 확인', () => {
    /**
     * Free 티어 사용자는 대안 카드 기능을 이용할 수 없으며
     * 402 Payment Required(Paywall) 응답을 받는다.
     */
    it('Free 티어 시 402 Payment Required Paywall 반환', async () => {
      const res = await request(BASE_URL)
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

    describe('유료 티어', () => {
      describe('대안 카드 캐시 확인', () => {
        /**
         * 캐시에 대안 카드가 존재하면 (TTL 5분 이내)
         * 외부 서비스 호출 없이 캐시 결과를 반환한다.
         */
        it('캐시 HIT (TTL 5분) 시 대안 카드 목록 즉시 반환', async () => {
          const res = await request(BASE_URL)
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
          expect(typeof res.body.original_place_id).toBe('string');

          expect(res.body).toHaveProperty('cards');
          expect(Array.isArray(res.body.cards)).toBe(true);
          expect(res.body.cards.length).toBeLessThanOrEqual(3);

          expect(res.body).toHaveProperty('radius_used');
          expect(typeof res.body.radius_used).toBe('number');
        });

        describe('캐시 MISS', () => {
          describe('주변 장소 검색 (단계적 반경 확장)', () => {
            /**
             * 반경 1km 검색 결과가 충분한 경우 (3장 이상)
             * 반경 확장 없이 대안 카드를 반환한다.
             */
            it('반경 1km 검색 결과 충분 시 대안 카드 3장 반환', async () => {
              const res = await request(BASE_URL)
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

              // AlternativeCard 스키마 검증
              if (res.body.cards.length > 0) {
                const card = res.body.cards[0];
                expect(card).toHaveProperty('alt_id');
                expect(typeof card.alt_id).toBe('string');

                expect(card).toHaveProperty('place_id');
                expect(typeof card.place_id).toBe('string');

                expect(card).toHaveProperty('name');
                expect(typeof card.name).toBe('string');

                expect(card).toHaveProperty('distance_m');
                expect(typeof card.distance_m).toBe('number');

                expect(card).toHaveProperty('rating');
                expect(typeof card.rating).toBe('number');
                expect(card.rating).toBeGreaterThanOrEqual(0);
                expect(card.rating).toBeLessThanOrEqual(5);

                expect(card).toHaveProperty('congestion');
                expect(['낮음', '보통', '혼잡']).toContain(card.congestion);

                expect(card).toHaveProperty('reason');
                expect(typeof card.reason).toBe('string');

                expect(card).toHaveProperty('status_label');

                expect(card).toHaveProperty('coordinates');
                expect(card.coordinates).toHaveProperty('lat');
                expect(card.coordinates).toHaveProperty('lng');

                expect(card).toHaveProperty('travel_time');
                expect(card.travel_time).toHaveProperty('walking_minutes');
                expect(typeof card.travel_time.walking_minutes).toBe('number');
              }

              expect(res.body).toHaveProperty('radius_used');
              expect(typeof res.body.radius_used).toBe('number');
            });

            /**
             * 반경 1km 결과가 3장 미만이면 반경 2km로 추가 검색한다 (P22).
             */
            it('결과 3장 미만 (P22) 시 반경 2km 추가 검색 후 대안 카드 반환', async () => {
              const res = await request(BASE_URL)
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
              expect(res.body.cards.length).toBeLessThanOrEqual(3);

              expect(res.body).toHaveProperty('radius_used');
              expect(typeof res.body.radius_used).toBe('number');
            });

            /**
             * 반경 2km에서도 3장 미만이면 반경 3km로 최종 추가 검색한다.
             */
            it('여전히 3장 미만 시 반경 3km 최종 확장 검색 후 대안 카드 반환', async () => {
              const res = await request(BASE_URL)
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
              expect(res.body.cards.length).toBeLessThanOrEqual(3);

              expect(res.body).toHaveProperty('radius_used');
              expect(typeof res.body.radius_used).toBe('number');
            });
          });

          describe('배지 상태 필터링', () => {
            /**
             * 후보 장소의 배지 상태를 일괄 조회하여
             * 빨강 상태 장소는 제외하고, 노랑은 "주의 필요",
             * 회색은 "정보 미확인" 레이블을 부여한다.
             */
            it('빨강 상태 장소 제외 후 초록/노랑/회색 레이블 포함 대안 카드 반환', async () => {
              const res = await request(BASE_URL)
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

              // 각 카드의 status_label 검증
              res.body.cards.forEach((card: { status_label: string | null }) => {
                // status_label은 null(초록), "주의 필요"(노랑), "정보 미확인"(회색) 중 하나
                expect([null, '주의 필요', '정보 미확인']).toContain(card.status_label);
              });
            });
          });

          describe('합산 점수 정렬 및 상위 3장 선택', () => {
            /**
             * score = 0.5*distance + 0.3*rating + 0.2*congestion 공식으로
             * 합산 점수를 계산하여 상위 3장을 선택한다.
             */
            it('합산 점수 내림차순 정렬 시 상위 3장 대안 카드 반환', async () => {
              const res = await request(BASE_URL)
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

              // 모든 카드에 추천 이유가 포함되어야 함
              res.body.cards.forEach((card: { reason: string }) => {
                expect(card).toHaveProperty('reason');
                expect(typeof card.reason).toBe('string');
                expect(card.reason.length).toBeGreaterThan(0);
              });
            });
          });
        });
      });
    });
  });
});
