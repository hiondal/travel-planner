package com.travelplanner.schedule.event;

import lombok.Getter;

/**
 * 일정 장소 삭제 이벤트.
 *
 * <p>MNTR 서비스가 모니터링 대상을 해제하기 위해 구독한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
public class ScheduleItemDeletedEvent {

    private final String scheduleItemId;
    private final String placeId;
    private final String tripId;

    public ScheduleItemDeletedEvent(String scheduleItemId, String placeId, String tripId) {
        this.scheduleItemId = scheduleItemId;
        this.placeId = placeId;
        this.tripId = tripId;
    }
}
