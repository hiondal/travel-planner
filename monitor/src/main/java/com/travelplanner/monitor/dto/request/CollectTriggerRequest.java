package com.travelplanner.monitor.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 데이터 수집 트리거 요청 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class CollectTriggerRequest {

    @JsonProperty("triggered_by")
    private String triggeredBy;

    @JsonProperty("triggered_at")
    private LocalDateTime triggeredAt;
}
