# MONITOR 서비스 - 패키지 구조

## 서비스 개요

| 항목 | 내용 |
|------|------|
| 서비스명 | monitor-service |
| 포트 | 8084 |
| 패키지 | com.travelplanner.monitor |
| DB | PostgreSQL (monitor) |
| Redis | DB4 (MNTR 전용) |
| 스케줄러 | 15분 주기 외부 API 수집 |

---

## 패키지 트리

```
com.travelplanner.monitor
│
├── MonitorApplication.java               # Spring Boot 진입점 (@EnableScheduling)
│
├── config/
│   ├── AppConfig.java                    # RestTemplate Bean 등록
│   ├── RedisConfig.java                  # Redis DB4 연결 설정
│   └── SecurityConfig.java              # Spring Security (Phase 1: permitAll)
│
├── controller/
│   └── MntrController.java              # API 컨트롤러 (모든 엔드포인트)
│
├── service/
│   ├── BadgeService.java                # 배지 서비스 인터페이스
│   ├── BadgeServiceImpl.java            # 배지 서비스 구현체 (Cache-Aside)
│   ├── DataCollectionService.java       # 데이터 수집 서비스 인터페이스
│   ├── DataCollectionServiceImpl.java   # 데이터 수집 서비스 구현체
│   └── StatusJudgmentService.java       # 4단계 상태 판정 서비스
│
├── repository/
│   ├── MonitoringRepository.java        # 모니터링 대상 JPA 리포지토리
│   ├── StatusHistoryRepository.java     # 상태 이력 JPA 리포지토리
│   └── CollectedDataRepository.java     # 수집 데이터 JPA 리포지토리
│
├── domain/
│   ├── MonitoringTarget.java            # 모니터링 대상 엔티티
│   ├── StatusHistory.java               # 상태 이력 엔티티 (append-only)
│   ├── CollectedData.java               # 수집 데이터 엔티티
│   ├── StatusBadge.java                 # 상태 배지 도메인
│   ├── PlaceStatusEnum.java             # 상태 열거형 [GREEN/YELLOW/RED/GREY]
│   ├── BadgeIcon.java                   # 배지 아이콘 열거형 [CHECK/EXCLAMATION/X/QUESTION]
│   ├── ItemStatus.java                  # 개별 항목 상태 열거형 [NORMAL/WARNING/DANGER]
│   ├── BusinessStatusData.java          # 영업 상태 데이터
│   ├── WeatherData.java                 # 날씨 데이터
│   ├── TravelTimeData.java              # 이동시간 데이터
│   └── CollectionJob.java               # 수집 작업 도메인
│
├── client/
│   ├── GooglePlacesClient.java          # Google Places API 클라이언트
│   ├── OpenWeatherMapClient.java        # OpenWeatherMap API 클라이언트
│   └── GoogleDirectionsClient.java      # Google Directions API 클라이언트
│
├── scheduler/
│   └── DataCollectionScheduler.java     # 15분 주기 스케줄러
│
└── dto/
    ├── request/
    │   └── CollectTriggerRequest.java    # 수집 트리거 요청
    ├── response/
    │   ├── BadgeListResponse.java        # 배지 목록 응답
    │   ├── BadgeItemDto.java             # 배지 아이템 DTO
    │   ├── StatusDetailResponse.java     # 상태 상세 응답
    │   ├── StatusDetailsDto.java         # 상태 상세 DTO (중첩 클래스 포함)
    │   └── CollectTriggerResponse.java   # 수집 트리거 응답
    └── internal/
        ├── StatusDetail.java             # 상태 상세 내부 DTO
        └── PlaceStatusChangedEvent.java  # 상태 변경 이벤트
```

---

## API 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | /api/v1/badges?place_ids=... | 장소별 상태 배지 목록 조회 (MNTR-01) |
| GET | /api/v1/badges/{placeId}/detail | 장소 상태 상세 조회 (MNTR-02) |
| POST | /api/v1/monitor/collect | 외부 데이터 수집 트리거 (내부용) |

---

## 상태 판정 로직 (4단계)

| 상태 | 색상 | 아이콘 | 조건 |
|------|------|-------|------|
| GREEN | #4CAF50 | CHECK | 모든 항목 정상 |
| YELLOW | #FFC107 | EXCLAMATION | 날씨 강수 40%+ 또는 혼잡도 50%+ |
| RED | #F44336 | X | 날씨 강수 70%+ 또는 혼잡도 80%+ 또는 영업 종료 |
| GREY | #9E9E9E | QUESTION | 3회 연속 수집 실패 |

---

## 외부 API 연동

| API | 환경변수 | Phase 1 동작 |
|-----|---------|-------------|
| OpenWeatherMap | OPENWEATHERMAP_API_KEY | 키 없으면 Mock (Clear, 10%) |
| Google Places | GOOGLE_PLACES_API_KEY | 키 없으면 Mock (OPEN) |
| Google Directions | GOOGLE_DIRECTIONS_API_KEY | 키 없으면 거리 기반 계산 |
| 혼잡도 | 없음 | Phase 1: null 반환 (NORMAL 처리) |

---

## DB 테이블

| 테이블명 | 설명 |
|---------|------|
| monitoring_targets | 모니터링 대상 장소 |
| status_history | 상태 판정 이력 (append-only) |
| collected_data | 외부 API 수집 원본 데이터 |

---

## Redis 캐시 (DB4)

| 키 패턴 | TTL | 설명 |
|--------|-----|------|
| mntr:badge:{placeId} | 5분 | 상태 배지 (Hash) |

---

## 이벤트

| 이벤트 | 발행 시점 | 수신 서비스 |
|--------|----------|-----------|
| PlaceStatusChangedEvent | 상태가 변경될 때 | BRIF (브리핑 생성 트리거) |

---

**작성일**: 2026-02-24
**작성자**: 강도윤/데브-백
