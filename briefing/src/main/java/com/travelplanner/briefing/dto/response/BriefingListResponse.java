package com.travelplanner.briefing.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.briefing.domain.Briefing;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 브리핑 목록 조회 응답 DTO.
 */
@Getter
public class BriefingListResponse {

    @JsonProperty("date")
    private final LocalDate date;

    @JsonProperty("briefings")
    private final List<BriefingListItemDto> briefings;

    private BriefingListResponse(LocalDate date, List<BriefingListItemDto> briefings) {
        this.date = date;
        this.briefings = briefings;
    }

    public static BriefingListResponse of(LocalDate date, List<Briefing> briefings) {
        List<BriefingListItemDto> items = briefings.stream()
                .map(BriefingListItemDto::from)
                .collect(Collectors.toList());
        return new BriefingListResponse(date, items);
    }
}
