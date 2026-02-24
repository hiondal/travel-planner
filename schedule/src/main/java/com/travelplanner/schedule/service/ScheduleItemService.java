package com.travelplanner.schedule.service;

import com.travelplanner.schedule.dto.internal.ReplaceResult;
import com.travelplanner.schedule.dto.internal.ScheduleItemAddResult;
import com.travelplanner.schedule.dto.request.AddScheduleItemRequest;

/**
 * 일정 아이템 서비스 인터페이스.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
public interface ScheduleItemService {

    ScheduleItemAddResult addScheduleItem(String tripId, String userId, AddScheduleItemRequest request);

    void deleteScheduleItem(String tripId, String itemId, String userId);

    ReplaceResult replaceScheduleItem(String tripId, String itemId, String newPlaceId, String userId);
}
