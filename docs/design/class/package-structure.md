# Travel Planner — 전체 패키지 구조도

## 개요

- 아키텍처 패턴: Layered Architecture (모든 서비스 공통)
- 레이어 순서: Controller → Service → Repository → Domain
- 외부 서비스 호출: Client/Gateway 클래스로 분리
- 마이크로서비스 간 크로스 참조 금지 (서비스 클라이언트 경유)

---

## 전체 패키지 트리

```
com.travelplanner
│
├── common/                          # 공통 컴포넌트 (common-base.puml)
│   ├── exception/
│   │   ├── BusinessException
│   │   ├── ValidationException
│   │   ├── ConsentRequiredException
│   │   ├── ResourceNotFoundException
│   │   ├── PaywallException
│   │   └── ExternalApiException
│   ├── response/
│   │   ├── ApiResponse<T>
│   │   ├── ErrorResponse
│   │   └── PageResponse<T>
│   ├── domain/
│   │   ├── BaseEntity  (abstract)
│   │   └── BaseTimeEntity  (abstract)
│   ├── security/
│   │   ├── JwtToken
│   │   ├── UserPrincipal
│   │   └── JwtProvider
│   └── enums/
│       ├── SubscriptionTier       [FREE, TRIP_PASS, PRO]
│       ├── OAuthProvider          [GOOGLE, APPLE]
│       ├── PlaceStatus            [GREEN, YELLOW, RED, GREY]
│       ├── TripStatus             [ACTIVE, COMPLETED, CANCELLED]
│       └── BriefingType           [SAFE, WARNING]
│
├── auth/                            # AUTH 서비스 (auth.puml / auth-simple.puml)
│   ├── controller/
│   │   └── AuthController
│   ├── service/
│   │   ├── AuthService  (interface)
│   │   └── AuthServiceImpl
│   ├── repository/
│   │   ├── UserRepository
│   │   ├── ConsentRepository
│   │   └── AuthSessionRepository
│   ├── client/
│   │   ├── OAuthClient
│   │   └── OAuthProfile
│   ├── domain/
│   │   ├── User
│   │   ├── Consent
│   │   └── AuthSession
│   └── dto/
│       ├── request/
│       │   ├── SocialLoginRequest
│       │   ├── TokenRefreshRequest
│       │   ├── LogoutRequest
│       │   ├── TokenInvalidateRequest
│       │   └── ConsentRequest
│       ├── response/
│       │   ├── SocialLoginResponse
│       │   ├── TokenRefreshResponse
│       │   ├── TokenInvalidateResponse
│       │   ├── ConsentResponse
│       │   └── UserProfileDto
│       └── internal/
│           ├── SocialLoginResult
│           ├── TokenRefreshResult
│           └── TokenInvalidateResult
│
├── schedule/                        # SCHEDULE 서비스 (schedule.puml / schedule-simple.puml)
│   ├── controller/
│   │   └── SchdController
│   ├── service/
│   │   ├── TripService  (interface)
│   │   ├── TripServiceImpl
│   │   ├── ScheduleItemService  (interface)
│   │   └── ScheduleItemServiceImpl
│   ├── repository/
│   │   ├── TripRepository
│   │   ├── ScheduleItemRepository
│   │   └── ConsentRepository
│   ├── client/
│   │   ├── PlaceServiceClient
│   │   ├── EventPublisher
│   │   ├── PlaceDetail
│   │   └── BusinessHour
│   ├── domain/
│   │   ├── Trip
│   │   └── ScheduleItem
│   └── dto/
│       ├── request/
│       │   ├── CreateTripRequest
│       │   ├── AddScheduleItemRequest
│       │   └── ReplaceScheduleItemRequest
│       ├── response/
│       │   ├── TripResponse
│       │   ├── ScheduleResponse
│       │   ├── ScheduleItemResponse
│       │   ├── ScheduleItemSummary
│       │   ├── ReplaceScheduleItemResponse
│       │   ├── BusinessHoursWarningResponse
│       │   └── PlaceRef
│       └── internal/
│           ├── ScheduleItemAddResult
│           ├── ReplaceResult
│           ├── ScheduleResult
│           ├── ScheduleItemAddedEvent
│           ├── ScheduleItemDeletedEvent
│           └── ScheduleItemReplacedEvent
│
├── place/                           # PLACE 서비스 (place.puml / place-simple.puml)
│   ├── controller/
│   │   └── PlceController
│   ├── service/
│   │   ├── PlaceService  (interface)
│   │   └── PlaceServiceImpl
│   ├── repository/
│   │   └── PlaceRepository
│   ├── client/
│   │   ├── GooglePlacesClient
│   │   └── GooglePlaceDto
│   ├── domain/
│   │   ├── Place
│   │   ├── BusinessHour
│   │   └── Coordinates
│   └── dto/
│       ├── response/
│       │   ├── PlaceSearchResponse
│       │   ├── PlaceDetailResponse
│       │   ├── NearbyPlaceSearchResponse
│       │   ├── PlaceSummary
│       │   ├── NearbyPlaceDto
│       │   ├── BusinessHourDto
│       │   └── CoordinatesDto
│       └── internal/
│           ├── NearbySearchResult
│           └── NearbyPlace
│
├── monitor/                         # MONITOR 서비스 (monitor.puml / monitor-simple.puml)
│   ├── controller/
│   │   └── MntrController
│   ├── service/
│   │   ├── BadgeService  (interface)
│   │   ├── BadgeServiceImpl
│   │   ├── DataCollectionService  (interface)
│   │   ├── DataCollectionServiceImpl
│   │   └── StatusJudgmentService
│   ├── repository/
│   │   ├── MonitoringRepository
│   │   └── StatusHistoryRepository
│   ├── client/
│   │   ├── GooglePlacesClient
│   │   ├── OpenWeatherMapClient
│   │   ├── GoogleDirectionsClient
│   │   ├── AzureAppConfigClient
│   │   └── EventPublisher
│   ├── domain/
│   │   ├── MonitoringTarget
│   │   ├── StatusBadge
│   │   ├── CollectedData
│   │   ├── StatusHistory
│   │   ├── CurrentStatus
│   │   ├── BusinessStatusData
│   │   ├── WeatherData
│   │   ├── TravelTimeData
│   │   ├── CollectionJob
│   │   ├── PlaceStatusEnum  [GREEN, YELLOW, RED, GREY]
│   │   ├── BadgeIcon        [CHECK, EXCLAMATION, X, QUESTION]
│   │   └── ItemStatus       [NORMAL, WARNING, DANGER]
│   └── dto/
│       ├── request/
│       │   └── CollectTriggerRequest
│       ├── response/
│       │   ├── BadgeListResponse
│       │   ├── BadgeItemDto
│       │   ├── StatusDetailResponse
│       │   ├── StatusDetailsDto
│       │   └── CollectTriggerResponse
│       └── internal/
│           ├── StatusDetail
│           ├── CollectionConfig
│           ├── PlaceStatusChangedEvent
│           └── WeatherForecast
│
├── briefing/                        # BRIEFING 서비스 (briefing.puml / briefing-simple.puml)
│   ├── controller/
│   │   ├── BriefController
│   │   └── BriefingScheduler
│   ├── service/
│   │   ├── BriefingService  (interface)
│   │   └── BriefingServiceImpl
│   ├── generator/
│   │   ├── BriefingTextGenerator  (interface)
│   │   └── RuleBasedBriefingGenerator
│   ├── repository/
│   │   └── BriefingRepository
│   ├── client/
│   │   ├── MonitorServiceClient
│   │   ├── PayServiceClient
│   │   ├── FcmClient
│   │   └── EventPublisher
│   ├── domain/
│   │   ├── Briefing
│   │   ├── BriefingContent
│   │   ├── BriefingContext
│   │   ├── BriefingText
│   │   ├── RiskItem
│   │   ├── BriefingLog
│   │   └── StatusLevel  [SAFE, CAUTION, DANGER]
│   └── dto/
│       ├── request/
│       │   └── GenerateBriefingRequest
│       ├── response/
│       │   ├── BriefingDetailResponse
│       │   ├── BriefingListResponse
│       │   ├── BriefingListItemDto
│       │   ├── BriefingContentDto
│       │   └── GenerateBriefingResponse
│       └── internal/
│           ├── GenerateBriefingResult
│           ├── MonitorData
│           ├── SubscriptionInfo
│           └── BriefingCreatedEvent
│
├── alternative/                     # ALTERNATIVE 서비스 (alternative.puml / alternative-simple.puml)
│   ├── controller/
│   │   └── AltnController
│   ├── service/
│   │   ├── AlternativeService  (interface)
│   │   └── AlternativeServiceImpl
│   ├── scoring/
│   │   ├── ScoreWeightsProvider  (interface)
│   │   ├── FixedScoreWeightsProvider
│   │   └── ScoreCalculator
│   ├── repository/
│   │   └── AlternativeRepository
│   ├── client/
│   │   ├── PlaceServiceClient
│   │   ├── MonitorServiceClient
│   │   └── ScheduleServiceClient
│   ├── domain/
│   │   ├── Alternative
│   │   ├── ScoreWeights
│   │   ├── WeightsContext
│   │   ├── PlaceCandidate
│   │   ├── ScoredCandidate
│   │   ├── StatusBadge
│   │   ├── AlternativeCardSnapshot
│   │   └── SelectionLog
│   └── dto/
│       ├── request/
│       │   ├── AlternativeSearchRequest
│       │   ├── SelectAlternativeRequest
│       │   └── CoordinatesDto
│       ├── response/
│       │   ├── AlternativeSearchResponse
│       │   ├── AlternativeCardDto
│       │   ├── SelectAlternativeResponse
│       │   ├── PaywallResponse
│       │   ├── PlaceRefDto
│       │   └── TravelTimeDto
│       └── internal/
│           ├── AlternativeSearchResult
│           ├── SelectResult
│           └── ReplaceResult
│
└── payment/                         # PAYMENT 서비스 (payment.puml / payment-simple.puml)
    ├── controller/
    │   └── PayController
    ├── service/
    │   ├── SubscriptionService  (interface)
    │   └── SubscriptionServiceImpl
    ├── repository/
    │   └── SubscriptionRepository
    ├── client/
    │   ├── IapVerificationClient
    │   └── AuthServiceClient
    ├── domain/
    │   ├── Subscription
    │   ├── PaymentRecord
    │   ├── SubscriptionPlan
    │   ├── VerificationResult
    │   └── SubscriptionStatusEnum  [ACTIVE, CANCELLED, CANCELLING]
    └── dto/
        ├── request/
        │   └── PurchaseRequest
        ├── response/
        │   ├── SubscriptionPlansResponse
        │   ├── SubscriptionPlanDto
        │   ├── PurchaseResponse
        │   ├── SubscriptionStatusResponse
        │   └── PriceDto
        └── internal/
            ├── PurchaseResult
            └── SubscriptionStatus
```

---

## 서비스 간 의존성 (Client 경유)

| 호출 서비스 | 피호출 서비스 | Client 클래스 | 호출 목적 |
|------------|-------------|--------------|---------|
| schedule | place | PlaceServiceClient | 장소 상세/영업시간 조회 |
| briefing | monitor | MonitorServiceClient | 최신 상태 데이터 조회 |
| briefing | payment | PayServiceClient | 구독 티어 및 브리핑 횟수 조회 |
| alternative | place | PlaceServiceClient | 주변 장소 검색 |
| alternative | monitor | MonitorServiceClient | 배지 상태 일괄 조회 |
| alternative | schedule | ScheduleServiceClient | 일정 장소 교체 |
| payment | auth | AuthServiceClient | 토큰 즉시 무효화 및 재발급 |

---

## 설계 파일 목록

| 파일명 | 설명 |
|--------|------|
| `common-base.puml` | 공통 컴포넌트 (예외, 응답 래퍼, 도메인 베이스, JWT, 공통 Enum) |
| `auth.puml` | AUTH 서비스 상세 클래스 다이어그램 |
| `auth-simple.puml` | AUTH 서비스 요약 클래스 다이어그램 (API 매핑 포함) |
| `schedule.puml` | SCHEDULE 서비스 상세 클래스 다이어그램 |
| `schedule-simple.puml` | SCHEDULE 서비스 요약 클래스 다이어그램 (API 매핑 포함) |
| `place.puml` | PLACE 서비스 상세 클래스 다이어그램 |
| `place-simple.puml` | PLACE 서비스 요약 클래스 다이어그램 (API 매핑 포함) |
| `monitor.puml` | MONITOR 서비스 상세 클래스 다이어그램 |
| `monitor-simple.puml` | MONITOR 서비스 요약 클래스 다이어그램 (API 매핑 포함) |
| `briefing.puml` | BRIEFING 서비스 상세 클래스 다이어그램 |
| `briefing-simple.puml` | BRIEFING 서비스 요약 클래스 다이어그램 (API 매핑 포함) |
| `alternative.puml` | ALTERNATIVE 서비스 상세 클래스 다이어그램 |
| `alternative-simple.puml` | ALTERNATIVE 서비스 요약 클래스 다이어그램 (API 매핑 포함) |
| `payment.puml` | PAYMENT 서비스 상세 클래스 다이어그램 |
| `payment-simple.puml` | PAYMENT 서비스 요약 클래스 다이어그램 (API 매핑 포함) |
| `package-structure.md` | 전체 패키지 구조도 (이 파일) |
