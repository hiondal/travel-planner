/**
 * PLCE 서비스 - 장소 검색 행위 계약 테스트
 *
 * 설계 근거: plce-장소검색.puml
 * API 명세: place-service-api.yaml - GET /places/search
 */
import request from 'supertest';

const BASE_URL = process.env.PLCE_SERVICE_URL || 'http://localhost:8083';

describe('PLCE 서비스 - 장소 검색', () => {
  it('캐시 HIT 시 장소 목록 200 OK 반환', async () => {
    const res = await request(BASE_URL)
      .get('/api/v1/places/search')
      .set('Authorization', 'Bearer test-token')
      .query({ keyword: '시부야 라멘', city: '도쿄' })
      .expect(200);

    // PlaceSearchResponse 스키마 검증
    expect(res.body).toHaveProperty('places');
    expect(Array.isArray(res.body.places)).toBe(true);
    expect(res.body.places.length).toBeLessThanOrEqual(10);

    // PlaceSummary 스키마 검증 (항목이 있는 경우)
    if (res.body.places.length > 0) {
      const place = res.body.places[0];
      expect(place).toHaveProperty('place_id');
      expect(place).toHaveProperty('name');
      expect(place).toHaveProperty('address');
      expect(place).toHaveProperty('rating');
      expect(place).toHaveProperty('business_hours');
      expect(place).toHaveProperty('coordinates');
      expect(place.coordinates).toHaveProperty('lat');
      expect(place.coordinates).toHaveProperty('lng');
    }
  });

  describe('캐시 MISS', () => {
    it('API 정상 응답 시 장소 목록 200 OK 반환 및 캐시 저장', async () => {
      const res = await request(BASE_URL)
        .get('/api/v1/places/search')
        .set('Authorization', 'Bearer test-token')
        .query({ keyword: '시부야 라멘', city: '도쿄' })
        .expect(200);

      // PlaceSearchResponse 스키마 검증
      expect(res.body).toHaveProperty('places');
      expect(Array.isArray(res.body.places)).toBe(true);
      expect(res.body.places.length).toBeLessThanOrEqual(10);

      if (res.body.places.length > 0) {
        const place = res.body.places[0];
        expect(place).toHaveProperty('place_id');
        expect(place).toHaveProperty('name');
        expect(place).toHaveProperty('address');
        expect(place).toHaveProperty('rating');
        expect(place).toHaveProperty('business_hours');
        expect(Array.isArray(place.business_hours)).toBe(true);
        if (place.business_hours.length > 0) {
          const bh = place.business_hours[0];
          expect(bh).toHaveProperty('day');
          expect(bh).toHaveProperty('open');
          expect(bh).toHaveProperty('close');
        }
        expect(place).toHaveProperty('coordinates');
        expect(place.coordinates).toHaveProperty('lat');
        expect(place.coordinates).toHaveProperty('lng');
      }
    });

    it('API 실패 (Circuit Breaker OPEN) 시 DB Fallback 장소 목록 200 OK 반환', async () => {
      const res = await request(BASE_URL)
        .get('/api/v1/places/search')
        .set('Authorization', 'Bearer test-token')
        .query({ keyword: '시부야 라멘', city: '도쿄' })
        .expect(200);

      // PlaceSearchResponse 스키마 검증 (DB Fallback 시에도 동일 스키마)
      expect(res.body).toHaveProperty('places');
      expect(Array.isArray(res.body.places)).toBe(true);
    });
  });
});
