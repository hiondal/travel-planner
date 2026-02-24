package com.travelplanner.schedule.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 여행 생성 요청 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class CreateTripRequest {

    @NotBlank(message = "여행명은 필수입니다.")
    @Size(min = 1, max = 50, message = "여행명은 1~50자 사이여야 합니다.")
    @JsonProperty("name")
    private String name;

    @NotNull(message = "여행 시작일은 필수입니다.")
    @JsonProperty("start_date")
    private LocalDate startDate;

    @NotNull(message = "여행 종료일은 필수입니다.")
    @JsonProperty("end_date")
    private LocalDate endDate;

    @NotBlank(message = "여행 도시는 필수입니다.")
    @JsonProperty("city")
    private String city;
}
