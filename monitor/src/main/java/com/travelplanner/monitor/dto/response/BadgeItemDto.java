package com.travelplanner.monitor.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.monitor.domain.StatusBadge;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 배지 아이템 응답 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@Builder
public class BadgeItemDto {

    @JsonProperty("place_id")
    private String placeId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("icon")
    private String icon;

    @JsonProperty("label")
    private String label;

    @JsonProperty("color_hex")
    private String colorHex;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public static BadgeItemDto from(StatusBadge badge) {
        return BadgeItemDto.builder()
            .placeId(badge.getPlaceId())
            .status(badge.getStatus().name())
            .icon(badge.getIcon().name())
            .label(badge.getLabel())
            .colorHex(badge.getColorHex())
            .updatedAt(badge.getUpdatedAt())
            .build();
    }
}
