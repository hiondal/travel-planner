package com.travelplanner.monitor.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.monitor.domain.StatusBadge;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 배지 목록 응답 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@Builder
public class BadgeListResponse {

    @JsonProperty("badges")
    private List<BadgeItemDto> badges;

    public static BadgeListResponse of(List<StatusBadge> badges) {
        return BadgeListResponse.builder()
            .badges(badges.stream()
                .map(BadgeItemDto::from)
                .collect(Collectors.toList()))
            .build();
    }
}
