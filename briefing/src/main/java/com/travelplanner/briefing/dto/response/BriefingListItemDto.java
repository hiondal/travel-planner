package com.travelplanner.briefing.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.briefing.domain.Briefing;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 브리핑 목록 항목 DTO.
 */
@Getter
public class BriefingListItemDto {

    @JsonProperty("briefing_id")
    private final String briefingId;

    @JsonProperty("type")
    private final String type;

    @JsonProperty("place_id")
    private final String placeId;

    @JsonProperty("place_name")
    private final String placeName;

    @JsonProperty("created_at")
    private final LocalDateTime createdAt;

    @JsonProperty("expired")
    private final boolean expired;

    private BriefingListItemDto(Briefing briefing) {
        this.briefingId = briefing.getId();
        this.type = briefing.getType().name();
        this.placeId = briefing.getPlaceId();
        this.placeName = briefing.getPlaceName();
        this.createdAt = briefing.getCreatedAt();
        this.expired = briefing.isExpired();
    }

    public static BriefingListItemDto from(Briefing briefing) {
        return new BriefingListItemDto(briefing);
    }
}
