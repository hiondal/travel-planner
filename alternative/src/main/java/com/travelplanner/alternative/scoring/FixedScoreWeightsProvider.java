package com.travelplanner.alternative.scoring;

import com.travelplanner.alternative.domain.ScoreWeights;
import com.travelplanner.alternative.domain.WeightsContext;
import org.springframework.stereotype.Component;

/**
 * 고정 가중치 제공자.
 *
 * <p>Phase 1 구현 — 고정 가중치 (w1=0.5, w2=0.3, w3=0.2) 반환.</p>
 * <ul>
 *   <li>w1 (distanceWeight): 0.5 — 거리 점수</li>
 *   <li>w2 (ratingWeight): 0.3 — 평점 점수</li>
 *   <li>w3 (congestionWeight): 0.2 — 혼잡도 점수</li>
 * </ul>
 *
 * <p>Phase 2에서 MLScoreWeightsProvider로 교체 예정.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Component
public class FixedScoreWeightsProvider implements ScoreWeightsProvider {

    private static final double DISTANCE_WEIGHT = 0.5;
    private static final double RATING_WEIGHT = 0.3;
    private static final double CONGESTION_WEIGHT = 0.2;

    @Override
    public ScoreWeights getWeights(WeightsContext context) {
        return new ScoreWeights(DISTANCE_WEIGHT, RATING_WEIGHT, CONGESTION_WEIGHT);
    }
}
