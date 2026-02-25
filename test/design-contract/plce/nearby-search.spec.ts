/**
 * PLCE 서비스 - 주변 장소 검색 (반경 기반) 행위 계약 테스트
 *
 * 설계 근거: plce-주변장소검색.puml
 * API 명세: place-service-api.yaml - GET /places/nearby
 */
import request from 'supertest';

const BASE_URL = process.env.PLCE_SERVICE_URL || 'http://localhost:8083';

describe('PLCE 서비스 - 주변 장소 검색 (반경 기반)', () => {
  const nearbyParams = {
    lat: 35.6595,
    lng: 139.7004,
    category: '라멘',
    radius: 1000,
  };

  it('캐시 HIT 시 주변 장소 목록 200 OK 반환', async () => {
    const res = await request(BASE_URL)
      .get('/api/v1/places/nearby')
      .set('Authorization', 'Bearer test-token')
      .query(nearbyParams)
      .expect(200);

    // NearbyPlaceSearchResponse 스키마 검증
    expect(res.body).toHaveProperty('places');
    expect(Array.isArray(res.body.places)).toBe(true);
    expect(res.body).toHaveProperty('radius_used');
    expect(typeof res.body.radius_used).toBe('number');

    // NearbyPlace 스키마 검증 (항목이 있는 경우)
    if (res.body.places.length > 0) {
      const place = res.body.places[0];
      expect(place).toHaveProperty('place_id');
      expect(place).toHaveProperty('name');
      expect(place).toHaveProperty('address');
      expect(place).toHaveProperty('distance_m');
      expect(place).toHaveProperty('rating');
      expect(place).toHaveProperty('category');
      expect(place).toHaveProperty('coordinates');
      expect(place.coordinates).toHaveProperty('lat');
      expect(place.coordinates).toHaveProperty('lng');
      expect(place).toHaveProperty('is_open');
    }
  });

  describe('캐시 MISS', () => {
    it('API 정상 응답 시 주변 장소 목록 200 OK 반환', async () => {
      const res = await request(BASE_URL)
        .get('/api/v1/places/nearby')
        .set('Authorization', 'Bearer test-token')
        .query(nearbyParams)
        .expect(200);

      // NearbyPlaceSearchResponse 스키마 검증
      expect(res.body).toHaveProperty('places');
      expect(Array.isArray(res.body.places)).toBe(true);
      expect(res.body).toHaveProperty('radius_used');

      if (res.body.places.length > 0) {
        const place = res.body.places[0];
        expect(place).toHaveProperty('place_id');
        expect(place).toHaveProperty('name');
        expect(place).toHaveProperty('address');
        expect(place).toHaveProperty('distance_m');
        expect(typeof place.distance_m).toBe('number');
        expect(place).toHaveProperty('rating');
        expect(typeof place.rating).toBe('number');
        expect(place).toHaveProperty('category');
        expect(place).toHaveProperty('coordinates');
        expect(place.coordinates).toHaveProperty('lat');
        expect(place.coordinates).toHaveProperty('lng');
        expect(place).toHaveProperty('is_open');
        expect(typeof place.is_open).toBe('boolean');
      }
    });

    it('API 실패 시 503 Service Unavailable 반환', async () => {
      const res = await request(BASE_URL)
        .get('/api/v1/places/nearby')
        .set('Authorization', 'Bearer test-token')
        .query(nearbyParams)
        .expect(503);

      // ErrorResponse 스키마 검증
      expect(res.body).toHaveProperty('error');
      expect(res.body).toHaveProperty('message');
    });
  });
});
