package com.travelplanner.briefing.generator;

import com.travelplanner.briefing.domain.BriefingContext;
import com.travelplanner.briefing.domain.BriefingText;

/**
 * 브리핑 텍스트 생성 인터페이스.
 *
 * <p>Phase 1: RuleBasedBriefingGenerator 구현.</p>
 * <p>Phase 2: LLMBriefingGenerator로 교체 예정.</p>
 */
public interface BriefingTextGenerator {

    /**
     * 브리핑 컨텍스트를 기반으로 브리핑 텍스트를 생성한다.
     *
     * @param context 브리핑 생성 컨텍스트
     * @return 생성된 브리핑 텍스트
     */
    BriefingText generate(BriefingContext context);
}
