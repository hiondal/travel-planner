# AUTH 서비스 — 패키지 구조도

## 개요

| 항목 | 값 |
|------|---|
| 서비스 ID | AUTH |
| 모듈 경로 | `auth/` |
| 루트 패키지 | `com.travelplanner.auth` |
| 포트 | 8081 |
| Spring Boot 진입점 | `AuthApplication` |
| DB | PostgreSQL `auth` 데이터베이스 |
| Redis | DB0 (JWT 블랙리스트), DB1 (Refresh Token / 세션) |

---

## 패키지 트리

```
com.travelplanner.auth
│
├── AuthApplication.java                         ← Spring Boot 진입점
│
├── config/
│   ├── SecurityConfig.java                      ← Spring Security + OAuth2 + JWT 필터 설정
│   ├── SwaggerConfig.java                       ← SpringDoc OpenAPI 설정
│   ├── RedisConfig.java                         ← Redis 멀티 DB 설정 (DB0, DB1)
│   ├── jwt/
│   │   ├── JwtAuthenticationFilter.java         ← OncePerRequestFilter: Bearer 토큰 검증
│   │   └── JwtAuthenticationEntryPoint.java     ← 401 응답 처리
│   └── oauth2/
│       ├── CustomOAuth2UserService.java          ← OAuth2 사용자 정보 로딩
│       ├── OAuth2SuccessHandler.java             ← 소셜 로그인 성공 후 JWT 발급
│       └── OAuth2FailureHandler.java             ← 소셜 로그인 실패 처리
│
├── controller/
│   └── AuthController.java                      ← REST API 엔드포인트 (5개)
│       - POST /api/v1/auth/social-login
│       - POST /api/v1/auth/token/refresh
│       - POST /api/v1/auth/logout
│       - POST /api/v1/auth/token/invalidate
│       - POST /api/v1/users/consent
│
├── service/
│   ├── AuthService.java                         ← 서비스 인터페이스
│   └── AuthServiceImpl.java                     ← 서비스 구현체
│
├── repository/
│   ├── UserRepository.java                      ← Spring Data JPA
│   ├── ConsentRepository.java                   ← Spring Data JPA
│   ├── RefreshTokenRepository.java              ← Spring Data JPA (PostgreSQL 영속화)
│   └── AuthSessionRedisRepository.java          ← RedisTemplate 기반 세션 저장
│
├── client/
│   ├── OAuthClient.java                         ← Google OAuth2 토큰 교환
│   └── OAuthProfile.java                        ← OAuth 프로필 VO
│
├── domain/
│   ├── User.java                                ← @Entity: users 테이블
│   ├── Consent.java                             ← @Entity: consents 테이블
│   └── RefreshToken.java                        ← @Entity: refresh_tokens 테이블
│
└── dto/
    ├── request/
    │   ├── SocialLoginRequest.java
    │   ├── TokenRefreshRequest.java
    │   ├── LogoutRequest.java
    │   ├── TokenInvalidateRequest.java
    │   └── ConsentRequest.java
    ├── response/
    │   ├── SocialLoginResponse.java
    │   ├── TokenRefreshResponse.java
    │   ├── TokenInvalidateResponse.java
    │   ├── ConsentResponse.java
    │   └── UserProfileDto.java
    └── internal/
        ├── SocialLoginResult.java
        ├── TokenRefreshResult.java
        └── TokenInvalidateResult.java
```

---

## API 매핑

| 메서드 | 경로 | 인증 필요 | 설명 |
|--------|------|----------|------|
| POST | /api/v1/auth/social-login | 없음 | Google OAuth 코드 → JWT 발급 |
| POST | /api/v1/auth/token/refresh | 없음 | Refresh Token → 새 Access Token |
| POST | /api/v1/auth/logout | Bearer JWT | 로그아웃 (토큰 무효화) |
| POST | /api/v1/auth/token/invalidate | Bearer JWT | 구독 티어 변경 시 토큰 재발급 |
| POST | /api/v1/users/consent | Bearer JWT | 위치/Push 동의 저장 |

---

## Redis 키 패턴

| DB | 키 패턴 | TTL | 용도 |
|----|---------|-----|------|
| DB0 | `auth:blacklist:{jti}` | Access Token 잔여 유효시간 | JWT 블랙리스트 |
| DB1 | `auth:refresh:{userId}` | 30일 | userId → refreshToken 역방향 조회 |
| DB1 | `auth:session:{refreshToken}` | 30일 | refreshToken → 세션 정보 (Hash) |

---

## 의존 관계

- common 모듈: `JwtProvider`, `UserPrincipal`, `JwtToken`, `BaseTimeEntity`, `SubscriptionTier`, `OAuthProvider`
- 외부: Google OAuth2 Token Endpoint (https://oauth2.googleapis.com/token)
- DB: PostgreSQL `auth` 데이터베이스 (users, consents, refresh_tokens)
- Cache: Redis DB0 (블랙리스트), DB1 (세션)
