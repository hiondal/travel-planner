package com.travelplanner.schedule.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.travelplanner.schedule.dto.request.deserializer.FlexibleLocalDateTimeDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 일정 장소 추가 요청 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class AddScheduleItemRequest {

    @NotBlank(message = "장소 ID는 필수입니다.")
    @JsonProperty("place_id")
    private String placeId;

    @NotNull(message = "방문 일시는 필수입니다.")
    @JsonProperty("visit_datetime")
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime visitDatetime;

    @NotBlank(message = "타임존은 필수입니다.")
    @JsonProperty("timezone")
    private String timezone;

    @JsonProperty("force")
    private boolean force = false;
}
