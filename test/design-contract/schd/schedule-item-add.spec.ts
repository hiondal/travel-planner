/**
 * SCHD 서비스 - 장소 추가 행위 계약 테스트
 *
 * 설계 근거: schd-장소추가.puml
 * API 명세: schedule-service-api.yaml - POST /trips/{trip_id}/schedule-items
 */
import request from 'supertest';

const BASE_URL = process.env.SCHD_SERVICE_URL || 'http://localhost:8082';

describe('SCHD 서비스 - 장소 추가', () => {
  const tripId = 'trip_01HX_test';

  it('영업시간 내 시 201 Created 장소 추가 성공', async () => {
    const res = await request(BASE_URL)
      .post(`/api/v1/trips/${tripId}/schedule-items`)
      .set('Authorization', 'Bearer test-token')
      .send({
        place_id: 'place_abc123',
        visit_datetime: '2026-03-16T12:00:00+09:00',
        timezone: 'Asia/Tokyo',
      })
      .expect(201);

    // ScheduleItemResponse 스키마 검증
    expect(res.body).toHaveProperty('schedule_item_id');
    expect(res.body).toHaveProperty('place_id');
    expect(res.body).toHaveProperty('place_name');
    expect(res.body).toHaveProperty('visit_datetime');
    expect(res.body).toHaveProperty('timezone');
    expect(res.body).toHaveProperty('order');
    expect(res.body).toHaveProperty('outside_business_hours');
    expect(res.body.outside_business_hours).toBe(false);
  });

  it('영업시간 외 시 경고 포함 200 OK 반환', async () => {
    const res = await request(BASE_URL)
      .post(`/api/v1/trips/${tripId}/schedule-items`)
      .set('Authorization', 'Bearer test-token')
      .send({
        place_id: 'place_abc123',
        visit_datetime: '2026-03-16T09:00:00+09:00',
        timezone: 'Asia/Tokyo',
      })
      .expect(200);

    // BusinessHoursWarningResponse 스키마 검증
    expect(res.body).toHaveProperty('warning');
    expect(res.body).toHaveProperty('message');
    expect(res.body).toHaveProperty('business_hours');
    expect(res.body.warning).toBe('OUTSIDE_BUSINESS_HOURS');
  });

  /**
   * 설계 노트: force=true 파라미터로 재요청 시 영업시간 외 마킹하여 저장
   */
  it('영업시간 외 force=true 재요청 시 201 Created 강제 추가 성공', async () => {
    const res = await request(BASE_URL)
      .post(`/api/v1/trips/${tripId}/schedule-items`)
      .set('Authorization', 'Bearer test-token')
      .send({
        place_id: 'place_abc123',
        visit_datetime: '2026-03-16T09:00:00+09:00',
        timezone: 'Asia/Tokyo',
        force: true,
      })
      .expect(201);

    // ScheduleItemResponse 스키마 검증 (영업시간 외 마킹)
    expect(res.body).toHaveProperty('schedule_item_id');
    expect(res.body).toHaveProperty('place_id');
    expect(res.body).toHaveProperty('outside_business_hours');
    expect(res.body.outside_business_hours).toBe(true);
  });
});
