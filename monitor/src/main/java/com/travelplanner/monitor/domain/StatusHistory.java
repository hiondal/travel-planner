package com.travelplanner.monitor.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 상태 판정 이력 도메인.
 *
 * <p>append-only. 6개월+ 보존.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Entity
@Table(
    name = "status_history",
    indexes = {
        @Index(name = "idx_status_history_place_id_judgment_at",
               columnList = "place_id, judgment_at"),
        @Index(name = "idx_status_history_schedule_item_id", columnList = "schedule_item_id"),
        @Index(name = "idx_status_history_judgment_at", columnList = "judgment_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StatusHistory {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "place_id", nullable = false, length = 200)
    private String placeId;

    @Column(name = "schedule_item_id", nullable = false, length = 36)
    private String scheduleItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private PlaceStatusEnum status;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "judgment_at", nullable = false)
    private LocalDateTime judgmentAt;

    @Column(name = "confidence_score", precision = 4, scale = 3)
    private Double confidenceScore;

    public StatusHistory(String id, String placeId, String scheduleItemId,
                         PlaceStatusEnum status, String reason, LocalDateTime judgmentAt) {
        this.id = id;
        this.placeId = placeId;
        this.scheduleItemId = scheduleItemId;
        this.status = status;
        this.reason = reason;
        this.judgmentAt = judgmentAt;
    }
}
