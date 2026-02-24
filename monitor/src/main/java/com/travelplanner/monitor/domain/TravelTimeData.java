package com.travelplanner.monitor.domain;

import lombok.Getter;

/**
 * 이동시간 데이터.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
public class TravelTimeData {

    private final int walkingMinutes;
    private final Integer transitMinutes;
    private final int distanceM;
    private final boolean isFallback;

    public TravelTimeData(int walkingMinutes, Integer transitMinutes,
                          int distanceM, boolean isFallback) {
        this.walkingMinutes = walkingMinutes;
        this.transitMinutes = transitMinutes;
        this.distanceM = distanceM;
        this.isFallback = isFallback;
    }

    public static TravelTimeData unknown() {
        return new TravelTimeData(0, null, 0, false);
    }
}
