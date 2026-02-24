# PLACE 서비스 — 패키지 구조도

## 개요

| 항목 | 값 |
|------|---|
| 서비스 ID | PLACE (PLCE) |
| 모듈 경로 | `place/` |
| 루트 패키지 | `com.travelplanner.place` |
| 포트 | 8083 |
| Spring Boot 진입점 | `PlaceApplication` |
| DB | PostgreSQL `place` 데이터베이스 |
| Redis | DB3 (장소 상세·검색·주변 장소 캐시) |
| 외부 API | Google Places API (Text Search, Nearby Search, Place Details) |

---

## 패키지 트리

```
com.travelplanner.place
│
├── PlaceApplication.java                            ← Spring Boot 진입점
│
├── config/
│   ├── AppConfig.java                               ← ObjectMapper, WebClient.Builder 빈 설정
│   ├── SecurityConfig.java                          ← Spring Security (permitAll, Phase 1)
│   ├── SwaggerConfig.java                           ← SpringDoc OpenAPI 설정
│   ├── RedisConfig.java                             ← Redis DB3 연결 및 RedisTemplate 설정
│   └── PlaceExceptionHandler.java                   ← MissingServletRequestParameterException 등 추가 예외 처리
│
├── controller/
│   └── PlceController.java                          ← REST API 엔드포인트 (3개)
│       - GET /places/search          (PLCE-01)
│       - GET /places/{place_id}      (PLCE-02)
│       - GET /places/nearby          (PLCE-03)
│
├── service/
│   ├── PlaceService.java                            ← 서비스 인터페이스
│   └── PlaceServiceImpl.java                        ← 서비스 구현체 (캐시 계층: Redis → DB → Google API)
│
├── repository/
│   ├── PlaceJpaRepository.java                      ← Spring Data JPA (쿼리 정의)
│   └── PlaceRepository.java                         ← 리포지토리 구현체 (거리 계산 로직 포함)
│
├── client/
│   ├── GooglePlacesClient.java                      ← Google Places API WebClient 호출
│   └── GooglePlaceDto.java                          ← Google API 응답 VO (toPlace() 변환 포함)
│
├── domain/
│   ├── Place.java                                   ← @Entity: places 테이블 (Google Place ID를 PK로 사용)
│   ├── BusinessHour.java                            ← @Entity: place_business_hours 테이블
│   └── Coordinates.java                             ← 위경도 값 객체 (Haversine 거리 계산)
│
└── dto/
    ├── response/
    │   ├── PlaceSearchResponse.java                 ← GET /places/search 응답
    │   ├── PlaceDetailResponse.java                 ← GET /places/{place_id} 응답
    │   ├── NearbyPlaceSearchResponse.java           ← GET /places/nearby 응답
    │   ├── PlaceSummary.java                        ← 검색 결과 장소 요약 DTO
    │   ├── NearbyPlaceDto.java                      ← 주변 장소 DTO (거리, 영업 여부 포함)
    │   ├── BusinessHourDto.java                     ← 영업시간 DTO
    │   └── CoordinatesDto.java                      ← 위경도 좌표 DTO
    └── internal/
        ├── NearbySearchResult.java                  ← 주변 검색 결과 내부 모델
        └── NearbyPlace.java                         ← 거리/영업 여부 포함 장소 내부 모델
```

---

## API 매핑

| 메서드 | 경로 | 인증 필요 | 설명 |
|--------|------|----------|------|
| GET | /places/search?keyword=&city= | 없음 (Phase 1) | 키워드 기반 장소 검색 (최대 10개) |
| GET | /places/{place_id} | 없음 (Phase 1) | 장소 상세 조회 (영업시간 포함) |
| GET | /places/nearby?lat=&lng=&category=&radius= | 없음 (Phase 1) | 주변 장소 검색 (1km/2km/3km 반경) |

---

## Redis 캐시 키 패턴

| DB | 키 패턴 | TTL | 용도 |
|----|---------|-----|------|
| DB3 | `plce:detail:{placeId}` | 1시간 | 장소 상세 정보 (영업시간 포함) |
| DB3 | `plce:search:{keyword}:{city}` | 30분 | 텍스트 검색 결과 목록 |
| DB3 | `plce:nearby:{latLng}:{category}:{radius}` | 15분 | 주변 장소 검색 결과 |

### 키 생성 규칙
- `{latLng}`: 위경도 소수점 3자리 반올림 (예: `35.681_139.767`)
- `{keyword}:{city}`: 소문자 정규화 후 URL 인코딩

---

## 데이터 흐름 (캐시 계층)

```
장소 검색 (searchPlaces):
  Redis DB3 캐시 확인 → DB miss → PostgreSQL 조회 → DB miss → Google Text Search API
  → places + place_business_hours upsert → Redis 캐시 등록 (30분 TTL)

장소 상세 조회 (getPlaceDetail):
  Redis DB3 캐시 확인 → DB miss → PostgreSQL 조회 (신선도 체크) → stale/miss → Google Place Details API
  → DB upsert → Redis 캐시 등록 (1시간 TTL)

주변 장소 검색 (searchNearbyPlaces):
  Redis DB3 캐시 확인 → DB miss → PostgreSQL 좌표 범위 쿼리 → DB miss → Google Nearby Search API
  → DB upsert → 거리 계산 + 영업 여부 판정 → Redis 캐시 등록 (15분 TTL)
```

---

## DB 테이블 매핑

| 엔티티 | 테이블 | 비고 |
|--------|--------|------|
| Place | places | Google Place ID를 PK로 사용 (VARCHAR 200) |
| BusinessHour | place_business_hours | places와 1:N, ON DELETE CASCADE |

---

## 의존 관계

- common 모듈: `ApiResponse`, `ErrorResponse`, `BusinessException`, `ResourceNotFoundException`, `ExternalApiException`, `ValidationException`, `GlobalExceptionHandler`, `BaseTimeEntity` (미사용, 독립 엔티티)
- 외부: Google Places API (Text Search, Nearby Search, Place Details)
- DB: PostgreSQL `place` 데이터베이스 (places, place_business_hours)
- Cache: Redis DB3 (장소 캐시)

---

## 테스트 구조

```
src/test/java/com/travelplanner/place/
├── controller/
│   └── PlceControllerTest.java          ← @WebMvcTest 기반 컨트롤러 단위 테스트 (11개)
└── service/
    └── PlaceServiceImplTest.java         ← @ExtendWith(MockitoExtension) 기반 서비스 단위 테스트 (10개)
```

### 테스트 케이스 목록

**PlceControllerTest (11개)**
- 장소 검색 정상 요청 200 반환
- keyword 파라미터 없을 때 400 반환
- keyword 1자 이하일 때 400 반환
- city 파라미터 없을 때 400 반환
- 빈 결과 200 반환
- 장소 상세 조회 정상 요청 200 반환
- 존재하지 않는 장소 404 반환
- 주변 장소 검색 정상 요청 200 반환
- 유효하지 않은 반경 400 반환
- lat 파라미터 없을 때 400 반환
- 2km 반경으로 검색 200 반환

**PlaceServiceImplTest (10개)**
- 장소 검색 Redis 캐시 히트 시 DB/API 호출 없음
- 장소 검색 캐시 미스, DB 히트 시 DB에서 반환
- 장소 검색 캐시/DB 모두 미스 시 Google API 호출
- 장소 검색 결과 최대 10개 제한
- 장소 상세 조회 Redis 캐시 히트
- 장소 상세 조회 캐시 미스, DB 히트 (최신 데이터)
- 장소 상세 조회 존재하지 않는 장소 ID 시 ResourceNotFoundException
- 주변 장소 검색 Redis 캐시 히트
- 주변 장소 검색 캐시 미스, DB 히트
- 주변 장소 검색 거리 필터링 검증

---

**작성일**: 2026-02-24
**작성자**: 강도윤/데브-백
