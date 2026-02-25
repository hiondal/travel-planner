/**
 * AUTH 서비스 - 소셜 로그인 행위 계약 테스트
 *
 * 설계 근거:
 *   - 시퀀스: docs/design/sequence/inner/auth-소셜로그인.puml
 *   - API 명세: docs/design/api/auth-service-api.yaml
 *
 * 흐름 요약:
 *   Client -> AuthController -> AuthService -> Google/Apple OAuth 검증
 *     -> UserRepository 사용자 조회/생성 -> JWT 발급 -> 세션 저장
 */
import request from 'supertest';

const BASE_URL = process.env.AUTH_SERVICE_URL || 'http://localhost:8081';

describe('AUTH 서비스 - 소셜 로그인', () => {
  describe('OAuth 검증 성공', () => {
    /**
     * OAuth 검증 성공 시 사용자 프로필이 반환되고,
     * JWT Access Token + Refresh Token이 발급된다.
     * JWT 클레임: user_id, tier, email, exp
     */
    it('기존 사용자 로그인 시 200 OK와 토큰 + 프로필 반환', async () => {
      const res = await request(BASE_URL)
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

      // UserProfile 스키마 검증
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
     * 신규 사용자 (최초 로그인) 시 사용자가 자동 생성되고
     * is_new_user = true 로 반환된다.
     * 닉네임/프로필 이미지가 자동 설정된다.
     */
    it('신규 사용자 (최초 로그인) 시 사용자 자동 생성 후 is_new_user=true 반환', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/auth/social-login')
        .send({
          provider: 'google',
          oauth_code: 'new_user_oauth_code',
        })
        .expect(200);

      // SocialLoginResponse 스키마 검증
      expect(res.body).toHaveProperty('access_token');
      expect(typeof res.body.access_token).toBe('string');

      expect(res.body).toHaveProperty('refresh_token');
      expect(typeof res.body.refresh_token).toBe('string');

      // 신규 사용자 프로필 검증
      expect(res.body).toHaveProperty('user_profile');
      expect(res.body.user_profile).toHaveProperty('user_id');
      expect(res.body.user_profile).toHaveProperty('nickname');
      expect(res.body.user_profile).toHaveProperty('avatar_url');
      expect(res.body.user_profile).toHaveProperty('tier');
      expect(res.body.user_profile.is_new_user).toBe(true);
    });
  });

  describe('OAuth 검증 실패', () => {
    /**
     * OAuth 인증 코드가 유효하지 않아 검증에 실패하면
     * 401 Unauthorized를 반환한다.
     */
    it('OAuth 검증 실패 시 401 Unauthorized 반환', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/auth/social-login')
        .send({
          provider: 'google',
          oauth_code: 'invalid_oauth_code',
        })
        .expect(401);

      // ErrorResponse 스키마 검증
      expect(res.body).toHaveProperty('error');
      expect(typeof res.body.error).toBe('string');

      expect(res.body).toHaveProperty('message');
      expect(typeof res.body.message).toBe('string');
    });
  });
});
