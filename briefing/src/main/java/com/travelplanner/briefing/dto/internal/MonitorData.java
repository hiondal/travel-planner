package com.travelplanner.briefing.dto.internal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MNTR 서비스에서 조회한 장소 상태 데이터.
 */
@Getter
@Setter
@NoArgsConstructor
public class MonitorData {

    private String placeId;
    private String businessStatus;
    private String congestion;
    private String weather;
    private int precipitationProb;
    private int walkingMinutes;
    private Integer transitMinutes;
    private int distanceM;
    private String overallStatus;
    private String placeName;
}
