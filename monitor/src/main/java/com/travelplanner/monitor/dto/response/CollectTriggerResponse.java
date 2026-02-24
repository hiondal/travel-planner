package com.travelplanner.monitor.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.monitor.domain.CollectionJob;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 수집 트리거 응답 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@Builder
public class CollectTriggerResponse {

    @JsonProperty("job_id")
    private String jobId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("target_count")
    private int targetCount;

    @JsonProperty("triggered_at")
    private LocalDateTime triggeredAt;

    public static CollectTriggerResponse from(CollectionJob job) {
        return CollectTriggerResponse.builder()
            .jobId(job.getJobId())
            .status(job.getStatus())
            .targetCount(job.getTargetCount())
            .triggeredAt(job.getTriggeredAt())
            .build();
    }
}
