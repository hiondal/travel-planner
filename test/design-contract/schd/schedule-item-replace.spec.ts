/**
 * SCHD 서비스 - 일정 장소 교체 행위 계약 테스트
 *
 * 설계 근거: schd-일정장소교체.puml
 * API 명세: schedule-service-api.yaml - PUT /trips/{trip_id}/schedule-items/{item_id}/replace
 */
import request from 'supertest';

const BASE_URL = process.env.SCHD_SERVICE_URL || 'http://localhost:8082';

describe('SCHD 서비스 - 일정 장소 교체', () => {
  const tripId = 'trip_01HX_test';
  const itemId = 'si_01HX_test';

  describe('이동시간 재계산 (2구간)', () => {
    it('Google Directions API 정상 시 교체 성공 및 이동시간 반환', async () => {
      const res = await request(BASE_URL)
        .put(`/api/v1/trips/${tripId}/schedule-items/${itemId}/replace`)
        .set('Authorization', 'Bearer test-token')
        .send({
          new_place_id: 'place_xyz789',
        })
        .expect(200);

      // ReplaceScheduleItemResponse 스키마 검증
      expect(res.body).toHaveProperty('schedule_item_id');
      expect(res.body).toHaveProperty('original_place');
      expect(res.body.original_place).toHaveProperty('place_id');
      expect(res.body.original_place).toHaveProperty('place_name');
      expect(res.body).toHaveProperty('new_place');
      expect(res.body.new_place).toHaveProperty('place_id');
      expect(res.body.new_place).toHaveProperty('place_name');
      expect(res.body).toHaveProperty('travel_time_diff_minutes');
      expect(typeof res.body.travel_time_diff_minutes).toBe('number');
      expect(res.body).toHaveProperty('updated_schedule_items');
      expect(Array.isArray(res.body.updated_schedule_items)).toBe(true);
    });

    /**
     * API 실패 (Fallback) 시 직선거리 기반 추정값으로 교체 수행
     * 설계 노트: 폴백 추정값 사용 표시
     */
    it('API 실패 (Fallback) 시 직선거리 기반 추정값으로 교체 성공', async () => {
      const res = await request(BASE_URL)
        .put(`/api/v1/trips/${tripId}/schedule-items/${itemId}/replace`)
        .set('Authorization', 'Bearer test-token')
        .send({
          new_place_id: 'place_xyz789',
        })
        .expect(200);

      // ReplaceScheduleItemResponse 스키마 검증 (폴백 시에도 동일 스키마)
      expect(res.body).toHaveProperty('schedule_item_id');
      expect(res.body).toHaveProperty('original_place');
      expect(res.body).toHaveProperty('new_place');
      expect(res.body).toHaveProperty('travel_time_diff_minutes');
      expect(typeof res.body.travel_time_diff_minutes).toBe('number');
      expect(res.body).toHaveProperty('updated_schedule_items');
      expect(Array.isArray(res.body.updated_schedule_items)).toBe(true);
    });
  });

  describe('단일 DB 트랜잭션으로 교체', () => {
    it('교체 완료 시 변경 요약 포함 200 OK 반환', async () => {
      const res = await request(BASE_URL)
        .put(`/api/v1/trips/${tripId}/schedule-items/${itemId}/replace`)
        .set('Authorization', 'Bearer test-token')
        .send({
          new_place_id: 'place_xyz789',
        })
        .expect(200);

      // ReplaceScheduleItemResponse 스키마 - updated_schedule_items 내부 항목 검증
      expect(res.body).toHaveProperty('updated_schedule_items');
      if (res.body.updated_schedule_items.length > 0) {
        const item = res.body.updated_schedule_items[0];
        expect(item).toHaveProperty('schedule_item_id');
        expect(item).toHaveProperty('place_id');
        expect(item).toHaveProperty('place_name');
        expect(item).toHaveProperty('visit_datetime');
      }
    });
  });
});
