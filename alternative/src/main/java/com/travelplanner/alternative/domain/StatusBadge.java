package com.travelplanner.alternative.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 장소 상태 배지.
 */
@Getter
@Setter
@NoArgsConstructor
public class StatusBadge {

    private String placeId;
    private String status;
    private String icon;
}
