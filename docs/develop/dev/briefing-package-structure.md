# BRIF 서비스 — 패키지 구조도

## 개요

| 항목 | 값 |
|------|---|
| 서비스 ID | BRIF |
| 모듈 경로 | `briefing/` |
| 루트 패키지 | `com.travelplanner.briefing` |
| 포트 | 8085 |
| Spring Boot 진입점 | `BriefingApplication` |
| DB | PostgreSQL `briefing` 데이터베이스 |
| Redis | DB5 (멱등성 캐시, 브리핑 카운트, 목록 캐시) |

---

## 패키지 트리

```
com.travelplanner.briefing
│
├── BriefingApplication.java                       ← Spring Boot 진입점
│
├── config/
│   ├── SecurityConfig.java                        ← Phase 1: 전체 permitAll (Phase 3 JWT 적용 예정)
│   ├── RedisConfig.java                           ← Redis DB5 설정
│   └── AppConfig.java                             ← RestTemplate Bean
│
├── controller/
│   ├── BriefController.java                       ← 브리핑 조회 API
│   │   - GET /api/v1/briefings/{briefingId}
│   │   - GET /api/v1/briefings
│   └── BriefingSchedulerController.java           ← 브리핑 생성 API (SCHD 서비스 호출)
│       - POST /api/v1/briefings/generate
│
├── service/
│   ├── BriefingService.java                       ← 서비스 인터페이스
│   └── BriefingServiceImpl.java                   ← 서비스 구현체
│       · 멱등성 체크 (Redis brif:idem: + DB findByIdempotencyKey)
│       · FREE 티어 일일 한도 체크 (brif:count: + DB countByUserIdAndCreatedAtDate)
│       · MNTR 서비스 상태 조회 → 상태 수준 판정 → 텍스트 생성
│       · FCM Push 발송, 이벤트 발행
│
├── repository/
│   ├── BriefingRepository.java                    ← Spring Data JPA (briefings 테이블)
│   │   · findByIdAndUserId, findByIdempotencyKey
│   │   · findByUserIdAndDate (JPQL 날짜 캐스팅)
│   │   · countByUserIdAndCreatedAtDate
│   └── BriefingLogRepository.java                 ← Spring Data JPA (briefing_logs 테이블)
│
├── domain/
│   ├── Briefing.java                              ← @Entity: briefings 테이블
│   │   · id(UUID), userId, scheduleItemId, placeId, placeName
│   │   · type(BriefingType), departureTime, idempotencyKey
│   │   · summaryText, statusLevel
│   │   · content(@Embedded BriefingContent)
│   │   · riskItems(@JdbcTypeCode JSON)
│   │   · createdAt
│   ├── BriefingLog.java                           ← @Entity: briefing_logs 테이블
│   │   · 생성(CREATED) / 스킵(SKIPPED) 이력
│   ├── BriefingContent.java                       ← @Embeddable: 브리핑 콘텐츠 (영업/혼잡/날씨/도보/거리)
│   ├── BriefingContext.java                       ← 텍스트 생성용 컨텍스트 (MonitorData + StatusLevel + RiskItems)
│   ├── BriefingText.java                          ← 생성된 텍스트 래퍼
│   ├── RiskItem.java                              ← 위험 항목 값 객체 (label, severity)
│   └── StatusLevel.java                           ← 상태 수준 Enum: SAFE, CAUTION, DANGER
│
├── generator/
│   ├── BriefingTextGenerator.java                 ← 텍스트 생성 인터페이스
│   └── RuleBasedBriefingGenerator.java            ← Phase 1: 규칙 기반 구현
│       · SAFE: "현재까지 모든 항목 정상입니다. 예정대로 출발하세요."
│       · WARNING: "{위험항목}이(가) 감지되었습니다. 대안을 확인해보세요."
│       (Phase 2: LLMBriefingGenerator 로 교체 예정)
│
├── client/
│   ├── MonitorServiceClient.java                  ← MNTR 서비스 HTTP 클라이언트 (Mock 폴백)
│   ├── PayServiceClient.java                      ← PAY 서비스 HTTP 클라이언트 (Mock 폴백)
│   ├── FcmClient.java                             ← Phase 1: 로그 전용 (Phase 2: Firebase Admin SDK)
│   └── EventPublisher.java                        ← Phase 1: 로그 전용 (Phase 2: 메시지 브로커)
│
└── dto/
    ├── request/
    │   └── GenerateBriefingRequest.java            ← scheduleItemId, placeId, userId, departureTime, triggeredAt
    ├── response/
    │   ├── BriefingDetailResponse.java             ← 브리핑 상세 응답
    │   ├── BriefingListResponse.java               ← 브리핑 목록 응답
    │   ├── BriefingListItemDto.java                ← 목록 아이템 DTO
    │   ├── BriefingContentDto.java                 ← 브리핑 콘텐츠 DTO
    │   └── GenerateBriefingResponse.java           ← 브리핑 생성 응답
    └── internal/
        ├── GenerateBriefingResult.java             ← 서비스 내부 결과 (created/existing/skipped)
        ├── BriefingCreatedEvent.java               ← 이벤트 발행용 VO
        ├── MonitorData.java                        ← MNTR 서비스 응답 내부 DTO
        └── SubscriptionInfo.java                   ← PAY 서비스 응답 내부 DTO
```

---

## API 매핑

| 메서드 | 경로 | 인증 필요 | 설명 |
|--------|------|----------|------|
| GET | /api/v1/briefings/{briefingId} | Phase 3 예정 | 브리핑 상세 조회 |
| GET | /api/v1/briefings | Phase 3 예정 | 날짜별 브리핑 목록 조회 |
| POST | /api/v1/briefings/generate | Phase 3 예정 | 브리핑 생성 (SCHD 스케줄러 호출) |

---

## Redis 키 패턴

| DB | 키 패턴 | TTL | 용도 |
|----|---------|-----|------|
| DB5 | `brif:idem:{placeId}:{yyyyMMddHH}` | 2시간 | 멱등성 체크 (briefingId 저장) |
| DB5 | `brif:count:{userId}:{date}` | - | FREE 티어 일일 브리핑 카운트 |
| DB5 | `brif:list:{userId}:{date}` | - | 브리핑 목록 캐시 키 (무효화용) |
| DB5 | `brif:briefing:{briefingId}` | - | 브리핑 개별 캐시 (향후 확장) |

---

## Phase 1 / Phase 2 전환 지점

| 컴포넌트 | Phase 1 | Phase 2 |
|---------|---------|---------|
| BriefingTextGenerator | RuleBasedBriefingGenerator | LLMBriefingGenerator (LLM 연동) |
| FcmClient | 로그 전용 | Firebase Admin SDK 실제 발송 |
| EventPublisher | 로그 전용 | Kafka/SQS 메시지 발행 |
| SecurityConfig | 전체 permitAll | JWT Bearer 인증 적용 |

---

## 의존 관계

- common 모듈: `ApiResponse`, `UserPrincipal`, `SubscriptionTier`, `BriefingType`, `BusinessException`, `ResourceNotFoundException`, `GlobalExceptionHandler`
- 외부 서비스: MNTR 서비스 (http://localhost:8083), PAY 서비스 (http://localhost:8087)
- DB: PostgreSQL `briefing` 데이터베이스 (briefings, briefing_logs)
- Cache: Redis DB5 (멱등성, 카운트, 목록 캐시)
