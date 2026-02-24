package com.travelplanner.schedule.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 일정 장소 교체 요청 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class ReplaceScheduleItemRequest {

    @NotBlank(message = "교체할 장소 ID는 필수입니다.")
    @JsonProperty("new_place_id")
    private String newPlaceId;
}
