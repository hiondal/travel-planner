/**
 * Integration 테스트 - 여행 일정 등록 (SCHD, PLCE)
 *
 * 설계 근거:
 *   - 시퀀스: docs/design/sequence/outer/03-여행일정등록.puml
 *   - API 명세: docs/design/api/schedule-service-api.yaml
 *                docs/design/api/place-service-api.yaml
 *
 * 흐름 요약:
 *   모바일 -> SCHD 서비스 (여행 생성) -> PLCE 서비스 (장소 검색)
 *     -> SCHD 서비스 (장소 추가) -> 이벤트 버스 -> MNTR 서비스 (모니터링 등록)
 */
import request from 'supertest';

const SCHD_URL = process.env.SCHD_SERVICE_URL || 'http://localhost:8082';
const PLCE_URL = process.env.PLCE_SERVICE_URL || 'http://localhost:8083';

describe('여행 일정 등록 (SCHD, PLCE)', () => {
  describe('여행 일정 생성', () => {
    /**
     * 첫 번째 여행 일정 생성 시 위치정보 동의가 필요하면
     * 동의 요청 필요 응답을 반환한다.
     */
    it('첫 번째 여행 일정 생성 (권한 동의) 시 동의 요청 필요 응답', async () => {
      const res = await request(SCHD_URL)
        .post('/api/v1/trips')
        .set('Authorization', 'Bearer valid_access_token')
        .send({
          name: '도쿄 3박4일',
          start_date: '2026-03-15',
          end_date: '2026-03-18',
          city: '도쿄',
        })
        .expect(428);

      expect(res.body).toHaveProperty('error');
      expect(res.body).toHaveProperty('message');
      expect(res.body).toHaveProperty('consent_required');
      expect(res.body.consent_required).toBe(true);
    });

    /**
     * 동의 완료 후 여행 일정이 정상 생성되어
     * 201 Created와 빈 일정표가 반환된다.
     */
    it('동의 완료 후 여행 일정 생성 시 201 Created 빈 일정표 반환', async () => {
      const res = await request(SCHD_URL)
        .post('/api/v1/trips')
        .set('Authorization', 'Bearer valid_access_token')
        .send({
          name: '도쿄 3박4일',
          start_date: '2026-03-15',
          end_date: '2026-03-18',
          city: '도쿄',
        })
        .expect(201);

      // TripResponse 스키마 검증
      expect(res.body).toHaveProperty('trip_id');
      expect(typeof res.body.trip_id).toBe('string');

      expect(res.body).toHaveProperty('name');
      expect(res.body).toHaveProperty('start_date');
      expect(res.body).toHaveProperty('end_date');
      expect(res.body).toHaveProperty('city');
      expect(res.body).toHaveProperty('status');
      expect(res.body.status).toBe('ACTIVE');

      expect(res.body).toHaveProperty('schedule_items');
      expect(Array.isArray(res.body.schedule_items)).toBe(true);
      expect(res.body.schedule_items).toHaveLength(0);
    });
  });

  describe('장소 검색', () => {
    /**
     * 키워드 기반으로 PLCE 서비스에서 장소를 검색하여
     * 최대 10개의 장소 목록을 반환한다.
     */
    it('키워드 기반 장소 검색 시 장소 목록 반환', async () => {
      const res = await request(PLCE_URL)
        .get('/api/v1/places/search')
        .query({ keyword: '시부야 라멘', city: '도쿄' })
        .set('Authorization', 'Bearer valid_access_token')
        .expect(200);

      // PlaceSearchResponse 스키마 검증
      expect(res.body).toHaveProperty('places');
      expect(Array.isArray(res.body.places)).toBe(true);

      if (res.body.places.length > 0) {
        const place = res.body.places[0];
        expect(place).toHaveProperty('place_id');
        expect(typeof place.place_id).toBe('string');

        expect(place).toHaveProperty('name');
        expect(typeof place.name).toBe('string');

        expect(place).toHaveProperty('address');
        expect(place).toHaveProperty('rating');
        expect(place).toHaveProperty('coordinates');
        expect(place.coordinates).toHaveProperty('lat');
        expect(place.coordinates).toHaveProperty('lng');
      }
    });
  });

  describe('일정에 장소 추가', () => {
    const tripId = 'trip_01HX_test';

    /**
     * 영업시간 내 방문 시간으로 장소를 추가하면
     * 201 Created와 ScheduleItemAdded 이벤트가 발행된다.
     */
    it('영업시간 내 방문 시간 시 201 Created 장소 추가 성공', async () => {
      const res = await request(SCHD_URL)
        .post(`/api/v1/trips/${tripId}/schedule-items`)
        .set('Authorization', 'Bearer valid_access_token')
        .send({
          place_id: 'place_abc123',
          visit_datetime: '2026-03-16T12:00:00+09:00',
          timezone: 'Asia/Tokyo',
        })
        .expect(201);

      // ScheduleItemResponse 스키마 검증
      expect(res.body).toHaveProperty('schedule_item_id');
      expect(typeof res.body.schedule_item_id).toBe('string');

      expect(res.body).toHaveProperty('place_id');
      expect(res.body).toHaveProperty('place_name');
      expect(res.body).toHaveProperty('visit_datetime');
      expect(res.body).toHaveProperty('timezone');
      expect(res.body).toHaveProperty('order');
      expect(res.body).toHaveProperty('outside_business_hours');
      expect(res.body.outside_business_hours).toBe(false);
    });

    /**
     * 영업시간 외 방문 시간으로 추가 시
     * 경고 메시지와 함께 200 OK를 반환한다.
     */
    it('영업시간 외 방문 시간 시 200 OK 경고 메시지 반환', async () => {
      const res = await request(SCHD_URL)
        .post(`/api/v1/trips/${tripId}/schedule-items`)
        .set('Authorization', 'Bearer valid_access_token')
        .send({
          place_id: 'place_abc123',
          visit_datetime: '2026-03-16T09:00:00+09:00',
          timezone: 'Asia/Tokyo',
        })
        .expect(200);

      // BusinessHoursWarningResponse 스키마 검증
      expect(res.body).toHaveProperty('warning');
      expect(res.body.warning).toBe('OUTSIDE_BUSINESS_HOURS');

      expect(res.body).toHaveProperty('message');
      expect(typeof res.body.message).toBe('string');

      expect(res.body).toHaveProperty('business_hours');
      expect(typeof res.body.business_hours).toBe('string');
    });
  });
});
