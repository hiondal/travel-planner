/**
 * SCHD 서비스 - 여행 일정 생성 행위 계약 테스트
 *
 * 설계 근거: schd-여행일정생성.puml
 * API 명세: schedule-service-api.yaml - POST /trips
 */
import request from 'supertest';

const BASE_URL = process.env.SCHD_SERVICE_URL || 'http://localhost:8082';

describe('SCHD 서비스 - 여행 일정 생성', () => {
  /**
   * 시퀀스: POST /api/v1/trips {name, start_date, end_date, city}
   * 분기: alt 첫 번째 여행 (동의 이력 없음) / else 동의 완료
   */

  it('첫 번째 여행 (동의 이력 없음) 시 428 Precondition Required 반환', async () => {
    const res = await request(BASE_URL)
      .post('/api/v1/trips')
      .set('Authorization', 'Bearer test-token')
      .send({
        name: '도쿄 3박4일',
        start_date: '2026-03-15',
        end_date: '2026-03-18',
        city: '도쿄',
      })
      .expect(428);

    // ConsentRequiredResponse 스키마 검증
    expect(res.body).toHaveProperty('error');
    expect(res.body).toHaveProperty('message');
    expect(res.body).toHaveProperty('consent_required');
    expect(res.body.consent_required).toBe(true);
  });

  it('동의 완료 시 201 Created 여행 일정 생성 성공', async () => {
    const res = await request(BASE_URL)
      .post('/api/v1/trips')
      .set('Authorization', 'Bearer test-token')
      .send({
        name: '도쿄 3박4일',
        start_date: '2026-03-15',
        end_date: '2026-03-18',
        city: '도쿄',
      })
      .expect(201);

    // TripResponse 스키마 검증
    expect(res.body).toHaveProperty('trip_id');
    expect(res.body).toHaveProperty('name');
    expect(res.body).toHaveProperty('start_date');
    expect(res.body).toHaveProperty('end_date');
    expect(res.body).toHaveProperty('city');
    expect(res.body).toHaveProperty('status');
    expect(res.body).toHaveProperty('schedule_items');
    expect(Array.isArray(res.body.schedule_items)).toBe(true);
    expect(res.body.schedule_items).toHaveLength(0);
    expect(res.body.status).toBe('ACTIVE');
  });
});
