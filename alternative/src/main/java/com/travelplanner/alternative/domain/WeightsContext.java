package com.travelplanner.alternative.domain;

import lombok.Getter;

/**
 * 가중치 계산 컨텍스트.
 */
@Getter
public class WeightsContext {

    private final String userId;
    private final String category;
    private final String timeOfDay;

    public WeightsContext(String userId, String category, String timeOfDay) {
        this.userId = userId;
        this.category = category;
        this.timeOfDay = timeOfDay;
    }
}
