package com.travelplanner.schedule.dto.internal;

import com.travelplanner.schedule.domain.ScheduleItem;
import lombok.Builder;
import lombok.Getter;

/**
 * 일정 아이템 추가 내부 결과.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@Builder
public class ScheduleItemAddResult {

    private final ScheduleItem item;
    private final boolean outsideBusinessHours;
    private final String businessHoursRange;
}
