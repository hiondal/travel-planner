package com.travelplanner.alternative.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.alternative.domain.Alternative;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 대안 검색 응답 DTO.
 */
@Getter
public class AlternativeSearchResponse {

    @JsonProperty("original_place_id")
    private final String originalPlaceId;

    @JsonProperty("alternatives")
    private final List<AlternativeCardDto> cards;

    @JsonProperty("radius_used")
    private final int radiusUsed;

    private AlternativeSearchResponse(String originalPlaceId, List<AlternativeCardDto> cards, int radiusUsed) {
        this.originalPlaceId = originalPlaceId;
        this.cards = cards;
        this.radiusUsed = radiusUsed;
    }

    public static AlternativeSearchResponse of(String originalPlaceId, List<Alternative> alternatives, int radiusUsed) {
        List<AlternativeCardDto> cards = new java.util.ArrayList<>();
        for (int i = 0; i < alternatives.size(); i++) {
            cards.add(AlternativeCardDto.from(alternatives.get(i), i + 1));
        }
        return new AlternativeSearchResponse(originalPlaceId, cards, radiusUsed);
    }
}
