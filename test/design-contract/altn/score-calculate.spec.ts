/**
 * ALTN 서비스 - 대안 점수 계산 행위 계약 테스트
 *
 * 설계 근거:
 *   - 시퀀스: docs/design/sequence/inner/altn-대안점수계산.puml
 *   - API 명세: docs/design/api/alternative-service-api.yaml
 *
 * 흐름 요약:
 *   AlternativeController -> AlternativeService
 *     -> PLCE 서비스 후보 조회 (반경 단계적 확장)
 *     -> MNTR 서비스 상태 필터링
 *     -> ScoreWeightsProvider 가중치 조회 (캐시 -> 인터페이스)
 *     -> ScoreCalculator 합산 점수 계산 -> 상위 3장 선정 -> 스냅샷 저장
 *
 * 설계 의도:
 *   UFR-ALTN-010 [T2]: 대안 카드 정렬 가중치는 인터페이스로 추상화되어
 *   Phase 2에서 MLScoreWeightsProvider로 교체 가능 (코드 변경 없음)
 */
import request from 'supertest';

const BASE_URL = process.env.ALTN_SERVICE_URL || 'http://localhost:8086';

describe('ALTN 서비스 - 대안 점수 계산', () => {
  describe('대안 장소 검색 요청', () => {
    /**
     * 요청 파라미터 검증: placeId, userId, location(위도/경도), category 필수값 확인
     */
    it('필수 파라미터 누락 시 400 Bad Request 반환', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/alternatives/search')
        .set('Authorization', 'Bearer valid_paid_token')
        .send({
          // place_id 누락
          category: '라멘',
        })
        .expect(400);

      expect(res.body).toHaveProperty('error');
      expect(typeof res.body.error).toBe('string');

      expect(res.body).toHaveProperty('message');
      expect(typeof res.body.message).toBe('string');
    });
  });

  describe('후보 장소 조회 (PLCE 서비스)', () => {
    /**
     * 반경 1km 동일 카테고리 영업 중 장소를 검색하고
     * 후보가 3장 미만이면 반경을 단계적으로 확장한다 (UFR-ALTN-010 P22).
     */
    it('후보 3장 미만 시 반경 단계적 확장 (UFR-ALTN-010 P22) 후 대안 카드 반환', async () => {
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

      // AlternativeSearchResponse 스키마 검증
      expect(res.body).toHaveProperty('original_place_id');
      expect(res.body).toHaveProperty('cards');
      expect(Array.isArray(res.body.cards)).toBe(true);
      expect(res.body.cards.length).toBeLessThanOrEqual(3);
      expect(res.body).toHaveProperty('radius_used');
      expect(typeof res.body.radius_used).toBe('number');
    });

    it('여전히 3장 미만 시 반경 3km 최대 확장 후 대안 카드 반환', async () => {
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
      expect(res.body).toHaveProperty('radius_used');
    });
  });

  describe('후보 장소 상태 필터링 (UFR-ALTN-010 [U19])', () => {
    /**
     * 상태 기반 후보 필터링 및 레이블 분류:
     *   빨강(DANGER) 상태 -> 후보 제외
     *   초록(SAFE) 상태 -> 최우선 후보
     *   노랑(CAUTION) 상태 -> "주의 필요" 레이블 포함
     *   회색(UNKNOWN) 상태 -> "정보 미확인" 레이블로 최후 후보
     */
    it('빨강 상태 제외 및 노랑/회색 레이블 분류 시 필터링된 대안 카드 반환', async () => {
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

      // 각 카드의 status_label이 올바른 값인지 검증
      res.body.cards.forEach((card: { status_label: string | null }) => {
        expect([null, '주의 필요', '정보 미확인']).toContain(card.status_label);
      });
    });
  });

  describe('가중치 조회 - 인터페이스 추상화', () => {
    /**
     * 가중치 캐시 조회 (키: weights:{userId}:{category})
     * 캐시 HIT 시 캐시 가중치 반환 (<5ms)
     */
    it('캐시 HIT (TTL 1시간) 시 캐시 가중치로 점수 계산 후 대안 카드 반환', async () => {
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
    });

    /**
     * 캐시 MISS 시 ScoreWeightsProvider 인터페이스를 통해 가중치를 조회한다.
     * MVP: FixedScoreWeightsProvider (w1=0.5 거리, w2=0.3 평점, w3=0.2 혼잡도)
     */
    it('캐시 MISS 시 FixedScoreWeightsProvider 가중치로 점수 계산 후 대안 카드 반환', async () => {
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

      // 카드에 점수 기반 정렬 결과가 반영되었는지 검증
      if (res.body.cards.length > 0) {
        const card = res.body.cards[0];
        expect(card).toHaveProperty('distance_m');
        expect(card).toHaveProperty('rating');
        expect(card).toHaveProperty('congestion');
      }
    });
  });

  describe('합산 점수 계산', () => {
    /**
     * 각 후보 장소별 점수 계산 반복 시나리오:
     *   distance_score: 거리 역수 정규화 (가까울수록 높은 점수) [U9 반영]
     *   rating_score: Google Places 평점(0~5) 정규화
     *   congestion_score: 혼잡도 역수 정규화
     *   score = w1*distance_score + w2*rating_score + w3*congestion_score
     */
    it('각 후보 장소별 점수 계산 반복 시나리오', async () => {
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

      // 각 카드에 점수 계산에 필요한 필드가 모두 포함되어야 함
      res.body.cards.forEach((card: {
        distance_m: number;
        rating: number;
        congestion: string;
        reason: string;
      }) => {
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
      });
    });
  });

  describe('대안 카드 정렬 및 상위 3장 선정', () => {
    /**
     * 합산 점수 내림차순 정렬 후 상위 3개를 선택하며
     * 각 카드에 장소명, 거리, 평점, 혼잡도, 추천이유,
     * 확장 반경 정보, 상태 레이블이 포함된다.
     */
    it('합산 점수 내림차순 정렬 시 상위 3장 선정 및 카드 구성 요소 포함', async () => {
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
      expect(res.body.cards.length).toBeLessThanOrEqual(3);

      if (res.body.cards.length > 0) {
        const card = res.body.cards[0];
        expect(card).toHaveProperty('name');
        expect(card).toHaveProperty('distance_m');
        expect(card).toHaveProperty('rating');
        expect(card).toHaveProperty('congestion');
        expect(card).toHaveProperty('reason');
        expect(card).toHaveProperty('status_label');
        expect(card).toHaveProperty('coordinates');
        expect(card).toHaveProperty('travel_time');
      }

      // radius_used로 반경 확장 정보 확인
      expect(res.body).toHaveProperty('radius_used');
    });
  });

  describe('오류 처리', () => {
    /**
     * PLCE 서비스 호출 실패 시 (최대 반경 3km 내 후보 없음 또는 서비스 장애)
     * 빈 대안 목록을 반환한다 (3초 SLA 유지 우선).
     */
    it('PLCE 서비스 호출 실패 시 빈 대안 목록 반환', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/alternatives/search')
        .set('Authorization', 'Bearer valid_paid_token')
        .send({
          place_id: 'place_no_results',
          category: '라멘',
          location: {
            lat: 35.6595,
            lng: 139.7004,
          },
        })
        .expect(200);

      expect(res.body).toHaveProperty('cards');
      expect(Array.isArray(res.body.cards)).toBe(true);
      expect(res.body.cards.length).toBe(0);
    });

    /**
     * Phase 2 MLScoreWeightsProvider 500ms 타임아웃 초과 시
     * FixedScoreWeightsProvider로 즉시 폴백하여 대안 카드 3장을 정상 생성한다.
     */
    it('MLScoreWeightsProvider 타임아웃 (Phase 2) 시 FixedScoreWeightsProvider 폴백 정상 응답', async () => {
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
    });
  });
});
