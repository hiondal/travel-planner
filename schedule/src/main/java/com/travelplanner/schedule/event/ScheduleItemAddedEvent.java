package com.travelplanner.schedule.event;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 일정 장소 추가 이벤트.
 *
 * <p>MNTR 서비스가 모니터링 대상을 등록하기 위해 구독한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
public class ScheduleItemAddedEvent {

    private final String scheduleItemId;
    private final String placeId;
    private final String placeName;
    private final LocalDateTime visitDatetime;
    private final String timezone;
    private final String tripId;
    private final String userId;

    public ScheduleItemAddedEvent(String scheduleItemId, String placeId, String placeName,
                                  LocalDateTime visitDatetime, String timezone,
                                  String tripId, String userId) {
        this.scheduleItemId = scheduleItemId;
        this.placeId = placeId;
        this.placeName = placeName;
        this.visitDatetime = visitDatetime;
        this.timezone = timezone;
        this.tripId = tripId;
        this.userId = userId;
    }
}
