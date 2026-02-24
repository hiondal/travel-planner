# SCHEDULE 서비스 - 패키지 구조

## 서비스 개요

| 항목 | 내용 |
|------|------|
| 서비스명 | schedule-service |
| 포트 | 8082 |
| 패키지 | com.travelplanner.schedule |
| DB | PostgreSQL (schedule) |
| Redis | DB2 (SCHD 전용) |
| 이벤트 | Spring ApplicationEventPublisher (In-Memory) |

---

## 패키지 트리

```
com.travelplanner.schedule
│
├── ScheduleApplication.java              # Spring Boot 진입점
│
├── config/
│   ├── AppConfig.java                    # RestTemplate Bean 등록
│   ├── RedisConfig.java                  # Redis DB2 연결 설정
│   └── SecurityConfig.java              # Spring Security (Phase 1: permitAll)
│
├── controller/
│   └── SchdController.java              # API 컨트롤러 (모든 엔드포인트)
│
├── service/
│   ├── TripService.java                 # 여행 서비스 인터페이스
│   ├── TripServiceImpl.java             # 여행 서비스 구현체
│   ├── ScheduleItemService.java         # 일정 아이템 서비스 인터페이스
│   └── ScheduleItemServiceImpl.java     # 일정 아이템 서비스 구현체
│
├── repository/
│   ├── TripRepository.java              # 여행 JPA 리포지토리
│   └── ScheduleItemRepository.java      # 일정 아이템 JPA 리포지토리
│
├── domain/
│   ├── Trip.java                        # 여행 엔티티
│   └── ScheduleItem.java                # 일정 아이템 엔티티
│
├── client/
│   └── PlaceServiceClient.java          # PLCE 서비스 RestTemplate 클라이언트
│       ├── PlaceDetail (inner class)    # 장소 상세 DTO
│       └── BusinessHour (inner class)  # 영업시간 DTO
│
├── event/
│   ├── ScheduleItemAddedEvent.java      # 장소 추가 이벤트
│   ├── ScheduleItemDeletedEvent.java    # 장소 삭제 이벤트
│   └── ScheduleItemReplacedEvent.java   # 장소 교체 이벤트
│
└── dto/
    ├── request/
    │   ├── CreateTripRequest.java        # 여행 생성 요청
    │   ├── AddScheduleItemRequest.java   # 장소 추가 요청
    │   └── ReplaceScheduleItemRequest.java # 장소 교체 요청
    ├── response/
    │   ├── TripResponse.java            # 여행 응답
    │   ├── TripListResponse.java        # 여행 목록 응답
    │   ├── ScheduleResponse.java        # 일정표 응답
    │   ├── ScheduleItemResponse.java    # 일정 아이템 응답
    │   ├── ScheduleItemSummary.java     # 일정 아이템 요약
    │   ├── ReplaceScheduleItemResponse.java # 교체 응답
    │   ├── BusinessHoursWarningResponse.java # 영업시간 외 경고 응답
    │   └── PlaceRef.java                # 장소 참조 DTO
    └── internal/
        ├── ScheduleItemAddResult.java   # 장소 추가 내부 결과
        ├── ReplaceResult.java           # 교체 내부 결과
        └── ScheduleResult.java          # 일정표 조회 내부 결과
```

---

## API 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | /api/v1/trips | 여행 목록 조회 (SCHD-00) |
| POST | /api/v1/trips | 여행 생성 (SCHD-01) |
| GET | /api/v1/trips/{tripId} | 여행 조회 (SCHD-02) |
| GET | /api/v1/trips/{tripId}/schedule | 일정표 조회 (SCHD-03) |
| POST | /api/v1/trips/{tripId}/schedule-items | 장소 추가 (SCHD-04) |
| DELETE | /api/v1/trips/{tripId}/schedule-items/{itemId} | 장소 삭제 (SCHD-05) |
| PUT | /api/v1/trips/{tripId}/schedule-items/{itemId}/replace | 장소 교체 (SCHD-06) |

---

## 이벤트 발행

| 이벤트 | 발행 시점 | 수신 서비스 |
|--------|----------|-----------|
| ScheduleItemAddedEvent | 장소 추가 완료 | MNTR (모니터링 등록) |
| ScheduleItemDeletedEvent | 장소 삭제 완료 | MNTR (모니터링 해제) |
| ScheduleItemReplacedEvent | 장소 교체 완료 | MNTR (모니터링 갱신) |

Phase 1: Spring ApplicationEventPublisher (In-Memory)
Phase 2+: Kafka/RabbitMQ 기반 메시지 큐로 전환 예정

---

## DB 테이블

| 테이블명 | 설명 |
|---------|------|
| trips | 여행 헤더 정보 |
| schedule_items | 일정 아이템 (장소 방문 계획) |

---

**작성일**: 2026-02-24
**작성자**: 강도윤/데브-백
