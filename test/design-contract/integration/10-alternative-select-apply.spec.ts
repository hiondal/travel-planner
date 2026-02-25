/**
 * Integration 테스트 - 대안 카드 선택 및 일정 반영 (ALTN, SCHD)
 *
 * 설계 근거:
 *   - 시퀀스: docs/design/sequence/outer/10-대안카드선택및일정반영.puml
 *   - API 명세: docs/design/api/alternative-service-api.yaml
 *                docs/design/api/schedule-service-api.yaml
 *
 * 흐름 요약:
 *   모바일 -> ALTN 서비스 (대안 카드 선택)
 *     -> SCHD 서비스 (일정 교체 동기 호출, Google Directions 이동시간 재계산)
 *     -> 이벤트 버스 (ScheduleItemReplaced) -> MNTR 서비스 (모니터링 대상 변경)
 *     -> ALTN 서비스 (선택 이력 저장)
 */
import request from 'supertest';

const ALTN_URL = process.env.ALTN_SERVICE_URL || 'http://localhost:8086';

describe('대안 카드 선택 및 일정 반영 (ALTN, SCHD)', () => {
  describe('대안 카드 선택', () => {
    describe('일정 교체 (SCHD 동기 호출)', () => {
      /**
       * Google Directions API가 정상 응답하면
       * 이동시간이 재계산되어 일정 교체가 완료된다.
       * 원래 장소 교체 + 이동시간 갱신이 단일 DB 트랜잭션으로 처리된다.
       */
      it('Google Directions API 정상 응답 시 일정 교체 완료', async () => {
        const altId = 'alt_01HX_test';
        const res = await request(ALTN_URL)
          .post(`/api/v1/alternatives/${altId}/select`)
          .set('Authorization', 'Bearer valid_paid_token')
          .send({
            original_place_id: 'place_abc123',
            schedule_item_id: 'si_01HX_test',
            trip_id: 'trip_01HX_test',
            selected_rank: 1,
            elapsed_seconds: 12,
          })
          .expect(200);

        // SelectAlternativeResponse 스키마 검증
        expect(res.body).toHaveProperty('schedule_item_id');
        expect(typeof res.body.schedule_item_id).toBe('string');

        expect(res.body).toHaveProperty('original_place');
        expect(res.body.original_place).toHaveProperty('place_id');
        expect(typeof res.body.original_place.place_id).toBe('string');
        expect(res.body.original_place).toHaveProperty('name');
        expect(typeof res.body.original_place.name).toBe('string');

        expect(res.body).toHaveProperty('new_place');
        expect(res.body.new_place).toHaveProperty('place_id');
        expect(typeof res.body.new_place.place_id).toBe('string');
        expect(res.body.new_place).toHaveProperty('name');
        expect(typeof res.body.new_place.name).toBe('string');

        expect(res.body).toHaveProperty('travel_time_diff_minutes');
        expect(typeof res.body.travel_time_diff_minutes).toBe('number');
      });

      /**
       * Google Directions API가 실패하면 (Fallback)
       * 직선거리 기반 추정값을 사용하여 교체를 수행한다.
       */
      it('API 실패 (Fallback) 시 직선거리 기반 추정값으로 일정 교체 완료', async () => {
        const altId = 'alt_02HX_test';
        const res = await request(ALTN_URL)
          .post(`/api/v1/alternatives/${altId}/select`)
          .set('Authorization', 'Bearer valid_paid_token')
          .send({
            original_place_id: 'place_abc123',
            schedule_item_id: 'si_01HX_test',
            trip_id: 'trip_01HX_test',
            selected_rank: 2,
            elapsed_seconds: 8,
          })
          .expect(200);

        // 폴백 시에도 동일 스키마
        expect(res.body).toHaveProperty('schedule_item_id');
        expect(res.body).toHaveProperty('original_place');
        expect(res.body).toHaveProperty('new_place');
        expect(res.body).toHaveProperty('travel_time_diff_minutes');
        expect(typeof res.body.travel_time_diff_minutes).toBe('number');
      });
    });
  });
});
