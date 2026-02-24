package com.travelplanner.alternative.domain;

import lombok.Getter;

/**
 * 점수가 계산된 대안 장소 후보.
 */
@Getter
public class ScoredCandidate {

    private final PlaceCandidate candidate;
    private final double distanceScore;
    private final double ratingScore;
    private final double congestionScore;
    private final double totalScore;

    public ScoredCandidate(PlaceCandidate candidate, double distanceScore,
                           double ratingScore, double congestionScore, double totalScore) {
        this.candidate = candidate;
        this.distanceScore = distanceScore;
        this.ratingScore = ratingScore;
        this.congestionScore = congestionScore;
        this.totalScore = totalScore;
    }
}
