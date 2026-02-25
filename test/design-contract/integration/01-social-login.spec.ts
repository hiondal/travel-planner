/**
 * Integration 테스트 - 소셜 로그인 (AUTH)
 *
 * 설계 근거:
 *   - 시퀀스: docs/design/sequence/outer/01-소셜로그인.puml
 *   - API 명세: docs/design/api/auth-service-api.yaml
 *
 * 흐름 요약:
 *   모바일 클라이언트 -> API Gateway -> AUTH 서비스 -> Google/Apple OAuth 검증
 *     -> Redis 세션 저장 -> JWT 토큰 + 프로필 반환
 */
import request from 'supertest';

const AUTH_URL = process.env.AUTH_SERVICE_URL || 'http://localhost:8081';

describe('소셜 로그인 (AUTH)', () => {
  describe('Google/Apple 로그인 요청', () => {
    describe('OAuth 검증', () => {
      /**
       * OAuth 검증 성공 시 세션 정보가 Redis에 저장되고
       * access_token(30분), refresh_token, user_profile이 반환된다.
       */
      it('OAuth 검증 성공 시 200 OK 토큰 + 프로필 반환', async () => {
        const res = await request(AUTH_URL)
          .post('/api/v1/auth/social-login')
          .send({
            provider: 'google',
            oauth_code: '4/0AX4XfWh...',
          })
          .expect(200);

        // SocialLoginResponse 스키마 검증
        expect(res.body).toHaveProperty('access_token');
        expect(typeof res.body.access_token).toBe('string');

        expect(res.body).toHaveProperty('refresh_token');
        expect(typeof res.body.refresh_token).toBe('string');

        expect(res.body).toHaveProperty('user_profile');
        expect(res.body.user_profile).toHaveProperty('user_id');
        expect(typeof res.body.user_profile.user_id).toBe('string');

        expect(res.body.user_profile).toHaveProperty('nickname');
        expect(typeof res.body.user_profile.nickname).toBe('string');

        expect(res.body.user_profile).toHaveProperty('avatar_url');
        expect(typeof res.body.user_profile.avatar_url).toBe('string');

        expect(res.body.user_profile).toHaveProperty('tier');
        expect(['FREE', 'TRIP_PASS', 'PRO']).toContain(res.body.user_profile.tier);

        expect(res.body.user_profile).toHaveProperty('is_new_user');
        expect(typeof res.body.user_profile.is_new_user).toBe('boolean');
      });

      /**
       * OAuth 인증 코드가 유효하지 않아 검증에 실패하면
       * 401 Unauthorized를 반환한다.
       */
      it('OAuth 검증 실패 시 401 Unauthorized 반환', async () => {
        const res = await request(AUTH_URL)
          .post('/api/v1/auth/social-login')
          .send({
            provider: 'google',
            oauth_code: 'invalid_oauth_code',
          })
          .expect(401);

        expect(res.body).toHaveProperty('error');
        expect(typeof res.body.error).toBe('string');

        expect(res.body).toHaveProperty('message');
        expect(typeof res.body.message).toBe('string');
      });

      /**
       * OAuth 서버와의 네트워크 오류 시
       * 503 Service Unavailable을 반환한다.
       */
      it('네트워크 오류 시 503 Service Unavailable 반환', async () => {
        const res = await request(AUTH_URL)
          .post('/api/v1/auth/social-login')
          .send({
            provider: 'google',
            oauth_code: 'network_error_code',
          })
          .expect(503);

        expect(res.body).toHaveProperty('error');
        expect(typeof res.body.error).toBe('string');

        expect(res.body).toHaveProperty('message');
        expect(typeof res.body.message).toBe('string');
      });
    });
  });
});
