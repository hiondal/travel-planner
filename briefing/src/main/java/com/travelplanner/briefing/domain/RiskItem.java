package com.travelplanner.briefing.domain;

import lombok.Getter;

/**
 * 브리핑 위험 항목.
 */
@Getter
public class RiskItem {

    private final String label;
    private final String severity;

    public RiskItem(String label, String severity) {
        this.label = label;
        this.severity = severity;
    }
}
