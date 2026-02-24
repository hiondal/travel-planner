# ALTN 서비스 — 패키지 구조도

## 개요

| 항목 | 값 |
|------|---|
| 서비스 ID | ALTN |
| 모듈 경로 | `alternative/` |
| 루트 패키지 | `com.travelplanner.alternative` |
| 포트 | 8086 |
| Spring Boot 진입점 | `AlternativeApplication` |
| DB | PostgreSQL `alternative` 데이터베이스 |
| Redis | DB6 (대안 카드 캐시) |

---

## 패키지 트리

```
com.travelplanner.alternative
│
├── AlternativeApplication.java                    ← Spring Boot 진입점
│
├── config/
│   ├── SecurityConfig.java                        ← Phase 1: 전체 permitAll (Phase 3 JWT 적용 예정)
│   ├── RedisConfig.java                           ← Redis DB6 설정
│   └── AppConfig.java                             ← RestTemplate, ObjectMapper(JavaTimeModule) Bean
│
├── controller/
│   └── AltnController.java                        ← 대안 검색/선택 API
│       - POST /api/v1/alternatives/search         (FREE 티어 페이월 체크)
│       - POST /api/v1/alternatives/{altId}/select
│
├── service/
│   ├── AlternativeService.java                    ← 서비스 인터페이스
│   └── AlternativeServiceImpl.java                ← 서비스 구현체
│       · 반경 단계별 검색 (1000m → 2000m → 3000m, 3개 이상이면 중단)
│       · RED 상태 필터링, YELLOW/GREY 레이블 부여
│       · ScoreWeightsProvider → ScoreCalculator → 상위 3개 선택
│       · DB 저장 + ML 스냅샷 저장 + Redis 캐시 (10분 TTL)
│       · 선택 시 SCHD 서비스 교체 요청 + 선택 로그 저장
│
├── repository/
│   ├── AlternativeRepository.java                 ← Spring Data JPA (alternatives 테이블)
│   ├── AlternativeCardSnapshotRepository.java     ← Spring Data JPA (alternative_card_snapshots 테이블)
│   └── SelectionLogRepository.java                ← Spring Data JPA (selection_logs 테이블)
│
├── domain/
│   ├── Alternative.java                           ← @Entity: alternatives 테이블
│   │   · id(UUID), userId, originalPlaceId, placeId, name
│   │   · distanceM, rating, congestion, reason, statusLabel
│   │   · lat, lng, walkingMinutes, transitMinutes, score
│   │   · createdAt
│   ├── AlternativeCardSnapshot.java               ← @Entity: alternative_card_snapshots (ML 학습 데이터)
│   │   · userId, originalPlaceId, candidatePlaceId
│   │   · weightsJson, scoresJson
│   ├── SelectionLog.java                          ← @Entity: selection_logs
│   │   · altId, userId, selectedRank, elapsedSeconds, success
│   ├── PlaceCandidate.java                        ← 장소 후보 VO (placeServiceClient 응답)
│   ├── ScoredCandidate.java                       ← 점수가 계산된 후보 (distanceScore, ratingScore, congestionScore, totalScore)
│   ├── ScoreWeights.java                          ← 가중치 값 객체 (distanceWeight + ratingWeight + congestionWeight = 1.0)
│   ├── WeightsContext.java                        ← 가중치 결정 컨텍스트 (userId, category, timeSlot)
│   └── StatusBadge.java                           ← MNTR 서비스 상태 배지 VO (GREEN/YELLOW/RED/GREY)
│
├── scoring/
│   ├── ScoreWeightsProvider.java                  ← 가중치 제공 인터페이스
│   ├── FixedScoreWeightsProvider.java             ← Phase 1: 고정 가중치 (distance=0.5, rating=0.3, congestion=0.2)
│   │   (Phase 2: MLScoreWeightsProvider 로 교체 예정)
│   └── ScoreCalculator.java                       ← 점수 계산 (정규화 + 가중합)
│       · normalizeDistance: 최대 3000m 기준 역수 정규화
│       · normalizeRating: 5.0 기준 정규화
│       · normalizeCongestion: 낮음=1.0, 보통=0.5, 혼잡=0.0
│
├── client/
│   ├── PlaceServiceClient.java                    ← PLACE 서비스 HTTP 클라이언트 (Mock: 3개 후보 반환)
│   ├── MonitorServiceClient.java                  ← MNTR 서비스 HTTP 클라이언트 (Mock: 전체 GREEN)
│   └── ScheduleServiceClient.java                 ← SCHD 서비스 HTTP 클라이언트 (Mock: 성공, -3분)
│
└── dto/
    ├── request/
    │   ├── AlternativeSearchRequest.java           ← placeId, category, coordinates(lat/lng)
    │   ├── SelectAlternativeRequest.java           ← originalPlaceId, scheduleItemId, tripId, selectedRank, elapsedSeconds
    │   └── CoordinatesDto.java                     ← lat, lng 좌표 DTO
    ├── response/
    │   ├── AlternativeSearchResponse.java          ← 대안 검색 응답 (카드 목록, 반경)
    │   ├── SelectAlternativeResponse.java          ← 대안 선택 응답 (교체 결과, 이동시간 차이)
    │   ├── AlternativeCardDto.java                 ← 개별 대안 카드 DTO
    │   ├── TravelTimeDto.java                      ← 이동 시간 DTO (도보/대중교통/거리)
    │   ├── PlaceRefDto.java                        ← 장소 참조 DTO (before/after)
    │   └── PaywallResponse.java                    ← FREE 티어 페이월 응답 (402)
    └── internal/
        ├── AlternativeSearchResult.java            ← 서비스 내부 검색 결과 (alternatives, radiusUsed)
        ├── SelectResult.java                       ← 서비스 내부 선택 결과
        └── ReplaceResult.java                      ← SCHD 서비스 교체 결과
```

---

## API 매핑

| 메서드 | 경로 | 인증 필요 | 설명 |
|--------|------|----------|------|
| POST | /api/v1/alternatives/search | Phase 3 예정 | 대안 장소 검색 (FREE 티어 페이월) |
| POST | /api/v1/alternatives/{altId}/select | Phase 3 예정 | 대안 선택 및 일정 교체 |

---

## Redis 키 패턴

| DB | 키 패턴 | TTL | 용도 |
|----|---------|-----|------|
| DB6 | `altn:cards:{placeId}:{category}:{radius}` | 10분 | 대안 카드 캐시 (JSON 직렬화) |

---

## 점수 계산 공식

```
totalScore = distanceWeight * normalizeDistance(distanceM)
           + ratingWeight   * normalizeRating(rating)
           + congestionWeight * normalizeCongestion(congestion)

Phase 1 가중치:
  distanceWeight   = 0.5
  ratingWeight     = 0.3
  congestionWeight = 0.2
```

---

## Phase 1 / Phase 2 전환 지점

| 컴포넌트 | Phase 1 | Phase 2 |
|---------|---------|---------|
| ScoreWeightsProvider | FixedScoreWeightsProvider | MLScoreWeightsProvider (ML 모델) |
| PlaceServiceClient | Mock 3개 후보 반환 | PLACE 서비스 실제 연동 |
| MonitorServiceClient | Mock 전체 GREEN | MNTR 서비스 실제 연동 |
| ScheduleServiceClient | Mock 성공 반환 | SCHD 서비스 실제 연동 |
| SecurityConfig | 전체 permitAll | JWT Bearer 인증 적용 |

---

## 의존 관계

- common 모듈: `ApiResponse`, `UserPrincipal`, `SubscriptionTier`, `BusinessException`, `ResourceNotFoundException`, `PaywallException`, `GlobalExceptionHandler`
- 외부 서비스: PLACE 서비스 (http://localhost:8084), MNTR 서비스 (http://localhost:8083), SCHD 서비스 (http://localhost:8082)
- DB: PostgreSQL `alternative` 데이터베이스 (alternatives, alternative_card_snapshots, selection_logs)
- Cache: Redis DB6 (대안 카드 캐시, 10분 TTL)
