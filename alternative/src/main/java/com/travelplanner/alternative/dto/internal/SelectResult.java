package com.travelplanner.alternative.dto.internal;

import lombok.Getter;

/**
 * 대안 선택 서비스 내부 결과.
 */
@Getter
public class SelectResult {

    private final String scheduleItemId;
    private final String originalPlaceId;
    private final String originalPlaceName;
    private final String newPlaceId;
    private final String newPlaceName;
    private final int travelTimeDiffMinutes;

    public SelectResult(String scheduleItemId, String originalPlaceId, String originalPlaceName,
                        String newPlaceId, String newPlaceName, int travelTimeDiffMinutes) {
        this.scheduleItemId = scheduleItemId;
        this.originalPlaceId = originalPlaceId;
        this.originalPlaceName = originalPlaceName;
        this.newPlaceId = newPlaceId;
        this.newPlaceName = newPlaceName;
        this.travelTimeDiffMinutes = travelTimeDiffMinutes;
    }
}
