# 백킹서비스 설치 결과서

## 구성 환경

| 항목 | 값 |
|------|---|
| 환경 | docker-compose (로컬 개발) |
| 작성 일시 | 2026-02-24 |
| 활성화 프로파일 | 기본(postgres + redis) / mock(선택, Prism Mock 포함) |
| MQ | Phase 1 — InMemoryEventPublisher 사용, docker-compose 미포함 |

---

## 서비스 연결 정보

### PostgreSQL

| 항목 | 값 |
|------|---|
| Image | postgres:16 |
| Host | localhost |
| Port | 5432 |
| User | travel |
| Password | P@ssw0rd$ |

#### 서비스별 Database

| 서비스 | Database | JDBC URL |
|--------|----------|----------|
| AUTH | auth | jdbc:postgresql://localhost:5432/auth |
| SCHD | schedule | jdbc:postgresql://localhost:5432/schedule |
| PLCE | place | jdbc:postgresql://localhost:5432/place |
| MNTR | monitor | jdbc:postgresql://localhost:5432/monitor |
| BRIF | briefing | jdbc:postgresql://localhost:5432/briefing |
| ALTN | alternative | jdbc:postgresql://localhost:5432/alternative |
| PAY | payment | jdbc:postgresql://localhost:5432/payment |

초기화 스크립트: `docker/postgres/init/01-create-databases.sql`
컨테이너 최초 기동 시 자동 실행되어 7개 데이터베이스를 생성합니다.

---

### Redis

| 항목 | 값 |
|------|---|
| Image | redis:7-alpine |
| Host | localhost |
| Port | 6379 |

#### Redis DB 할당

| DB | 서비스 | 용도 |
|----|--------|------|
| DB 0 | 공통 | JWT 블랙리스트 |
| DB 1 | AUTH | Refresh Token, 세션 |
| DB 2 | SCHD | 여행/일정 캐시 |
| DB 3 | PLCE | 장소 검색 결과 캐시 |
| DB 4 | MNTR | 상태 배지 캐시 |
| DB 5 | BRIF | 브리핑 캐시 |
| DB 6 | ALTN | 대안 카드 캐시 |
| DB 7 | PAY | 구독 상태 캐시 |

---

### Prism Mock (mock 프로파일)

API 명세 파일별로 독립 컨테이너를 기동합니다.

| 서비스 | 포트 | OpenAPI 명세 |
|--------|------|-------------|
| AUTH | 4010 | docs/design/api/auth-service-api.yaml |
| SCHD | 4011 | docs/design/api/schedule-service-api.yaml |
| PLCE | 4012 | docs/design/api/place-service-api.yaml |
| MNTR | 4013 | docs/design/api/monitor-service-api.yaml |
| BRIF | 4014 | docs/design/api/briefing-service-api.yaml |
| ALTN | 4015 | docs/design/api/alternative-service-api.yaml |
| PAY | 4016 | docs/design/api/payment-service-api.yaml |
| AI Pipeline | 4017 | docs/design/api/ai-pipeline-api.yaml |

---

## 기동 명령어

```bash
# .env 파일 준비 (최초 1회)
cp .env.example .env

# 기본 서비스 (PostgreSQL + Redis)
docker compose up -d

# Mock 서버 포함 (Prism 8개 서비스 추가 기동)
docker compose --profile mock up -d

# 상태 확인
docker compose ps

# 로그 확인
docker compose logs -f postgres
docker compose logs -f redis

# 중지 (볼륨 보존)
docker compose down

# 중지 + 볼륨 삭제 (DB 초기화 필요 시)
docker compose down -v
```

---

## 연결 확인

```bash
# PostgreSQL readiness
docker compose exec postgres pg_isready -U travel

# PostgreSQL 데이터베이스 목록 확인
docker compose exec postgres psql -U travel -c "\l"

# Redis ping
docker compose exec redis redis-cli ping

# Prism Mock (mock 프로파일 기동 후)
curl http://localhost:4010/
curl http://localhost:4011/
```

### 체크리스트

- [ ] PostgreSQL 컨테이너 기동 및 healthcheck 통과
- [ ] 7개 데이터베이스(auth, schedule, place, monitor, briefing, alternative, payment) 생성 확인
- [ ] Redis 컨테이너 기동 및 PING → PONG 응답 확인
- [ ] (mock 프로파일) Prism 8개 컨테이너 기동 확인
- [ ] `.env` 파일이 `.gitignore`에 포함되어 있음을 확인

---

## 설계 결정 사항

### MQ 미포함 이유

Phase 1 MVP는 Spring `ApplicationEventPublisher` 기반의 `InMemoryEventPublisher`를 사용합니다.
RabbitMQ/Kafka는 Phase 2 이후 도입 예정이며, 그 시점에 docker-compose에 추가합니다.

### Prism Mock 서비스별 분리 이유

단일 Prism 인스턴스는 하나의 OpenAPI yaml만 서빙합니다.
8개 API 명세를 모두 mock으로 활용하기 위해 서비스별로 컨테이너를 분리하고 포트를 4010~4017로 매핑했습니다.
