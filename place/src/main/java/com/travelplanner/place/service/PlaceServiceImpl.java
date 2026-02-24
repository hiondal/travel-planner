package com.travelplanner.place.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelplanner.common.exception.ExternalApiException;
import com.travelplanner.common.exception.ResourceNotFoundException;
import com.travelplanner.place.client.GooglePlaceDto;
import com.travelplanner.place.client.GooglePlacesClient;
import com.travelplanner.place.domain.BusinessHour;
import com.travelplanner.place.domain.Coordinates;
import com.travelplanner.place.domain.Place;
import com.travelplanner.place.dto.internal.NearbyPlace;
import com.travelplanner.place.dto.internal.NearbySearchResult;
import com.travelplanner.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 장소 서비스 구현체.
 *
 * <p>캐시 계층: Redis DB3 → PostgreSQL → Google Places API 순서로 데이터를 조회한다.
 * 검색 결과는 Redis에 write-through 방식으로 캐싱한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceServiceImpl implements PlaceService {

    private static final Duration DETAIL_CACHE_TTL = Duration.ofHours(1);
    private static final Duration SEARCH_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration NEARBY_CACHE_TTL = Duration.ofMinutes(15);
    private static final int MAX_SEARCH_RESULTS = 10;

    private final PlaceRepository placeRepository;
    private final GooglePlacesClient googlePlacesClient;
    private final RedisTemplate<String, String> placeRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 키워드와 도시 기반으로 장소를 검색한다.
     *
     * <p>조회 순서: Redis 캐시 → PostgreSQL → Google Places API</p>
     *
     * @param keyword 검색 키워드
     * @param city    도시명
     * @return 장소 목록 (최대 10개)
     */
    @Override
    @Transactional
    public List<Place> searchPlaces(String keyword, String city) {
        String cacheKey = buildSearchCacheKey(keyword, city);

        // 1. Redis 캐시 조회
        String cached = placeRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Place search cache hit: key={}", cacheKey);
            return deserializePlaceList(cached);
        }

        // 2. PostgreSQL 조회
        List<Place> dbResults = placeRepository.findByCityAndKeyword(city, keyword);
        if (!dbResults.isEmpty()) {
            log.debug("Place search DB hit: count={}", dbResults.size());
            cachePlaceList(cacheKey, dbResults, SEARCH_CACHE_TTL);
            return dbResults.stream().limit(MAX_SEARCH_RESULTS).collect(Collectors.toList());
        }

        // 3. Google Places API 호출
        log.info("Place search Google API call: keyword={}, city={}", keyword, city);
        List<GooglePlaceDto> googleResults = googlePlacesClient.textSearch(keyword, city);
        List<Place> places = googleResults.stream()
                .map(dto -> dto.toPlace(city))
                .limit(MAX_SEARCH_RESULTS)
                .collect(Collectors.toList());

        // 4. DB upsert
        if (!places.isEmpty()) {
            upsertPlaces(places);
        }

        // 5. Redis 캐시 등록
        cachePlaceList(cacheKey, places, SEARCH_CACHE_TTL);

        return places;
    }

    /**
     * 장소 상세 정보를 조회한다.
     *
     * <p>조회 순서: Redis 캐시 → PostgreSQL → Google Places API</p>
     *
     * @param placeId Google Place ID
     * @return 장소 상세 정보
     */
    @Override
    @Transactional
    public Place getPlaceDetail(String placeId) {
        String cacheKey = buildDetailCacheKey(placeId);

        // 1. Redis 캐시 조회
        String cached = placeRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Place detail cache hit: placeId={}", placeId);
            return deserializePlace(cached);
        }

        // 2. PostgreSQL 조회
        Optional<Place> dbPlace = placeRepository.findById(placeId);
        if (dbPlace.isPresent() && !dbPlace.get().isStale()) {
            log.debug("Place detail DB hit: placeId={}", placeId);
            cachePlace(cacheKey, dbPlace.get(), DETAIL_CACHE_TTL);
            return dbPlace.get();
        }

        // 3. Google Places API 호출
        log.info("Place detail Google API call: placeId={}", placeId);
        GooglePlaceDto googleDto = googlePlacesClient.placeDetail(placeId);

        if (googleDto.getPlaceId() == null || googleDto.getPlaceId().isEmpty()) {
            throw new ResourceNotFoundException("Place", placeId);
        }

        String city = dbPlace.map(Place::getCity).orElse("");
        Place place = googleDto.toPlace(city);

        // 4. DB upsert
        place = placeRepository.save(place);

        // 5. Redis 캐시 등록
        cachePlace(cacheKey, place, DETAIL_CACHE_TTL);

        return place;
    }

    /**
     * 좌표 기반 주변 장소를 검색한다.
     *
     * <p>조회 순서: Redis 캐시 → PostgreSQL → Google Places API</p>
     *
     * @param lat      기준 위도
     * @param lng      기준 경도
     * @param category 카테고리
     * @param radius   검색 반경 (미터)
     * @return 주변 장소 검색 결과
     */
    @Override
    @Transactional
    public NearbySearchResult searchNearbyPlaces(double lat, double lng, String category, int radius) {
        String cacheKey = buildNearbyCacheKey(lat, lng, category, radius);

        // 1. Redis 캐시 조회
        String cached = placeRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Nearby search cache hit: key={}", cacheKey);
            return deserializeNearbyResult(cached);
        }

        Coordinates baseCoords = new Coordinates(lat, lng);
        LocalDateTime now = LocalDateTime.now();

        // 2. PostgreSQL 조회
        List<Place> dbPlaces = placeRepository.findNearby(lat, lng, radius, category);
        if (!dbPlaces.isEmpty()) {
            log.debug("Nearby search DB hit: count={}", dbPlaces.size());
            NearbySearchResult result = buildNearbyResult(dbPlaces, baseCoords, now, radius);
            cacheNearbyResult(cacheKey, result, NEARBY_CACHE_TTL);
            return result;
        }

        // 3. Google Places API 호출
        log.info("Nearby search Google API call: lat={}, lng={}, category={}, radius={}", lat, lng, category, radius);
        List<GooglePlaceDto> googleResults = googlePlacesClient.nearbySearch(lat, lng, radius, category);
        List<Place> places = googleResults.stream()
                .map(dto -> dto.toPlace(""))
                .collect(Collectors.toList());

        // 4. DB upsert
        if (!places.isEmpty()) {
            places = upsertPlaces(places);
        }

        // 5. 거리 계산 후 결과 생성
        NearbySearchResult result = buildNearbyResult(places, baseCoords, now, radius);

        // 6. Redis 캐시 등록
        cacheNearbyResult(cacheKey, result, NEARBY_CACHE_TTL);

        return result;
    }

    // ===== Private Helper Methods =====

    private NearbySearchResult buildNearbyResult(List<Place> places, Coordinates baseCoords,
                                                  LocalDateTime now, int radius) {
        List<NearbyPlace> nearbyPlaces = places.stream()
                .map(place -> {
                    int distance = (int) baseCoords.distanceTo(place.getCoordinates());
                    boolean isOpen = place.isOpenAt(now);
                    return new NearbyPlace(place, distance, isOpen);
                })
                .filter(np -> np.getDistanceM() <= radius)
                .sorted(Comparator.comparingInt(NearbyPlace::getDistanceM))
                .collect(Collectors.toList());

        return new NearbySearchResult(nearbyPlaces, radius);
    }

    private List<Place> upsertPlaces(List<Place> places) {
        return placeRepository.saveAll(places);
    }

    private String buildDetailCacheKey(String placeId) {
        return "plce:detail:" + placeId;
    }

    private String buildSearchCacheKey(String keyword, String city) {
        String encodedKeyword = URLEncoder.encode(keyword.toLowerCase(), StandardCharsets.UTF_8);
        String encodedCity = URLEncoder.encode(city.toLowerCase(), StandardCharsets.UTF_8);
        return "plce:search:" + encodedKeyword + ":" + encodedCity;
    }

    private String buildNearbyCacheKey(double lat, double lng, String category, int radius) {
        String latLng = String.format("%.3f_%.3f", lat, lng);
        String encodedCategory = URLEncoder.encode(category.toLowerCase(), StandardCharsets.UTF_8);
        return "plce:nearby:" + latLng + ":" + encodedCategory + ":" + radius;
    }

    private void cachePlace(String key, Place place, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(place);
            placeRedisTemplate.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache place: key={}, error={}", key, e.getMessage());
        }
    }

    private void cachePlaceList(String key, List<Place> places, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(places);
            placeRedisTemplate.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache place list: key={}, error={}", key, e.getMessage());
        }
    }

    private void cacheNearbyResult(String key, NearbySearchResult result, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(result);
            placeRedisTemplate.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache nearby result: key={}, error={}", key, e.getMessage());
        }
    }

    private Place deserializePlace(String json) {
        try {
            return objectMapper.readValue(json, Place.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize place from cache: {}", e.getMessage());
            return null;
        }
    }

    private List<Place> deserializePlaceList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Place>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize place list from cache: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private NearbySearchResult deserializeNearbyResult(String json) {
        try {
            return objectMapper.readValue(json, NearbySearchResult.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize nearby result from cache: {}", e.getMessage());
            return new NearbySearchResult(new ArrayList<>(), 0);
        }
    }
}
