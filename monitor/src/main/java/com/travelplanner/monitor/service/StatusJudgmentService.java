package com.travelplanner.monitor.service;

import com.travelplanner.monitor.domain.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 상태 판정 서비스.
 *
 * <p>수집된 데이터를 기반으로 4단계 상태(GREEN/YELLOW/RED/GREY)를 판정한다.</p>
 *
 * <ul>
 *   <li>GREEN: 모든 항목 NORMAL</li>
 *   <li>YELLOW: 1개 이상 WARNING</li>
 *   <li>RED: 1개 이상 DANGER</li>
 *   <li>GREY: 3회 연속 수집 실패</li>
 * </ul>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Service
@Slf4j
public class StatusJudgmentService {

    @Value("${monitor.collection.max-consecutive-failures:3}")
    private int maxConsecutiveFailures;

    private static final int WEATHER_DANGER_THRESHOLD = 70;
    private static final int WEATHER_WARNING_THRESHOLD = 40;
    private static final int CONGESTION_DANGER_THRESHOLD = 80;
    private static final int CONGESTION_WARNING_THRESHOLD = 50;

    /**
     * 수집 데이터를 바탕으로 종합 상태를 판정한다.
     *
     * @param target 모니터링 대상
     * @param businessStatus 영업 상태 데이터
     * @param weatherData 날씨 데이터
     * @param travelTimeData 이동시간 데이터
     * @param popularity 혼잡도 (0~100, null이면 미확인)
     * @return 판정된 상태
     */
    public PlaceStatusEnum judge(MonitoringTarget target,
                                  BusinessStatusData businessStatus,
                                  WeatherData weatherData,
                                  TravelTimeData travelTimeData,
                                  Integer popularity) {

        if (target.getConsecutiveFailureCount() >= maxConsecutiveFailures) {
            return PlaceStatusEnum.GREY;
        }

        List<ItemStatus> statuses = new ArrayList<>();

        statuses.add(judgeBusinessStatus(businessStatus));
        statuses.add(judgeWeather(weatherData));
        statuses.add(judgeCongestion(popularity));

        return aggregateStatus(statuses);
    }

    /**
     * 종합 상태 판정 사유를 생성한다.
     */
    public String buildReason(BusinessStatusData businessStatus,
                               WeatherData weatherData,
                               Integer popularity) {
        List<String> reasons = new ArrayList<>();

        ItemStatus bsStatus = judgeBusinessStatus(businessStatus);
        if (bsStatus == ItemStatus.DANGER) {
            reasons.add("영업 종료");
        } else if (bsStatus == ItemStatus.WARNING) {
            reasons.add("영업 상태 주의");
        }

        ItemStatus weatherStatus = judgeWeather(weatherData);
        if (weatherStatus == ItemStatus.DANGER) {
            reasons.add("날씨 불량");
        } else if (weatherStatus == ItemStatus.WARNING) {
            reasons.add("날씨 주의");
        }

        ItemStatus congestionStatus = judgeCongestion(popularity);
        if (congestionStatus == ItemStatus.DANGER) {
            reasons.add("혼잡도 높음");
        } else if (congestionStatus == ItemStatus.WARNING) {
            reasons.add("혼잡");
        }

        return reasons.isEmpty() ? "정상" : String.join(", ", reasons);
    }

    ItemStatus judgeBusinessStatus(BusinessStatusData businessStatus) {
        if (businessStatus == null) return ItemStatus.NORMAL;
        String status = businessStatus.getStatus();
        if ("CLOSED_PERMANENTLY".equals(status) || "CLOSED_TEMPORARILY".equals(status)) {
            return ItemStatus.DANGER;
        }
        return ItemStatus.NORMAL;
    }

    ItemStatus judgeWeather(WeatherData weatherData) {
        if (weatherData == null) return ItemStatus.NORMAL;
        int precipProb = weatherData.getPrecipitationProb();
        if (precipProb >= WEATHER_DANGER_THRESHOLD) return ItemStatus.DANGER;
        if (precipProb >= WEATHER_WARNING_THRESHOLD) return ItemStatus.WARNING;
        return ItemStatus.NORMAL;
    }

    ItemStatus judgeCongestion(Integer popularity) {
        if (popularity == null) return ItemStatus.NORMAL;
        if (popularity >= CONGESTION_DANGER_THRESHOLD) return ItemStatus.DANGER;
        if (popularity >= CONGESTION_WARNING_THRESHOLD) return ItemStatus.WARNING;
        return ItemStatus.NORMAL;
    }

    PlaceStatusEnum aggregateStatus(List<ItemStatus> statuses) {
        boolean hasDanger = statuses.stream().anyMatch(s -> s == ItemStatus.DANGER);
        boolean hasWarning = statuses.stream().anyMatch(s -> s == ItemStatus.WARNING);

        if (hasDanger) return PlaceStatusEnum.RED;
        if (hasWarning) return PlaceStatusEnum.YELLOW;
        return PlaceStatusEnum.GREEN;
    }
}
