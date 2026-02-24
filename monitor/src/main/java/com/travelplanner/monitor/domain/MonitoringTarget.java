package com.travelplanner.monitor.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 모니터링 대상 도메인.
 *
 * <p>SCHD 서비스에서 ScheduleItemAddedEvent 수신 시 등록된다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Entity
@Table(
    name = "monitoring_targets",
    indexes = {
        @Index(name = "idx_monitoring_targets_place_id", columnList = "place_id"),
        @Index(name = "idx_monitoring_targets_visit_datetime", columnList = "visit_datetime"),
        @Index(name = "idx_monitoring_targets_user_id", columnList = "user_id"),
        @Index(name = "idx_monitoring_targets_schedule_item_id",
               columnList = "schedule_item_id", unique = true)
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonitoringTarget {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "place_id", nullable = false, length = 200)
    private String placeId;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(name = "schedule_item_id", nullable = false, unique = true, length = 36)
    private String scheduleItemId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "visit_datetime", nullable = false)
    private LocalDateTime visitDatetime;

    @Column(name = "lat", nullable = false)
    private double lat;

    @Column(name = "lng", nullable = false)
    private double lng;

    @Column(name = "category", length = 50)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 10)
    private PlaceStatusEnum currentStatus;

    @Column(name = "current_status_updated_at")
    private LocalDateTime currentStatusUpdatedAt;

    @Column(name = "consecutive_failure_count", nullable = false)
    private int consecutiveFailureCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public MonitoringTarget(String id, String placeId, String tripId,
                            String scheduleItemId, String userId,
                            LocalDateTime visitDatetime, double lat, double lng) {
        this.id = id;
        this.placeId = placeId;
        this.tripId = tripId;
        this.scheduleItemId = scheduleItemId;
        this.userId = userId;
        this.visitDatetime = visitDatetime;
        this.lat = lat;
        this.lng = lng;
        this.currentStatus = PlaceStatusEnum.GREY;
        this.consecutiveFailureCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    public void updateStatus(PlaceStatusEnum status) {
        this.currentStatus = status;
        this.currentStatusUpdatedAt = LocalDateTime.now();
    }

    public void incrementFailure() {
        this.consecutiveFailureCount++;
    }

    public void resetFailure() {
        this.consecutiveFailureCount = 0;
    }

    public boolean isWithinCollectionWindow(LocalDateTime now, int windowHours) {
        LocalDateTime windowStart = now;
        LocalDateTime windowEnd = now.plusHours(windowHours);
        return !visitDatetime.isBefore(windowStart) && !visitDatetime.isAfter(windowEnd);
    }
}
