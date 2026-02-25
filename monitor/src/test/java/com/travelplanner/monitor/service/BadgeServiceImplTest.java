package com.travelplanner.monitor.service;

import com.travelplanner.common.exception.ResourceNotFoundException;
import com.travelplanner.monitor.domain.*;
import com.travelplanner.monitor.dto.internal.StatusDetail;
import com.travelplanner.monitor.repository.CollectedDataRepository;
import com.travelplanner.monitor.repository.MonitoringRepository;
import com.travelplanner.monitor.repository.StatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * BadgeServiceImpl 단위 테스트.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class BadgeServiceImplTest {

    @Mock
    private MonitoringRepository monitoringRepository;

    @Mock
    private StatusHistoryRepository statusHistoryRepository;

    @Mock
    private CollectedDataRepository collectedDataRepository;

    @Mock
    private RedisTemplate<String, String> mntrRedisTemplate;

    @InjectMocks
    private BadgeServiceImpl badgeService;

    private MonitoringTarget sampleTarget;
    private static final String PLACE_ID = "place_abc123";

    @BeforeEach
    void setUp() {
        sampleTarget = new MonitoringTarget(
            "mt_001", PLACE_ID, "trip_001", "si_001", "user_001",
            LocalDateTime.now().plusHours(1), 35.68, 139.76);
        sampleTarget.updateStatus(PlaceStatusEnum.GREEN);
    }

    @Test
    @DisplayName("배지 목록 조회 - Redis 캐시 미스 시 DB 조회")
    void getBadgeStatuses_cacheMiss_dbQuery() {
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        given(mntrRedisTemplate.opsForHash()).willReturn(hashOps);
        given(hashOps.entries(anyString())).willReturn(Map.of());
        given(monitoringRepository.findByPlaceId(PLACE_ID))
            .willReturn(Optional.of(sampleTarget));

        List<StatusBadge> result = badgeService.getBadgeStatuses(List.of(PLACE_ID));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(PlaceStatusEnum.GREEN);
        assertThat(result.get(0).getIcon()).isEqualTo(BadgeIcon.CHECK);
        assertThat(result.get(0).getColorHex()).isEqualTo("#4CAF50");
    }

    @Test
    @DisplayName("배지 목록 조회 - Redis 캐시 히트")
    void getBadgeStatuses_cacheHit() {
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        given(mntrRedisTemplate.opsForHash()).willReturn(hashOps);
        given(hashOps.entries(anyString())).willReturn(Map.of(
            "status", "YELLOW",
            "updatedAt", LocalDateTime.now().toString()
        ));

        List<StatusBadge> result = badgeService.getBadgeStatuses(List.of(PLACE_ID));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(PlaceStatusEnum.YELLOW);
        assertThat(result.get(0).getIcon()).isEqualTo(BadgeIcon.EXCLAMATION);
    }

    @Test
    @DisplayName("배지 목록 조회 - 대상 없으면 GREY 반환")
    void getBadgeStatuses_noTarget_grey() {
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        given(mntrRedisTemplate.opsForHash()).willReturn(hashOps);
        given(hashOps.entries(anyString())).willReturn(Map.of());
        given(monitoringRepository.findByPlaceId(PLACE_ID))
            .willReturn(Optional.empty());

        List<StatusBadge> result = badgeService.getBadgeStatuses(List.of(PLACE_ID));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(PlaceStatusEnum.GREY);
    }

    @Test
    @DisplayName("상태 상세 조회 - 성공")
    void getStatusDetail_success() {
        given(monitoringRepository.findTopByPlaceIdOrderByVisitDatetimeDesc(PLACE_ID))
            .willReturn(Optional.of(sampleTarget));
        given(collectedDataRepository.findLatestByPlaceId(any(), any()))
            .willReturn(List.of());

        StatusDetail result = badgeService.getStatusDetail(PLACE_ID);

        assertThat(result.getPlaceId()).isEqualTo(PLACE_ID);
        assertThat(result.getOverallStatus()).isEqualTo(PlaceStatusEnum.GREEN);
        assertThat(result.isShowAlternativeButton()).isFalse();
    }

    @Test
    @DisplayName("상태 상세 조회 - RED 상태면 대안 버튼 표시")
    void getStatusDetail_red_showAlternativeButton() {
        sampleTarget.updateStatus(PlaceStatusEnum.RED);
        given(monitoringRepository.findTopByPlaceIdOrderByVisitDatetimeDesc(PLACE_ID))
            .willReturn(Optional.of(sampleTarget));
        given(collectedDataRepository.findLatestByPlaceId(any(), any()))
            .willReturn(List.of());

        StatusDetail result = badgeService.getStatusDetail(PLACE_ID);

        assertThat(result.getOverallStatus()).isEqualTo(PlaceStatusEnum.RED);
        assertThat(result.isShowAlternativeButton()).isTrue();
    }

    @Test
    @DisplayName("상태 상세 조회 - 없는 장소면 ResourceNotFoundException")
    void getStatusDetail_notFound() {
        given(monitoringRepository.findTopByPlaceIdOrderByVisitDatetimeDesc(PLACE_ID))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> badgeService.getStatusDetail(PLACE_ID))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("StatusBadge - GREEN 상태 배지 속성 확인")
    void statusBadge_green_attributes() {
        StatusBadge badge = new StatusBadge(PLACE_ID, PlaceStatusEnum.GREEN, LocalDateTime.now());

        assertThat(badge.getIcon()).isEqualTo(BadgeIcon.CHECK);
        assertThat(badge.getColorHex()).isEqualTo("#4CAF50");
        assertThat(badge.getLabel()).isNull();
        assertThat(badge.hasAlert()).isFalse();
        assertThat(badge.shouldShowAlternativeButton()).isFalse();
    }

    @Test
    @DisplayName("StatusBadge - GREY 상태 배지 속성 확인")
    void statusBadge_grey_attributes() {
        StatusBadge badge = new StatusBadge(PLACE_ID, PlaceStatusEnum.GREY, LocalDateTime.now());

        assertThat(badge.getIcon()).isEqualTo(BadgeIcon.QUESTION);
        assertThat(badge.getColorHex()).isEqualTo("#9E9E9E");
        assertThat(badge.getLabel()).isEqualTo("데이터 미확인");
    }
}
