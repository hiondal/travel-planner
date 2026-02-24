package com.travelplanner.monitor.service;

import com.travelplanner.monitor.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StatusJudgmentService 단위 테스트.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class StatusJudgmentServiceTest {

    @InjectMocks
    private StatusJudgmentService statusJudgmentService;

    private MonitoringTarget normalTarget;
    private MonitoringTarget failedTarget;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(statusJudgmentService, "maxConsecutiveFailures", 3);

        normalTarget = new MonitoringTarget(
            "mt_001", "place_001", "trip_001", "si_001", "user_001",
            LocalDateTime.now().plusHours(1), 35.68, 139.76);

        failedTarget = new MonitoringTarget(
            "mt_002", "place_002", "trip_001", "si_002", "user_001",
            LocalDateTime.now().plusHours(1), 35.68, 139.76);
        for (int i = 0; i < 3; i++) failedTarget.incrementFailure();
    }

    @Test
    @DisplayName("3회 연속 실패 - GREY 반환")
    void judge_consecutiveFailures_grey() {
        PlaceStatusEnum result = statusJudgmentService.judge(
            failedTarget,
            new BusinessStatusData("OPEN", false),
            new WeatherData(10, "Clear", false),
            new TravelTimeData(10, null, 500, false),
            null
        );

        assertThat(result).isEqualTo(PlaceStatusEnum.GREY);
    }

    @Test
    @DisplayName("모든 항목 정상 - GREEN 반환")
    void judge_allNormal_green() {
        PlaceStatusEnum result = statusJudgmentService.judge(
            normalTarget,
            new BusinessStatusData("OPEN", false),
            new WeatherData(10, "Clear", false),
            new TravelTimeData(10, null, 500, false),
            20
        );

        assertThat(result).isEqualTo(PlaceStatusEnum.GREEN);
    }

    @Test
    @DisplayName("날씨 강수 확률 40% 이상 - YELLOW 반환")
    void judge_weatherWarning_yellow() {
        PlaceStatusEnum result = statusJudgmentService.judge(
            normalTarget,
            new BusinessStatusData("OPEN", false),
            new WeatherData(50, "Rain", false),
            new TravelTimeData(10, null, 500, false),
            null
        );

        assertThat(result).isEqualTo(PlaceStatusEnum.YELLOW);
    }

    @Test
    @DisplayName("날씨 강수 확률 70% 이상 - RED 반환")
    void judge_weatherDanger_red() {
        PlaceStatusEnum result = statusJudgmentService.judge(
            normalTarget,
            new BusinessStatusData("OPEN", false),
            new WeatherData(80, "Storm", false),
            new TravelTimeData(10, null, 500, false),
            null
        );

        assertThat(result).isEqualTo(PlaceStatusEnum.RED);
    }

    @Test
    @DisplayName("영업 종료 - RED 반환")
    void judge_businessClosed_red() {
        PlaceStatusEnum result = statusJudgmentService.judge(
            normalTarget,
            new BusinessStatusData("CLOSED_PERMANENTLY", false),
            new WeatherData(10, "Clear", false),
            new TravelTimeData(10, null, 500, false),
            null
        );

        assertThat(result).isEqualTo(PlaceStatusEnum.RED);
    }

    @Test
    @DisplayName("혼잡도 80 이상 - RED 반환")
    void judge_highCongestion_red() {
        PlaceStatusEnum result = statusJudgmentService.judge(
            normalTarget,
            new BusinessStatusData("OPEN", false),
            new WeatherData(10, "Clear", false),
            new TravelTimeData(10, null, 500, false),
            85
        );

        assertThat(result).isEqualTo(PlaceStatusEnum.RED);
    }

    @Test
    @DisplayName("혼잡도 50 이상 - YELLOW 반환")
    void judge_mediumCongestion_yellow() {
        PlaceStatusEnum result = statusJudgmentService.judge(
            normalTarget,
            new BusinessStatusData("OPEN", false),
            new WeatherData(10, "Clear", false),
            new TravelTimeData(10, null, 500, false),
            60
        );

        assertThat(result).isEqualTo(PlaceStatusEnum.YELLOW);
    }

    @Test
    @DisplayName("영업 상태 판정 - OPEN은 NORMAL")
    void judgeBusinessStatus_open_normal() {
        ItemStatus result = statusJudgmentService
            .judgeBusinessStatus(new BusinessStatusData("OPEN", false));
        assertThat(result).isEqualTo(ItemStatus.NORMAL);
    }

    @Test
    @DisplayName("영업 상태 판정 - CLOSED_PERMANENTLY는 DANGER")
    void judgeBusinessStatus_closedPermanently_danger() {
        ItemStatus result = statusJudgmentService
            .judgeBusinessStatus(new BusinessStatusData("CLOSED_PERMANENTLY", false));
        assertThat(result).isEqualTo(ItemStatus.DANGER);
    }

    @Test
    @DisplayName("상태 집계 - DANGER 있으면 RED")
    void aggregateStatus_hasDanger_red() {
        PlaceStatusEnum result = statusJudgmentService.aggregateStatus(
            List.of(ItemStatus.NORMAL, ItemStatus.WARNING, ItemStatus.DANGER));
        assertThat(result).isEqualTo(PlaceStatusEnum.RED);
    }

    @Test
    @DisplayName("상태 집계 - WARNING만 있으면 YELLOW")
    void aggregateStatus_hasWarning_yellow() {
        PlaceStatusEnum result = statusJudgmentService.aggregateStatus(
            List.of(ItemStatus.NORMAL, ItemStatus.WARNING));
        assertThat(result).isEqualTo(PlaceStatusEnum.YELLOW);
    }
}
