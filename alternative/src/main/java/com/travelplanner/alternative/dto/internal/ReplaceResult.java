package com.travelplanner.alternative.dto.internal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SCHD 서비스 일정 교체 결과.
 */
@Getter
@Setter
@NoArgsConstructor
public class ReplaceResult {

    private boolean success;
    private int travelTimeDiffMinutes;
    private String newPlaceName;
}
