package com.travelplanner.alternative.dto.internal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.alternative.domain.Alternative;
import lombok.Getter;

import java.util.List;

/**
 * 대안 검색 서비스 내부 결과.
 */
@Getter
public class AlternativeSearchResult {

    @JsonProperty("alternatives")
    private final List<Alternative> alternatives;

    @JsonProperty("radius_used")
    private final int radiusUsed;

    @JsonCreator
    public AlternativeSearchResult(
            @JsonProperty("alternatives") List<Alternative> alternatives,
            @JsonProperty("radius_used") int radiusUsed) {
        this.alternatives = alternatives;
        this.radiusUsed = radiusUsed;
    }
}
