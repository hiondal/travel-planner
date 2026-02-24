package com.travelplanner.briefing.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 브리핑 생성/조회 이력 로그 엔티티.
 */
@Entity
@Table(name = "briefing_logs",
        indexes = {
                @Index(name = "idx_briefing_logs_user_id_created_at", columnList = "userId, createdAt DESC"),
                @Index(name = "idx_briefing_logs_briefing_id", columnList = "briefingId")
        })
@Getter
@NoArgsConstructor
public class BriefingLog {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "briefing_id", length = 36)
    private String briefingId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "reason", length = 200)
    private String reason;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "read_latency_seconds")
    private Integer readLatencySeconds;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static BriefingLog created(String id, String briefingId, String userId) {
        BriefingLog log = new BriefingLog();
        log.id = id;
        log.briefingId = briefingId;
        log.userId = userId;
        log.status = "CREATED";
        log.createdAt = LocalDateTime.now();
        return log;
    }

    public static BriefingLog skipped(String id, String userId, String reason) {
        BriefingLog log = new BriefingLog();
        log.id = id;
        log.briefingId = null;
        log.userId = userId;
        log.status = "SKIPPED";
        log.reason = reason;
        log.createdAt = LocalDateTime.now();
        return log;
    }
}
