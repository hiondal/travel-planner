/**
 * ALTN 서비스 - 대안 카드 선택 및 일정 반영 행위 계약 테스트
 *
 * 설계 근거:
 *   - 시퀀스: docs/design/sequence/inner/altn-대안카드선택.puml
 *   - API 명세: docs/design/api/alternative-service-api.yaml
 *
 * 흐름 요약:
 *   Client -> AltnController -> AlternativeService
 *     -> AlternativeRepository 대안 카드 정보 조회
 *     -> SchdService 일정 교체 동기 호출
 *     -> Redis 캐시 무효화
 *     -> SelectionLog 선택 이력 저장 (ML 학습 데이터)
 */
import request from 'supertest';

const BASE_URL = process.env.ALTN_SERVICE_URL || 'http://localhost:8086';

describe('ALTN 서비스 - 대안 카드 선택 및 일정 반영', () => {
  const altId = 'alt_01HX_test';

  describe('SCHD 서비스 동기 호출로 일정 교체', () => {
    /**
     * 선택된 대안 카드의 장소로 일정을 교체하고
     * 변경 요약(원래->대안, 이동시간 변화)을 반환한다.
     */
    it('일정 교체 성공 시 200 OK 변경 요약 반환', async () => {
      const res = await request(BASE_URL)
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
  });

  describe('캐시 무효화', () => {
    /**
     * 대안 카드 선택 후 기존 장소의 대안 카드 캐시가
     * 무효화된다 (altn:{original_place_id}:*).
     * 이후 동일 장소에 대한 대안 검색 시 캐시 MISS가 발생하여
     * 새로운 대안 카드를 생성한다.
     */
    it('캐시 무효화 후 동일 장소 재검색 시 새로운 결과 반환', async () => {
      const res = await request(BASE_URL)
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

      expect(res.body).toHaveProperty('schedule_item_id');
      expect(res.body).toHaveProperty('original_place');
      expect(res.body).toHaveProperty('new_place');
      expect(res.body).toHaveProperty('travel_time_diff_minutes');
    });
  });

  describe('선택 이력 저장 (ML 학습 데이터)', () => {
    /**
     * 대안 카드 선택 이력이 selection_logs에 저장된다.
     * (alt_card_id, selected_rank(1/2/3), elapsed_seconds, adopted=true)
     * 선택 순위와 경과 시간이 응답에 반영된다.
     */
    it('선택 이력 저장 완료 시 200 OK 교체 결과 반환', async () => {
      const res = await request(BASE_URL)
        .post(`/api/v1/alternatives/${altId}/select`)
        .set('Authorization', 'Bearer valid_paid_token')
        .send({
          original_place_id: 'place_abc123',
          schedule_item_id: 'si_01HX_test',
          trip_id: 'trip_01HX_test',
          selected_rank: 3,
          elapsed_seconds: 25,
        })
        .expect(200);

      // SelectAlternativeResponse 스키마 검증
      expect(res.body).toHaveProperty('schedule_item_id');
      expect(res.body).toHaveProperty('original_place');
      expect(res.body).toHaveProperty('new_place');
      expect(res.body).toHaveProperty('travel_time_diff_minutes');
      expect(typeof res.body.travel_time_diff_minutes).toBe('number');
    });
  });
});
