package com.travelplanner.schedule.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.schedule.domain.ScheduleItem;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 일정 아이템 요약 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@Builder
public class ScheduleItemSummary {

    @JsonProperty("schedule_item_id")
    private String scheduleItemId;

    @JsonProperty("place_id")
    private String placeId;

    @JsonProperty("place_name")
    private String placeName;

    @JsonProperty("visit_datetime")
    private LocalDateTime visitDatetime;

    @JsonProperty("timezone")
    private String timezone;

    @JsonProperty("order")
    private int order;

    @JsonProperty("outside_business_hours")
    private boolean outsideBusinessHours;

    public static ScheduleItemSummary from(ScheduleItem item) {
        return ScheduleItemSummary.builder()
            .scheduleItemId(item.getId())
            .placeId(item.getPlaceId())
            .placeName(item.getPlaceName())
            .visitDatetime(item.getVisitDatetime())
            .timezone(item.getTimezone())
            .order(item.getSortOrder())
            .outsideBusinessHours(item.isOutsideBusinessHours())
            .build();
    }
}
