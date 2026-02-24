package com.travelplanner.briefing.dto.internal;

import com.travelplanner.common.enums.BriefingType;
import lombok.Getter;

/**
 * 브리핑 생성 완료 이벤트.
 */
@Getter
public class BriefingCreatedEvent {

    private final String briefingId;
    private final String userId;
    private final String placeId;
    private final BriefingType type;

    public BriefingCreatedEvent(String briefingId, String userId, String placeId, BriefingType type) {
        this.briefingId = briefingId;
        this.userId = userId;
        this.placeId = placeId;
        this.type = type;
    }
}
