package com.travelplanner.alternative.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 좌표 DTO.
 */
@Getter
@NoArgsConstructor
public class CoordinatesDto {

    @NotNull
    @JsonProperty("lat")
    private Double lat;

    @NotNull
    @JsonProperty("lng")
    private Double lng;
}
