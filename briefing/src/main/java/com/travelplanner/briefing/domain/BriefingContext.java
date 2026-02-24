package com.travelplanner.briefing.domain;

import com.travelplanner.briefing.dto.internal.MonitorData;
import lombok.Getter;

import java.util.List;

/**
 * 브리핑 텍스트 생성에 필요한 컨텍스트.
 */
@Getter
public class BriefingContext {

    private final MonitorData placeStatus;
    private final StatusLevel statusLevel;
    private final List<RiskItem> riskItems;
    private final String subscriptionTier;

    public BriefingContext(MonitorData placeStatus, StatusLevel statusLevel,
                           List<RiskItem> riskItems, String subscriptionTier) {
        this.placeStatus = placeStatus;
        this.statusLevel = statusLevel;
        this.riskItems = riskItems;
        this.subscriptionTier = subscriptionTier;
    }
}
