package com.travelplanner.alternative.scoring;

import com.travelplanner.alternative.domain.ScoreWeights;
import com.travelplanner.alternative.domain.WeightsContext;

/**
 * 대안 점수 가중치 제공 인터페이스.
 *
 * <p>Phase 1: FixedScoreWeightsProvider 구현.</p>
 * <p>Phase 2: MLScoreWeightsProvider로 교체 예정.</p>
 */
public interface ScoreWeightsProvider {

    ScoreWeights getWeights(WeightsContext context);
}
