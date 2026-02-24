package com.travelplanner.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * 가격 정보 DTO.
 */
@Getter
public class PriceDto {

    @JsonProperty("amount")
    private final int amount;

    @JsonProperty("currency")
    private final String currency;

    @JsonProperty("period")
    private final String period;

    public PriceDto(int amount, String currency, String period) {
        this.amount = amount;
        this.currency = currency;
        this.period = period;
    }
}
