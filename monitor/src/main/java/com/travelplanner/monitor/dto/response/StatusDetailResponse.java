package com.travelplanner.monitor.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.monitor.dto.internal.StatusDetail;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 장소 상태 상세 응답 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@Builder
public class StatusDetailResponse {

    @JsonProperty("place_id")
    private String placeId;

    @JsonProperty("place_name")
    private String placeName;

    @JsonProperty("overall_status")
    private String overallStatus;

    @JsonProperty("details")
    private StatusDetailsDto details;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("show_alternative_button")
    private boolean showAlternativeButton;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public static StatusDetailResponse from(StatusDetail detail) {
        StatusDetailsDto.BusinessStatusItemDto businessStatusDto =
            StatusDetailsDto.BusinessStatusItemDto.builder()
                .status(resolveItemStatus(detail.getBusinessStatus() != null
                    ? detail.getBusinessStatus().getStatus() : "UNKNOWN"))
                .value(detail.getBusinessStatus() != null
                    ? detail.getBusinessStatus().getStatus() : "UNKNOWN")
                .build();

        StatusDetailsDto.WeatherItemDto weatherDto =
            StatusDetailsDto.WeatherItemDto.builder()
                .status(detail.getWeatherData() != null
                    ? resolveWeatherStatus(detail.getWeatherData().getPrecipitationProb())
                    : "NORMAL")
                .value(detail.getWeatherData() != null
                    ? detail.getWeatherData().getCondition() : "UNKNOWN")
                .precipitationProb(detail.getWeatherData() != null
                    ? detail.getWeatherData().getPrecipitationProb() : 0)
                .build();

        StatusDetailsDto.CongestionItemDto congestionDto =
            StatusDetailsDto.CongestionItemDto.builder()
                .status("NORMAL")
                .value(detail.getCongestionValue() != null
                    ? detail.getCongestionValue() : "정보 없음")
                .isUnknown(detail.isCongestionUnknown())
                .build();

        StatusDetailsDto.TravelTimeItemDto travelTimeDto =
            StatusDetailsDto.TravelTimeItemDto.builder()
                .status("NORMAL")
                .walkingMinutes(detail.getTravelTimeData() != null
                    ? detail.getTravelTimeData().getWalkingMinutes() : 0)
                .transitMinutes(detail.getTravelTimeData() != null
                    ? detail.getTravelTimeData().getTransitMinutes() : null)
                .distanceM(detail.getTravelTimeData() != null
                    ? detail.getTravelTimeData().getDistanceM() : 0)
                .build();

        StatusDetailsDto detailsDto = StatusDetailsDto.builder()
            .businessStatus(businessStatusDto)
            .congestion(congestionDto)
            .weather(weatherDto)
            .travelTime(travelTimeDto)
            .build();

        return StatusDetailResponse.builder()
            .placeId(detail.getPlaceId())
            .placeName(detail.getPlaceName())
            .overallStatus(detail.getOverallStatus().name())
            .details(detailsDto)
            .reason(detail.getReason())
            .showAlternativeButton(detail.isShowAlternativeButton())
            .updatedAt(detail.getUpdatedAt())
            .build();
    }

    private static String resolveItemStatus(String businessStatus) {
        if ("CLOSED_PERMANENTLY".equals(businessStatus) || "CLOSED_TEMPORARILY".equals(businessStatus)) {
            return "DANGER";
        }
        return "NORMAL";
    }

    private static String resolveWeatherStatus(int precipitationProb) {
        if (precipitationProb >= 70) return "DANGER";
        if (precipitationProb >= 40) return "WARNING";
        return "NORMAL";
    }
}
