package com.travelplanner.monitor.dto.internal;

import com.travelplanner.monitor.domain.BusinessStatusData;
import com.travelplanner.monitor.domain.PlaceStatusEnum;
import com.travelplanner.monitor.domain.TravelTimeData;
import com.travelplanner.monitor.domain.WeatherData;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 장소 상태 상세 내부 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@Builder
public class StatusDetail {

    private final String placeId;
    private final String placeName;
    private final PlaceStatusEnum overallStatus;
    private final BusinessStatusData businessStatus;
    private final WeatherData weatherData;
    private final TravelTimeData travelTimeData;
    private final String congestionValue;
    private final boolean congestionUnknown;
    private final String reason;
    private final boolean showAlternativeButton;
    private final LocalDateTime updatedAt;
}
