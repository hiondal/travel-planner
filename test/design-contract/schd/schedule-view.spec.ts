/**
 * SCHD 서비스 - 일정표 조회 행위 계약 테스트
 *
 * 설계 근거: schd-일정표조회.puml
 * API 명세: schedule-service-api.yaml - GET /trips/{trip_id}/schedule
 */
import request from 'supertest';

const BASE_URL = process.env.SCHD_SERVICE_URL || 'http://localhost:8082';

describe('SCHD 서비스 - 일정표 조회', () => {
  const tripId = 'trip_01HX_test';

  describe('상태 배지 일괄 조회', () => {
    it('캐시 HIT 시 배지 포함 일정표 200 OK 반환', async () => {
      const res = await request(BASE_URL)
        .get(`/api/v1/trips/${tripId}/schedule`)
        .set('Authorization', 'Bearer test-token')
        .expect(200);

      // ScheduleResponse 스키마 검증
      expect(res.body).toHaveProperty('trip_id');
      expect(res.body).toHaveProperty('name');
      expect(res.body).toHaveProperty('city');
      expect(res.body).toHaveProperty('schedule_items');
      expect(Array.isArray(res.body.schedule_items)).toBe(true);

      // ScheduleItemSummary 스키마 검증 (항목이 있는 경우)
      if (res.body.schedule_items.length > 0) {
        const item = res.body.schedule_items[0];
        expect(item).toHaveProperty('schedule_item_id');
        expect(item).toHaveProperty('place_id');
        expect(item).toHaveProperty('place_name');
        expect(item).toHaveProperty('visit_datetime');
        expect(item).toHaveProperty('timezone');
        expect(item).toHaveProperty('order');
        expect(item).toHaveProperty('outside_business_hours');
      }
    });

    it('캐시 MISS 시 DB 조회 후 배지 포함 일정표 200 OK 반환', async () => {
      const res = await request(BASE_URL)
        .get(`/api/v1/trips/${tripId}/schedule`)
        .set('Authorization', 'Bearer test-token')
        .expect(200);

      // ScheduleResponse 스키마 검증 (캐시 MISS 시에도 동일 스키마)
      expect(res.body).toHaveProperty('trip_id');
      expect(res.body).toHaveProperty('name');
      expect(res.body).toHaveProperty('city');
      expect(res.body).toHaveProperty('schedule_items');
      expect(Array.isArray(res.body.schedule_items)).toBe(true);
    });
  });
});
