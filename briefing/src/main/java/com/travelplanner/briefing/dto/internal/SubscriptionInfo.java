package com.travelplanner.briefing.dto.internal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PAY 서비스에서 조회한 구독 정보.
 */
@Getter
@Setter
@NoArgsConstructor
public class SubscriptionInfo {

    private String tier;
    private int todayBriefingCount;
}
