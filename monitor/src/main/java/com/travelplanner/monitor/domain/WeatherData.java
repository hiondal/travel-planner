package com.travelplanner.monitor.domain;

import lombok.Getter;

/**
 * 날씨 데이터.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
public class WeatherData {

    private final int precipitationProb;
    private final String condition;
    private final boolean isFallback;

    public WeatherData(int precipitationProb, String condition, boolean isFallback) {
        this.precipitationProb = precipitationProb;
        this.condition = condition;
        this.isFallback = isFallback;
    }

    public static WeatherData unknown() {
        return new WeatherData(0, "UNKNOWN", false);
    }

    public static WeatherData fallback(int precipitationProb, String condition) {
        return new WeatherData(precipitationProb, condition, true);
    }
}
