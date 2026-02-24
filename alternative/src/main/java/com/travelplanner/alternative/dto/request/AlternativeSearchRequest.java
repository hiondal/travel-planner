package com.travelplanner.alternative.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대안 장소 검색 요청 DTO.
 */
@Getter
@NoArgsConstructor
public class AlternativeSearchRequest {

    @NotBlank
    @JsonProperty("place_id")
    private String placeId;

    @NotBlank
    @JsonProperty("category")
    private String category;

    @NotNull
    @Valid
    @JsonProperty("location")
    private CoordinatesDto location;
}
