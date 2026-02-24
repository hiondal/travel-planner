package com.travelplanner.schedule.dto.internal;

import com.travelplanner.schedule.domain.ScheduleItem;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 일정 장소 교체 내부 결과.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@Builder
public class ReplaceResult {

    private final ScheduleItem item;
    private final String originalPlaceId;
    private final String originalPlaceName;
    private final int travelTimeDiffMinutes;
    private final List<ScheduleItem> updatedItems;
}
