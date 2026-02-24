package com.travelplanner.alternative.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Paywall 응답 DTO.
 */
@Getter
public class PaywallResponse {

    @JsonProperty("paywall")
    private final boolean paywall;

    @JsonProperty("message")
    private final String message;

    @JsonProperty("upgrade_url")
    private final String upgradeUrl;

    public PaywallResponse(String message, String upgradeUrl) {
        this.paywall = true;
        this.message = message;
        this.upgradeUrl = upgradeUrl;
    }
}
