package com.travelplanner.alternative.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.travelplanner.alternative.client.MonitorServiceClient;
import com.travelplanner.alternative.client.PlaceServiceClient;
import com.travelplanner.alternative.client.ScheduleServiceClient;
import com.travelplanner.alternative.domain.*;
import com.travelplanner.alternative.dto.internal.AlternativeSearchResult;
import com.travelplanner.alternative.dto.internal.ReplaceResult;
import com.travelplanner.alternative.dto.internal.SelectResult;
import com.travelplanner.alternative.repository.AlternativeCardSnapshotRepository;
import com.travelplanner.alternative.repository.AlternativeRepository;
import com.travelplanner.alternative.repository.SelectionLogRepository;
import com.travelplanner.alternative.scoring.FixedScoreWeightsProvider;
import com.travelplanner.alternative.scoring.ScoreCalculator;
import com.travelplanner.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * AlternativeServiceImpl 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class AlternativeServiceImplTest {

    @Mock
    private AlternativeRepository alternativeRepository;

    @Mock
    private AlternativeCardSnapshotRepository snapshotRepository;

    @Mock
    private SelectionLogRepository selectionLogRepository;

    @Mock
    private PlaceServiceClient placeServiceClient;

    @Mock
    private MonitorServiceClient monitorServiceClient;

    @Mock
    private ScheduleServiceClient scheduleServiceClient;

    @Spy
    private FixedScoreWeightsProvider scoreWeightsProvider;

    @Spy
    private ScoreCalculator scoreCalculator;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;

    @InjectMocks
    private AlternativeServiceImpl alternativeService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // ObjectMapper 필드 주입
        try {
            var field = AlternativeServiceImpl.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            field.set(alternativeService, objectMapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("searchAlternatives 테스트")
    class SearchAlternativesTest {

        @Test
        @DisplayName("대안 장소를 검색하여 카드 3장을 생성한다")
        void searchAlternatives_success() {
            // given
            given(valueOperations.get(anyString())).willReturn(null);
            List<PlaceCandidate> candidates = createMockCandidates();
            given(placeServiceClient.searchNearby(anyDouble(), anyDouble(), anyString(), anyInt()))
                    .willReturn(candidates);
            given(monitorServiceClient.getBadges(anyList()))
                    .willReturn(createGreenBadgeMap(candidates));
            given(alternativeRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(snapshotRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // when
            AlternativeSearchResult result = alternativeService.searchAlternatives(
                    "user_001", "place_001", "라멘", 35.6595, 139.7004);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getAlternatives()).hasSize(3);
            assertThat(result.getRadiusUsed()).isEqualTo(1000);
        }

        @Test
        @DisplayName("RED 상태 장소는 후보에서 제외된다")
        void searchAlternatives_excludeRedStatus() {
            // given
            given(valueOperations.get(anyString())).willReturn(null);
            List<PlaceCandidate> candidates = createMockCandidates();
            given(placeServiceClient.searchNearby(anyDouble(), anyDouble(), anyString(), anyInt()))
                    .willReturn(candidates);

            // 첫 번째 장소를 RED 상태로 설정
            Map<String, StatusBadge> badgeMap = createGreenBadgeMap(candidates);
            StatusBadge redBadge = new StatusBadge();
            redBadge.setPlaceId(candidates.get(0).getPlaceId());
            redBadge.setStatus("RED");
            badgeMap.put(candidates.get(0).getPlaceId(), redBadge);
            given(monitorServiceClient.getBadges(anyList())).willReturn(badgeMap);

            given(alternativeRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(snapshotRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // when
            AlternativeSearchResult result = alternativeService.searchAlternatives(
                    "user_001", "place_001", "라멘", 35.6595, 139.7004);

            // then
            // RED 1개 제외 후 2개만 남음
            assertThat(result.getAlternatives()).hasSize(2);
        }

        @Test
        @DisplayName("캐시 히트 시 캐시 데이터를 반환한다")
        void searchAlternatives_cacheHit() throws Exception {
            // given
            List<Alternative> alternatives = createMockAlternatives();
            AlternativeSearchResult cachedResult = new AlternativeSearchResult(alternatives, 1000);
            String cachedJson = objectMapper.writeValueAsString(cachedResult);

            given(valueOperations.get(anyString())).willReturn(cachedJson);

            // when
            AlternativeSearchResult result = alternativeService.searchAlternatives(
                    "user_001", "place_001", "라멘", 35.6595, 139.7004);

            // then
            assertThat(result).isNotNull();
            verify(placeServiceClient, never()).searchNearby(anyDouble(), anyDouble(), anyString(), anyInt());
        }
    }

    @Nested
    @DisplayName("selectAlternative 테스트")
    class SelectAlternativeTest {

        @Test
        @DisplayName("대안을 선택하면 일정에 반영된다")
        void selectAlternative_success() {
            // given
            Alternative alternative = createAlternative("alt_001", "user_001");
            given(alternativeRepository.findById("alt_001")).willReturn(Optional.of(alternative));

            ReplaceResult replaceResult = new ReplaceResult();
            replaceResult.setSuccess(true);
            replaceResult.setTravelTimeDiffMinutes(-3);
            replaceResult.setNewPlaceName(alternative.getName());
            given(scheduleServiceClient.replaceScheduleItem(anyString(), anyString(), anyString())).willReturn(replaceResult);

            given(selectionLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // when
            SelectResult result = alternativeService.selectAlternative(
                    "user_001", "alt_001", "place_001", "si_001", "trip_001", 1, 12);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getScheduleItemId()).isEqualTo("si_001");
            assertThat(result.getTravelTimeDiffMinutes()).isEqualTo(-3);
            verify(selectionLogRepository).save(any(SelectionLog.class));
        }

        @Test
        @DisplayName("존재하지 않는 대안 선택 시 ResourceNotFoundException이 발생한다")
        void selectAlternative_notFound() {
            // given
            given(alternativeRepository.findById("alt_999")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> alternativeService.selectAlternative(
                    "user_001", "alt_999", "place_001", "si_001", "trip_001", 1, 12))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    private List<PlaceCandidate> createMockCandidates() {
        List<PlaceCandidate> candidates = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            PlaceCandidate c = new PlaceCandidate();
            c.setPlaceId("mock_place_00" + i);
            c.setName("Mock 장소 " + i);
            c.setDistanceM(300 * i);
            c.setRating(4.0f);
            c.setLat(35.6595 + i * 0.001);
            c.setLng(139.7004 + i * 0.001);
            c.setOpen(true);
            c.setCongestion("낮음");
            c.setWalkingMinutes(5 * i);
            candidates.add(c);
        }
        return candidates;
    }

    private Map<String, StatusBadge> createGreenBadgeMap(List<PlaceCandidate> candidates) {
        Map<String, StatusBadge> map = new HashMap<>();
        for (PlaceCandidate c : candidates) {
            StatusBadge badge = new StatusBadge();
            badge.setPlaceId(c.getPlaceId());
            badge.setStatus("GREEN");
            map.put(c.getPlaceId(), badge);
        }
        return map;
    }

    private List<Alternative> createMockAlternatives() {
        List<Alternative> alternatives = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            PlaceCandidate candidate = new PlaceCandidate();
            candidate.setPlaceId("mock_place_00" + i);
            candidate.setName("Mock 장소 " + i);
            candidate.setDistanceM(300 * i);
            candidate.setRating(4.0f);
            candidate.setLat(35.6595);
            candidate.setLng(139.7004);
            candidate.setCongestion("낮음");
            candidate.setWalkingMinutes(5);
            Alternative alt = Alternative.create("alt_00" + i, "user_001", "place_001",
                    candidate, "근거리 추천", 0.8);
            alternatives.add(alt);
        }
        return alternatives;
    }

    private Alternative createAlternative(String id, String userId) {
        PlaceCandidate candidate = new PlaceCandidate();
        candidate.setPlaceId("mock_place_001");
        candidate.setName("Mock 장소");
        candidate.setDistanceM(300);
        candidate.setRating(4.0f);
        candidate.setLat(35.6595);
        candidate.setLng(139.7004);
        candidate.setCongestion("낮음");
        candidate.setWalkingMinutes(5);
        return Alternative.create(id, userId, "place_001", candidate, "근거리 추천", 0.8);
    }
}
