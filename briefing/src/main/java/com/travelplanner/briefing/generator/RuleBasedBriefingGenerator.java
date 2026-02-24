package com.travelplanner.briefing.generator;

import com.travelplanner.briefing.domain.BriefingContext;
import com.travelplanner.briefing.domain.BriefingText;
import com.travelplanner.briefing.domain.RiskItem;
import com.travelplanner.briefing.domain.StatusLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 규칙 기반 브리핑 텍스트 생성기.
 *
 * <p>Phase 1 구현 — 템플릿 기반 텍스트 생성.</p>
 * <p>Phase 2에서 LLMBriefingGenerator로 교체 예정.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Slf4j
@Component
public class RuleBasedBriefingGenerator implements BriefingTextGenerator {

    private static final String SAFE_TEMPLATE = "현재까지 모든 항목 정상입니다. 예정대로 출발하세요.";
    private static final String WARNING_TEMPLATE = "%s이(가) 감지되었습니다. 대안을 확인해보세요.";

    @Override
    public BriefingText generate(BriefingContext context) {
        log.debug("규칙 기반 브리핑 텍스트 생성: statusLevel={}", context.getStatusLevel());
        String text;
        if (context.getStatusLevel() == StatusLevel.SAFE) {
            text = buildSafeText();
        } else {
            text = buildWarningText(context.getRiskItems());
        }
        return new BriefingText(text);
    }

    private String buildSafeText() {
        return SAFE_TEMPLATE;
    }

    private String buildWarningText(List<RiskItem> riskItems) {
        if (riskItems == null || riskItems.isEmpty()) {
            return String.format(WARNING_TEMPLATE, "이상 상황");
        }
        String riskLabels = joinRiskLabels(riskItems);
        return String.format(WARNING_TEMPLATE, riskLabels);
    }

    private String joinRiskLabels(List<RiskItem> riskItems) {
        return riskItems.stream()
                .map(RiskItem::getLabel)
                .collect(Collectors.joining(", "));
    }
}
