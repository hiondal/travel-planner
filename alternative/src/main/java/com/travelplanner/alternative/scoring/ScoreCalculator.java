package com.travelplanner.alternative.scoring;

import com.travelplanner.alternative.domain.PlaceCandidate;
import com.travelplanner.alternative.domain.ScoreWeights;
import com.travelplanner.alternative.domain.ScoredCandidate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 대안 장소 점수 계산기.
 *
 * <p>거리, 평점, 혼잡도를 기반으로 종합 점수를 계산한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Slf4j
@Component
public class ScoreCalculator {

    private static final int MAX_DISTANCE_M = 3000;
    private static final float MAX_RATING = 5.0f;

    /**
     * 후보 장소들의 점수를 계산한다.
     *
     * @param candidates 후보 장소 목록
     * @param weights    가중치
     * @return 점수가 계산된 후보 목록 (내림차순 정렬)
     */
    public List<ScoredCandidate> calculateScores(List<PlaceCandidate> candidates, ScoreWeights weights) {
        return candidates.stream()
                .map(candidate -> scoreCandiate(candidate, weights))
                .sorted((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()))
                .collect(Collectors.toList());
    }

    private ScoredCandidate scoreCandiate(PlaceCandidate candidate, ScoreWeights weights) {
        double distanceScore = normalizeDistance(candidate.getDistanceM(), MAX_DISTANCE_M);
        double ratingScore = normalizeRating(candidate.getRating());
        double congestionScore = normalizeCongestion(candidate.getCongestion());
        double totalScore = computeScore(distanceScore, ratingScore, congestionScore, weights);

        log.debug("점수 계산: placeId={}, distance={:.3f}, rating={:.3f}, congestion={:.3f}, total={:.4f}",
                candidate.getPlaceId(), distanceScore, ratingScore, congestionScore, totalScore);

        return new ScoredCandidate(candidate, distanceScore, ratingScore, congestionScore, totalScore);
    }

    double normalizeDistance(int distanceM, int maxDistance) {
        if (distanceM <= 0) return 1.0;
        return Math.max(0.0, 1.0 - (double) distanceM / maxDistance);
    }

    double normalizeRating(float rating) {
        if (rating <= 0) return 0.5;
        return Math.min(1.0, rating / MAX_RATING);
    }

    double normalizeCongestion(String congestionLevel) {
        if (congestionLevel == null) return 0.5;
        return switch (congestionLevel) {
            case "낮음" -> 1.0;
            case "보통" -> 0.5;
            case "혼잡" -> 0.0;
            default -> 0.5;
        };
    }

    double computeScore(double distScore, double ratingScore, double congestionScore, ScoreWeights weights) {
        return distScore * weights.getDistanceWeight()
                + ratingScore * weights.getRatingWeight()
                + congestionScore * weights.getCongestionWeight();
    }
}
