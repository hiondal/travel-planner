# AUTH 서비스 - 데이터 설계서

## 데이터설계 요약

| 항목 | 내용 |
|------|------|
| 서비스 | AUTH (인증/인가) |
| DBMS | PostgreSQL |
| 테이블 수 | 3개 (users, consents, refresh_tokens) |
| Redis DB | DB 0 (공통: JWT 블랙리스트), DB 1 (AUTH 전용: Refresh Token) |
| 서비스 간 FK | 없음 (데이터 독립성 원칙) |

---

## 1. 개요

소셜 로그인(Google/Apple OAuth), JWT 토큰 관리, 사용자 동의 정보를 관리한다.
`AuthSession` 도메인은 Redis DB 1에 캐시 전용으로 저장하며, PostgreSQL에는 영속화하지 않는다.

---

## 2. 테이블 정의

### 2.1 users

사용자 기본 정보. OAuth 프로바이더와 프로바이더 고유 ID로 식별한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | VARCHAR(36) | PK | UUID |
| provider | VARCHAR(20) | NOT NULL | OAuth 프로바이더 (GOOGLE, APPLE) |
| provider_id | VARCHAR(200) | NOT NULL, UNIQUE | 프로바이더 고유 식별자 |
| email | VARCHAR(200) | NOT NULL | 이메일 |
| nickname | VARCHAR(100) | NOT NULL | 닉네임 |
| avatar_url | VARCHAR(500) | | 프로필 이미지 URL |
| tier | VARCHAR(20) | NOT NULL, DEFAULT 'FREE' | 구독 등급 (FREE, TRIP_PASS, PRO) |
| created_at | TIMESTAMPTZ | NOT NULL | 생성 일시 |
| updated_at | TIMESTAMPTZ | NOT NULL | 수정 일시 |

인덱스:
- `idx_users_provider_provider_id` (provider, provider_id) UNIQUE
- `idx_users_email` (email)

### 2.2 consents

사용자 동의 이력. 최신 동의 정보만 조회하므로 user_id + 생성일 기준 조회.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | VARCHAR(36) | PK | UUID |
| user_id | VARCHAR(36) | NOT NULL | users.id 참조 (서비스 내부 FK) |
| location | BOOLEAN | NOT NULL | 위치정보 수집 동의 |
| push | BOOLEAN | NOT NULL | 푸시 알림 동의 |
| consented_at | TIMESTAMPTZ | NOT NULL | 동의 일시 (사용자 입력) |
| created_at | TIMESTAMPTZ | NOT NULL | 레코드 생성 일시 |

인덱스:
- `idx_consents_user_id_created_at` (user_id, created_at DESC)

### 2.3 refresh_tokens

리프레시 토큰 영속화. Redis TTL 만료 시 폴백용.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | VARCHAR(36) | PK | UUID |
| user_id | VARCHAR(36) | NOT NULL | users.id 참조 (서비스 내부 FK) |
| refresh_token | VARCHAR(500) | NOT NULL, UNIQUE | 리프레시 토큰 값 |
| expires_at | TIMESTAMPTZ | NOT NULL | 만료 일시 |
| created_at | TIMESTAMPTZ | NOT NULL | 생성 일시 |

인덱스:
- `idx_refresh_tokens_user_id` (user_id)
- `idx_refresh_tokens_refresh_token` (refresh_token) UNIQUE
- `idx_refresh_tokens_expires_at` (expires_at) — 만료 토큰 배치 삭제용

---

## 3. Redis 캐시 설계

### 3.1 DB 0 — 공통 영역 (JWT 블랙리스트)

| 키 패턴 | 설명 | TTL | 데이터 타입 |
|---------|------|-----|-------------|
| `auth:blacklist:{jti}` | 무효화된 JWT (로그아웃/강제 갱신) | Access Token 잔여 유효시간 | String |

캐시 무효화:
- 로그아웃 요청 시 해당 access token의 JTI를 블랙리스트에 등록
- TTL을 access token 만료 시간으로 설정하여 자동 제거

### 3.2 DB 1 — AUTH 전용 (Refresh Token)

| 키 패턴 | 설명 | TTL | 데이터 타입 |
|---------|------|-----|-------------|
| `auth:refresh:{userId}` | 리프레시 토큰 (userId → token 역방향 조회) | 30일 | String |
| `auth:session:{refreshToken}` | 세션 정보 (token → userId 조회) | 30일 | Hash |

Hash 필드 (`auth:session:{refreshToken}`):
- `userId`: 사용자 ID
- `tier`: 구독 등급
- `expiresAt`: 만료 일시 (ISO 8601)

캐시 무효화:
- 로그아웃 시 `auth:refresh:{userId}`, `auth:session:{refreshToken}` 동시 삭제
- 토큰 강제 갱신(invalidateAndReissue) 시 기존 토큰 키 삭제 후 신규 등록

---

## 4. 데이터 흐름

```
소셜 로그인:
  OAuth Code → OAuthClient.verify() → OAuthProfile
  → findOrCreateUser() → users 테이블 upsert
  → JwtProvider.generate() → refresh_token Redis 저장
  → consents 확인 (없으면 신규 사용자 플래그)

토큰 갱신:
  refresh_token → Redis DB1 조회 (Cache-Aside)
  → miss 시 refresh_tokens 테이블 조회
  → 유효하면 새 access_token 발급

로그아웃:
  access_token JTI → Redis DB0 블랙리스트 등록
  → refresh_token → Redis DB1 삭제
  → refresh_tokens 테이블 삭제
```

---

## 5. 설계 결정 사항

- `AuthSession`은 Redis 전용(TTL 기반 자동 만료)이며 PostgreSQL에 persist하지 않음. 단, 재시작/장애 복구를 위해 `refresh_tokens` 테이블에 중복 저장
- `consents`는 이력 append 방식(insert-only). `findLatestByUserId`로 최신 동의 조회
- `users.tier`는 payment 서비스의 구독 완료 이벤트 수신 시 업데이트 (이벤트 기반 동기화)
