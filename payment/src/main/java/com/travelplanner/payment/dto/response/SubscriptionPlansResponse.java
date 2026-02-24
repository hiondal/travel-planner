package com.travelplanner.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.payment.domain.SubscriptionPlan;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 구독 플랜 목록 응답 DTO.
 */
@Getter
public class SubscriptionPlansResponse {

    @JsonProperty("plans")
    private final List<SubscriptionPlanDto> plans;

    private SubscriptionPlansResponse(List<SubscriptionPlanDto> plans) {
        this.plans = plans;
    }

    public static SubscriptionPlansResponse of(List<SubscriptionPlan> plans) {
        List<SubscriptionPlanDto> dtos = plans.stream()
                .map(SubscriptionPlanDto::from)
                .collect(Collectors.toList());
        return new SubscriptionPlansResponse(dtos);
    }
}
