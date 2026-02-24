package com.travelplanner.monitor.dto.internal;

import com.travelplanner.monitor.domain.PlaceStatusEnum;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 장소 상태 변경 이벤트.
 *
 * <p>BRIF 서비스가 구독하여 브리핑 생성 트리거로 활용한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
public class PlaceStatusChangedEvent {

    private final String placeId;
    private final PlaceStatusEnum prevStatus;
    private final PlaceStatusEnum newStatus;
    private final LocalDateTime changedAt;

    public PlaceStatusChangedEvent(String placeId, PlaceStatusEnum prevStatus,
                                   PlaceStatusEnum newStatus, LocalDateTime changedAt) {
        this.placeId = placeId;
        this.prevStatus = prevStatus;
        this.newStatus = newStatus;
        this.changedAt = changedAt;
    }
}
