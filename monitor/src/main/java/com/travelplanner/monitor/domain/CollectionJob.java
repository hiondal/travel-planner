package com.travelplanner.monitor.domain;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 수집 작업 도메인.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
public class CollectionJob {

    private final String jobId;
    private final String status;
    private final int targetCount;
    private final LocalDateTime triggeredAt;

    public CollectionJob(String jobId, String status, int targetCount,
                         LocalDateTime triggeredAt) {
        this.jobId = jobId;
        this.status = status;
        this.targetCount = targetCount;
        this.triggeredAt = triggeredAt;
    }
}
