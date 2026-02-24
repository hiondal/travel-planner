package com.travelplanner.briefing.domain;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 브리핑 본문 내용 (임베디드 객체).
 */
@Embeddable
@Getter
@NoArgsConstructor
public class BriefingContent {

    private String businessStatus;
    private String congestion;
    private String weather;
    private Integer walkingMinutes;
    private Integer transitMinutes;
    private Integer distanceM;

    public BriefingContent(String businessStatus, String congestion, String weather,
                           Integer walkingMinutes, Integer transitMinutes, Integer distanceM) {
        this.businessStatus = businessStatus;
        this.congestion = congestion;
        this.weather = weather;
        this.walkingMinutes = walkingMinutes;
        this.transitMinutes = transitMinutes;
        this.distanceM = distanceM;
    }
}
