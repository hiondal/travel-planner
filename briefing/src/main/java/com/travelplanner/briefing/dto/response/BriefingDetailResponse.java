package com.travelplanner.briefing.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.briefing.domain.Briefing;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 브리핑 상세 조회 응답 DTO.
 */
@Getter
public class BriefingDetailResponse {

    @JsonProperty("briefing_id")
    private final String briefingId;

    @JsonProperty("type")
    private final String type;

    @JsonProperty("place_id")
    private final String placeId;

    @JsonProperty("place_name")
    private final String placeName;

    @JsonProperty("departure_time")
    private final LocalDateTime departureTime;

    @JsonProperty("created_at")
    private final LocalDateTime createdAt;

    @JsonProperty("expired")
    private final boolean expired;

    @JsonProperty("expire_message")
    private final String expireMessage;

    @JsonProperty("content")
    private final BriefingContentDto content;

    @JsonProperty("alternative_link")
    private final String alternativeLink;

    private BriefingDetailResponse(Briefing briefing, boolean expired) {
        this.briefingId = briefing.getId();
        this.type = briefing.getType().name();
        this.placeId = briefing.getPlaceId();
        this.placeName = briefing.getPlaceName();
        this.departureTime = briefing.getDepartureTime();
        this.createdAt = briefing.getCreatedAt();
        this.expired = expired;
        this.expireMessage = expired ? "이미 지난 브리핑입니다" : null;
        this.content = new BriefingContentDto(briefing.getContent(), briefing.getSummaryText());
        this.alternativeLink = briefing.getAlternativeLink();
    }

    public static BriefingDetailResponse from(Briefing briefing, boolean expired) {
        return new BriefingDetailResponse(briefing, expired);
    }
}
