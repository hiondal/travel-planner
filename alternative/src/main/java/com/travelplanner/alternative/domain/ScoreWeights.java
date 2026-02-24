package com.travelplanner.alternative.domain;

import lombok.Getter;

/**
 * 대안 점수 가중치.
 */
@Getter
public class ScoreWeights {

    private final double distanceWeight;
    private final double ratingWeight;
    private final double congestionWeight;

    public ScoreWeights(double distanceWeight, double ratingWeight, double congestionWeight) {
        this.distanceWeight = distanceWeight;
        this.ratingWeight = ratingWeight;
        this.congestionWeight = congestionWeight;
        validate();
    }

    public void validate() {
        double sum = distanceWeight + ratingWeight + congestionWeight;
        if (Math.abs(sum - 1.0) > 0.001) {
            throw new IllegalArgumentException("가중치 합이 1.0이어야 합니다: " + sum);
        }
    }
}
