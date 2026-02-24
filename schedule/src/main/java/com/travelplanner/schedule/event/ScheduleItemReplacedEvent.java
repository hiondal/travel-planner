package com.travelplanner.schedule.event;

import lombok.Getter;

/**
 * 일정 장소 교체 이벤트.
 *
 * <p>MNTR 서비스가 기존 장소 모니터링을 해제하고 신규 장소를 등록하기 위해 구독한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
public class ScheduleItemReplacedEvent {

    private final String scheduleItemId;
    private final String oldPlaceId;
    private final String newPlaceId;
    private final String tripId;

    public ScheduleItemReplacedEvent(String scheduleItemId, String oldPlaceId,
                                     String newPlaceId, String tripId) {
        this.scheduleItemId = scheduleItemId;
        this.oldPlaceId = oldPlaceId;
        this.newPlaceId = newPlaceId;
        this.tripId = tripId;
    }
}
