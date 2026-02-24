package com.travelplanner.schedule.dto.internal;

import com.travelplanner.schedule.domain.ScheduleItem;
import com.travelplanner.schedule.domain.Trip;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 일정표 조회 내부 결과.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@Builder
public class ScheduleResult {

    private final Trip trip;
    private final List<ScheduleItem> items;
}
