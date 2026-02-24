package com.travelplanner.place.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.travelplanner.common.exception.ResourceNotFoundException;
import com.travelplanner.place.client.GooglePlaceDto;
import com.travelplanner.place.client.GooglePlacesClient;
import com.travelplanner.place.domain.Place;
import com.travelplanner.place.dto.internal.NearbySearchResult;
import com.travelplanner.place.repository.PlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * PlaceServiceImpl 단위 테스트.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class PlaceServiceImplTest {

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private GooglePlacesClient googlePlacesClient;

    @Mock
    private RedisTemplate<String, String> placeRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Spy
    private ObjectMapper objectMapper = createObjectMapper();

    @InjectMocks
    private PlaceServiceImpl placeService;

    private Place samplePlace;
    private GooglePlaceDto sampleGoogleDto;

    @BeforeEach
    void setUp() {
        samplePlace = Place.create(
                "place_abc123",
                "이치란 라멘 시부야",
                "도쿄 시부야구 도겐자카 1-22-7",
                "restaurant",
                4.2f,
                35.6595,
                139.7004,
                "Asia/Tokyo",
                "https://example.com/photo.jpg",
                "도쿄"
        );

        sampleGoogleDto = new GooglePlaceDto();
        sampleGoogleDto.setPlaceId("place_abc123");
        sampleGoogleDto.setName("이치란 라멘 시부야");
        sampleGoogleDto.setAddress("도쿄 시부야구 도겐자카 1-22-7");
        sampleGoogleDto.setRating(4.2f);
        sampleGoogleDto.setLat(35.6595);
        sampleGoogleDto.setLng(139.7004);
        sampleGoogleDto.setCategory("restaurant");
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    // ===== searchPlaces 테스트 =====

    @Test
    @DisplayName("장소 검색 - Redis 캐시 히트 시 DB/API 호출 없음")
    void searchPlaces_cacheHit_returnsFromCache() throws Exception {
        // given
        List<Place> cachedPlaces = List.of(samplePlace);
        String cachedJson = objectMapper.writeValueAsString(cachedPlaces);

        given(placeRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(cachedJson);

        // when
        List<Place> result = placeService.searchPlaces("라멘", "도쿄");

        // then
        assertThat(result).isNotEmpty();
        verify(placeRepository, never()).findByCityAndKeyword(anyString(), anyString());
        verify(googlePlacesClient, never()).textSearch(anyString(), anyString());
    }

    @Test
    @DisplayName("장소 검색 - 캐시 미스, DB 히트 시 DB에서 반환")
    void searchPlaces_cacheMiss_dbHit_returnsFromDb() throws Exception {
        // given
        given(placeRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(null);
        given(placeRepository.findByCityAndKeyword("도쿄", "라멘")).willReturn(List.of(samplePlace));

        // when
        List<Place> result = placeService.searchPlaces("라멘", "도쿄");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("이치란 라멘 시부야");
        verify(googlePlacesClient, never()).textSearch(anyString(), anyString());
    }

    @Test
    @DisplayName("장소 검색 - 캐시/DB 모두 미스 시 Google API 호출")
    void searchPlaces_cacheMiss_dbMiss_callsGoogleApi() throws Exception {
        // given
        given(placeRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(null);
        given(placeRepository.findByCityAndKeyword(anyString(), anyString())).willReturn(Collections.emptyList());
        given(googlePlacesClient.textSearch("라멘", "도쿄")).willReturn(List.of(sampleGoogleDto));
        given(placeRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

        // when
        List<Place> result = placeService.searchPlaces("라멘", "도쿄");

        // then
        assertThat(result).hasSize(1);
        verify(googlePlacesClient).textSearch("라멘", "도쿄");
        verify(placeRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("장소 검색 - 검색 결과 최대 10개 제한")
    void searchPlaces_dbHit_returnsMaxTenResults() throws Exception {
        // given
        List<Place> manyPlaces = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            manyPlaces.add(Place.create(
                    "place_" + i, "장소" + i, "주소" + i, "restaurant",
                    4.0f, 35.0 + i * 0.01, 139.0 + i * 0.01, "Asia/Tokyo", null, "도쿄"
            ));
        }

        given(placeRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(null);
        given(placeRepository.findByCityAndKeyword("도쿄", "라멘")).willReturn(manyPlaces);

        // when
        List<Place> result = placeService.searchPlaces("라멘", "도쿄");

        // then
        assertThat(result).hasSizeLessThanOrEqualTo(10);
    }

    // ===== getPlaceDetail 테스트 =====

    @Test
    @DisplayName("장소 상세 조회 - Redis 캐시 히트")
    void getPlaceDetail_cacheHit_returnsFromCache() throws Exception {
        // given
        String cachedJson = objectMapper.writeValueAsString(samplePlace);
        given(placeRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(cachedJson);

        // when
        Place result = placeService.getPlaceDetail("place_abc123");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("place_abc123");
        verify(placeRepository, never()).findById(anyString());
    }

    @Test
    @DisplayName("장소 상세 조회 - 캐시 미스, DB 히트 (최신 데이터)")
    void getPlaceDetail_cacheMiss_dbHit_fresh_returnsFromDb() throws Exception {
        // given
        given(placeRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(null);
        given(placeRepository.findById("place_abc123")).willReturn(Optional.of(samplePlace));

        // when
        Place result = placeService.getPlaceDetail("place_abc123");

        // then
        assertThat(result.getId()).isEqualTo("place_abc123");
        verify(googlePlacesClient, never()).placeDetail(anyString());
    }

    @Test
    @DisplayName("장소 상세 조회 - 존재하지 않는 장소 ID 시 ResourceNotFoundException")
    void getPlaceDetail_notFound_throwsException() {
        // given
        GooglePlaceDto emptyDto = new GooglePlaceDto();

        given(placeRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(null);
        given(placeRepository.findById("not_exist")).willReturn(Optional.empty());
        given(googlePlacesClient.placeDetail("not_exist")).willReturn(emptyDto);

        // when & then
        assertThatThrownBy(() -> placeService.getPlaceDetail("not_exist"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ===== searchNearbyPlaces 테스트 =====

    @Test
    @DisplayName("주변 장소 검색 - Redis 캐시 히트")
    void searchNearbyPlaces_cacheHit_returnsFromCache() throws Exception {
        // given
        NearbySearchResult cachedResult = new NearbySearchResult(Collections.emptyList(), 1000);
        String cachedJson = objectMapper.writeValueAsString(cachedResult);

        given(placeRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(cachedJson);

        // when
        NearbySearchResult result = placeService.searchNearbyPlaces(35.6595, 139.7004, "라멘", 1000);

        // then
        assertThat(result).isNotNull();
        verify(placeRepository, never()).findNearby(anyDouble(), anyDouble(), anyInt(), anyString());
    }

    @Test
    @DisplayName("주변 장소 검색 - 캐시 미스, DB 히트")
    void searchNearbyPlaces_cacheMiss_dbHit() {
        // given
        given(placeRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(null);
        given(placeRepository.findNearby(35.6595, 139.7004, 1000, "라멘"))
                .willReturn(List.of(samplePlace));

        // when
        NearbySearchResult result = placeService.searchNearbyPlaces(35.6595, 139.7004, "라멘", 1000);

        // then
        assertThat(result.getRadiusUsed()).isEqualTo(1000);
        verify(googlePlacesClient, never()).nearbySearch(anyDouble(), anyDouble(), anyInt(), anyString());
    }

    @Test
    @DisplayName("주변 장소 검색 - 거리 필터링 검증")
    void searchNearbyPlaces_filtersOutOfRadiusPlaces() {
        // given
        Place farPlace = Place.create(
                "place_far", "먼 장소", "먼 주소", "restaurant",
                4.0f, 36.0, 140.0, "Asia/Tokyo", null, "도쿄"
        );

        given(placeRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(null);
        given(placeRepository.findNearby(35.6595, 139.7004, 1000, "라멘"))
                .willReturn(Arrays.asList(samplePlace, farPlace));

        // when
        NearbySearchResult result = placeService.searchNearbyPlaces(35.6595, 139.7004, "라멘", 1000);

        // then
        // farPlace는 1000m 반경 밖이므로 필터링됨
        result.getPlaces().forEach(np ->
                assertThat(np.getDistanceM()).isLessThanOrEqualTo(1000));
    }
}
