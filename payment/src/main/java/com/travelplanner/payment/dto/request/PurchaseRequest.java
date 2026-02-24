package com.travelplanner.payment.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 구독 구매 요청 DTO.
 */
@Getter
@NoArgsConstructor
public class PurchaseRequest {

    @NotBlank
    @JsonProperty("plan_id")
    private String planId;

    @NotBlank
    @JsonProperty("receipt")
    private String receipt;

    @NotBlank
    @Pattern(regexp = "apple|google", message = "provider는 apple 또는 google이어야 합니다.")
    @JsonProperty("provider")
    private String provider;
}
