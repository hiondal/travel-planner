/**
 * Integration 테스트 - 온보딩 (SCHD)
 *
 * 설계 근거:
 *   - 시퀀스: docs/design/sequence/outer/02-온보딩.puml
 *   - API 명세: docs/design/api/schedule-service-api.yaml
 *
 * 흐름 요약:
 *   모바일 클라이언트 -> API Gateway -> SCHD 서비스
 *     -> 온보딩 상태 조회 -> 온보딩 완료 처리 + 샘플 일정 생성
 */
import request from 'supertest';

const SCHD_URL = process.env.SCHD_SERVICE_URL || 'http://localhost:8082';

describe('온보딩 (SCHD)', () => {
  describe('최초 로그인 후 온보딩 표시', () => {
    /**
     * 앱 최초 실행 시 온보딩 상태를 조회하여
     * 미완료 상태인 경우 onboarding_completed: false를 반환한다.
     */
    it('온보딩 미완료 시 onboarding_completed false 반환', async () => {
      const res = await request(SCHD_URL)
        .get('/api/v1/users/onboarding-status')
        .set('Authorization', 'Bearer valid_access_token')
        .expect(200);

      expect(res.body).toHaveProperty('onboarding_completed');
      expect(res.body.onboarding_completed).toBe(false);
    });
  });

  describe('온보딩 완료 처리', () => {
    /**
     * 온보딩 완료 버튼 탭 또는 건너뛰기 시
     * 샘플 일정이 생성되고 onboarding_completed: true를 반환한다.
     */
    it('온보딩 완료 시 200 OK 샘플 일정 ID 및 완료 상태 반환', async () => {
      const res = await request(SCHD_URL)
        .post('/api/v1/users/onboarding-complete')
        .set('Authorization', 'Bearer valid_access_token')
        .expect(200);

      expect(res.body).toHaveProperty('sample_trip_id');
      expect(typeof res.body.sample_trip_id).toBe('string');

      expect(res.body).toHaveProperty('onboarding_completed');
      expect(res.body.onboarding_completed).toBe(true);
    });
  });
});
