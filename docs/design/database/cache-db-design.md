# 캐시 데이터베이스 설계서

## 데이터설계 요약

| 항목 | 내용 |
|------|------|
| 캐시 시스템 | Redis 7.x |
| 분리 방식 | Redis database 번호 (0~15) |
| 총 database 사용 | 10개 (DB 0~8, DB 15) |
| Key Naming | `{domain}:{entity}:{id}` 형식 |

---

## 1. 개요

### 1.1 설계 목적
Travel Planner 마이크로서비스 아키텍처에서 각 서비스별 캐시 영역을 논리적으로 분리하여
데이터 격리와 효율적인 캐시 관리를 구현한다.

### 1.2 설계 원칙
- **논리적 분리**: Redis database 번호(0~15)로 서비스별 캐시 영역 분리
- **격리 원칙**: 각 서비스는 자신에게 할당된 database만 접근
- **Key Naming**: `{domain}:{entity}:{id}` 형식 통일
- **TTL 정책**: 데이터 특성에 따라 차등 TTL 적용

---

## 2. Redis Database 할당표

| Database | 서비스 | 용도 | 비고 |
|:--------:|--------|------|------|
| **DB 0** | 공통 영역 | JWT 블랙리스트, 세션 공유 | 모든 서비스 접근 가능 |
| **DB 1** | AUTH | Refresh Token, 세션 정보 | 인증 전용 |
| **DB 2** | SCHEDULE (SCHD) | 여행/일정 캐시 | 일정 관리 전용 |
| **DB 3** | PLACE (PLCE) | 장소 검색·상세 캐시 | Google Places API 응답 캐시 |
| **DB 4** | MONITOR (MNTR) | 상태 배지·수집 데이터 캐시 | 실시간 상태 전용 |
| **DB 5** | BRIEFING (BRIF) | 브리핑·멱등성 캐시 | 브리핑 서비스 전용 |
| **DB 6** | ALTERNATIVE (ALTN) | 대안 카드 캐시 | 대안 추천 전용 |
| **DB 7** | PAYMENT (PAY) | 구독 상태 캐시 | 결제 서비스 전용 |
| **DB 8** | AI Pipeline | AI 응답 캐시, 프롬프트 캐시 | Phase 2+ 도입 |
| **DB 9~14** | 예비 | 향후 서비스 확장용 | 미사용 |
| **DB 15** | 예비 | 긴급 백업/마이그레이션 | 미사용 |

---

## 3. 서비스별 캐시 설계

### 3.1 공통 영역 (DB 0)

#### 캐시 대상
| 캐시 키 패턴 | 설명 | TTL | 데이터 타입 |
|--------------|------|-----|-------------|
| `auth:blacklist:{jti}` | 무효화된 JWT Access Token | Access Token 잔여 유효시간 | String |

#### 캐시 무효화 정책
- 로그아웃 시 access token JTI 등록
- TTL 자동 만료 (access token 유효시간과 동일)

#### 설정 예시
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      database: 0
```

---

### 3.2 AUTH 서비스 (DB 1)

#### 캐시 대상
| 캐시 키 패턴 | 설명 | TTL | 데이터 타입 |
|--------------|------|-----|-------------|
| `auth:refresh:{userId}` | userId → refreshToken 역방향 조회 | 30일 | String |
| `auth:session:{refreshToken}` | 세션 정보 (userId, tier, expiresAt) | 30일 | Hash |

#### Hash 필드 (auth:session:{refreshToken})
| 필드 | 타입 | 설명 |
|------|------|------|
| userId | String | 사용자 ID |
| tier | String | 구독 등급 |
| expiresAt | String | 만료 일시 (ISO 8601) |

#### 캐시 무효화 정책
- 로그아웃: `auth:refresh:{userId}`, `auth:session:{refreshToken}` 동시 삭제
- 강제 갱신(invalidateAndReissue): 기존 키 삭제 후 신규 등록

#### 설정 예시
```yaml
spring:
  data:
    redis:
      database: 1
```

---

### 3.3 SCHEDULE 서비스 (DB 2)

#### 캐시 대상
| 캐시 키 패턴 | 설명 | TTL | 데이터 타입 |
|--------------|------|-----|-------------|
| `schd:trip:{tripId}` | 여행 기본 정보 | 10분 | Hash |
| `schd:schedule:{tripId}` | 일정 아이템 목록 (JSON) | 5분 | String |

#### Hash 필드 (schd:trip:{tripId})
| 필드 | 타입 | 설명 |
|------|------|------|
| userId | String | 소유자 ID |
| name | String | 여행명 |
| startDate | String | 시작일 |
| endDate | String | 종료일 |
| city | String | 도시 |
| status | String | 상태 |

#### 캐시 무효화 정책
- 일정 아이템 추가/삭제/교체 시: `schd:schedule:{tripId}` 삭제
- 여행 상태 변경 시: `schd:trip:{tripId}` 삭제

#### 설정 예시
```yaml
spring:
  data:
    redis:
      database: 2
```

---

### 3.4 PLACE 서비스 (DB 3)

#### 캐시 대상
| 캐시 키 패턴 | 설명 | TTL | 데이터 타입 |
|--------------|------|-----|-------------|
| `plce:detail:{placeId}` | 장소 상세 (영업시간 포함) | 1시간 | String (JSON) |
| `plce:search:{keyword}:{city}` | 텍스트 검색 결과 목록 | 30분 | String (JSON) |
| `plce:nearby:{latLng}:{category}:{radius}` | 근처 장소 검색 결과 | 15분 | String (JSON) |

#### 키 생성 규칙
- `{latLng}`: 위경도 소수점 3자리 반올림 (예: `35.681_139.767`)
- `{keyword}:{city}`: 소문자 정규화 후 URL 인코딩
- `{radius}`: 미터 단위 정수

#### 캐시 무효화 정책
- Google Places API 신규 조회 시: write-through로 즉시 갱신
- updated_at 24시간 초과 시: 강제 refresh

#### 설정 예시
```yaml
spring:
  data:
    redis:
      database: 3
```

---

### 3.5 MONITOR 서비스 (DB 4)

#### 캐시 대상
| 캐시 키 패턴 | 설명 | TTL | 데이터 타입 |
|--------------|------|-----|-------------|
| `mntr:badge:{placeId}` | 상태 배지 (status, icon, color) | 5분 | Hash |
| `mntr:collected:{placeId}` | 최신 수집 데이터 (폴백용) | 30분 | String (JSON) |
| `mntr:status:{placeId}` | 현재 상태 요약 | 5분 | Hash |

#### Hash 필드 (mntr:badge:{placeId})
| 필드 | 타입 | 설명 |
|------|------|------|
| status | String | GREEN/YELLOW/RED/GREY |
| icon | String | CHECK/EXCLAMATION/X/QUESTION |
| label | String | 표시 텍스트 |
| colorHex | String | HEX 색상 코드 |
| updatedAt | String | 마지막 업데이트 시각 (ISO 8601) |

#### 캐시 무효화 정책
- 상태 판정 완료 시: `mntr:badge:{placeId}`, `mntr:status:{placeId}` 즉시 갱신
- `PlaceStatusChangedEvent` 발행으로 briefing/alternative 서비스 캐시 무효화 연계

#### 설정 예시
```yaml
spring:
  data:
    redis:
      database: 4
```

---

### 3.6 BRIEFING 서비스 (DB 5)

#### 캐시 대상
| 캐시 키 패턴 | 설명 | TTL | 데이터 타입 |
|--------------|------|-----|-------------|
| `brif:briefing:{briefingId}` | 브리핑 상세 정보 | 30분 | String (JSON) |
| `brif:list:{userId}:{date}` | 날짜별 브리핑 목록 | 10분 | String (JSON) |
| `brif:count:{userId}:{date}` | 일별 브리핑 생성 카운트 | 해당일 자정까지 | String |
| `brif:idem:{idempotencyKey}` | 멱등성 체크 | 2시간 | String (briefingId) |

#### 캐시 무효화 정책
- 브리핑 생성 완료: `brif:list:{userId}:{date}` 삭제
- `brif:count` TTL: 해당일 자정으로 설정 (일별 자동 리셋)

#### 설정 예시
```yaml
spring:
  data:
    redis:
      database: 5
```

---

### 3.7 ALTERNATIVE 서비스 (DB 6)

#### 캐시 대상
| 캐시 키 패턴 | 설명 | TTL | 데이터 타입 |
|--------------|------|-----|-------------|
| `altn:cards:{placeId}:{category}:{radius}` | 대안 카드 목록 | 10분 | String (JSON) |
| `altn:alt:{altId}` | 개별 대안 상세 | 30분 | String (JSON) |

#### 캐시 무효화 정책
- `PlaceStatusChangedEvent` 수신 시: 해당 placeId 관련 카드 캐시 삭제
- 대안 선택 완료 시: `altn:cards:{placeId}:*` 패턴 삭제

#### 설정 예시
```yaml
spring:
  data:
    redis:
      database: 6
```

---

### 3.8 PAYMENT 서비스 (DB 7)

#### 캐시 대상
| 캐시 키 패턴 | 설명 | TTL | 데이터 타입 |
|--------------|------|-----|-------------|
| `pay:subscription:{userId}` | 구독 상태 (tier, status, expiresAt) | 5분 | Hash |

#### Hash 필드 (pay:subscription:{userId})
| 필드 | 타입 | 설명 |
|------|------|------|
| tier | String | FREE/TRIP_PASS/PRO |
| status | String | ACTIVE/CANCELLED/CANCELLING |
| subscriptionId | String | 구독 ID |
| expiresAt | String | 만료 일시 (ISO 8601) |

#### 캐시 무효화 정책
- 구독 구매/취소/변경 시: 즉시 삭제 (다음 조회 시 DB에서 갱신)

#### 설정 예시
```yaml
spring:
  data:
    redis:
      database: 7
```

---

### 3.9 AI Pipeline (DB 8) — Phase 2+

#### 캐시 대상
| 캐시 키 패턴 | 설명 | TTL | 데이터 타입 |
|--------------|------|-----|-------------|
| `ai:response:briefing:{hash}` | AI 브리핑 생성 응답 캐시 | 1시간 | String (JSON) |
| `ai:response:alternative:{hash}` | AI 대안 추천 응답 캐시 | 30분 | String (JSON) |
| `ai:prompt:{featureKey}:{version}` | 프롬프트 템플릿 캐시 | 24시간 | String |

#### 해시 생성 규칙
- `{hash}`: 입력 파라미터(placeId, statusLevel, context)를 SHA-256 해시
- 동일 입력에 대한 AI 응답 재사용으로 API 비용 절감

#### 캐시 무효화 정책
- 프롬프트 버전 변경 시: `ai:prompt:{featureKey}:{version}` 삭제
- 상태 변경으로 인한 컨텍스트 변경 시: `ai:response:*` 관련 키 무효화

#### 설정 예시
```yaml
spring:
  data:
    redis:
      database: 8
```

---

## 4. Key Naming Convention

### 4.1 명명 규칙
```
{서비스약어}:{엔티티}:{식별자}[:{추가구분자}]
```

### 4.2 서비스 약어
| 서비스 | 약어 |
|--------|------|
| 공통 | `auth` (공통 인증 관련) |
| AUTH | `auth` |
| SCHEDULE | `schd` |
| PLACE | `plce` |
| MONITOR | `mntr` |
| BRIEFING | `brif` |
| ALTERNATIVE | `altn` |
| PAYMENT | `pay` |
| AI Pipeline | `ai` |

### 4.3 예시
```
auth:blacklist:eyJhbGc...          # JWT 블랙리스트
auth:session:rt_abc123             # 리프레시 토큰 세션
schd:schedule:trip_xyz789          # 일정 목록
plce:detail:ChIJN1t_tDeuEmsRUcIaWtf4MzE  # Google Place 상세
mntr:badge:ChIJN1t_tDeuEmsRUcIaWtf4MzE  # 장소 상태 배지
brif:idem:ChIJN1t...:2026-02-23T09:00  # 브리핑 멱등성
altn:cards:ChIJN1t...:RESTAURANT:500    # 대안 카드 목록
pay:subscription:user_abc123       # 구독 상태
ai:response:briefing:a3f8c2d1...  # AI 브리핑 응답
```

---

## 5. 주의사항

### 5.1 Redis Cluster 모드 전환 시
- Redis Cluster는 database 0만 지원
- Cluster 전환 시 Key Prefix 방식으로 변경 필요:
  ```
  {auth}:session:userId
  {schd}:schedule:tripId
  {plce}:detail:placeId
  ```

### 5.2 공통 영역(DB 0) 접근
- 공통 영역 데이터 변경 시 모든 서비스 영향도 분석 필수
- DB 0은 AUTH 서비스가 주로 관리하며 타 서비스는 읽기 전용

### 5.3 격리 원칙 준수
- 각 서비스는 자신에게 할당된 database만 접근
- 타 서비스 database 직접 참조 금지
- 서비스 간 데이터 공유 필요 시 API 호출 또는 이벤트 기반 동기화 사용

### 5.4 캐시 용량 모니터링
- MNTR, PLCE는 외부 API 데이터를 대량으로 캐시하므로 maxmemory 정책 필요
- 권장 정책: `allkeys-lru` (메모리 초과 시 최근 미사용 키 자동 제거)

---

## 6. 관련 문서

| 서비스 | 데이터 설계서 | ERD | 스키마 |
|--------|-------------|-----|--------|
| AUTH | [auth.md](./auth.md) | [auth-erd.puml](./auth-erd.puml) | [auth-schema.psql](./auth-schema.psql) |
| SCHEDULE | [schedule.md](./schedule.md) | [schedule-erd.puml](./schedule-erd.puml) | [schedule-schema.psql](./schedule-schema.psql) |
| PLACE | [place.md](./place.md) | [place-erd.puml](./place-erd.puml) | [place-schema.psql](./place-schema.psql) |
| MONITOR | [monitor.md](./monitor.md) | [monitor-erd.puml](./monitor-erd.puml) | [monitor-schema.psql](./monitor-schema.psql) |
| BRIEFING | [briefing.md](./briefing.md) | [briefing-erd.puml](./briefing-erd.puml) | [briefing-schema.psql](./briefing-schema.psql) |
| ALTERNATIVE | [alternative.md](./alternative.md) | [alternative-erd.puml](./alternative-erd.puml) | [alternative-schema.psql](./alternative-schema.psql) |
| PAYMENT | [payment.md](./payment.md) | [payment-erd.puml](./payment-erd.puml) | [payment-schema.psql](./payment-schema.psql) |

---

**작성일**: 2026-02-23
**작성자**: 홍길동/아키
