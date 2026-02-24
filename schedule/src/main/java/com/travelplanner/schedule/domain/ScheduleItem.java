package com.travelplanner.schedule.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 일정 아이템 도메인.
 *
 * <p>여행(Trip)에 속하는 장소 방문 계획 단위이다.
 * place_name은 PLCE 서비스 변경에 독립적으로 스냅샷으로 저장한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Entity
@Table(
    name = "schedule_items",
    indexes = {
        @Index(name = "idx_schedule_items_trip_id", columnList = "trip_id"),
        @Index(name = "idx_schedule_items_trip_id_visit_datetime", columnList = "trip_id, visit_datetime"),
        @Index(name = "idx_schedule_items_trip_id_sort_order", columnList = "trip_id, sort_order"),
        @Index(name = "idx_schedule_items_place_id", columnList = "place_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleItem {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(name = "place_id", nullable = false, length = 200)
    private String placeId;

    @Column(name = "place_name", nullable = false, length = 200)
    private String placeName;

    @Column(name = "visit_datetime", nullable = false)
    private LocalDateTime visitDatetime;

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "outside_business_hours", nullable = false)
    private boolean outsideBusinessHours;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ScheduleItem(String id, String tripId, String placeId, String placeName,
                        LocalDateTime visitDatetime, String timezone, int sortOrder) {
        this.id = id;
        this.tripId = tripId;
        this.placeId = placeId;
        this.placeName = placeName;
        this.visitDatetime = visitDatetime;
        this.timezone = timezone;
        this.sortOrder = sortOrder;
        this.outsideBusinessHours = false;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
    }

    public void updatePlace(String placeId, String placeName) {
        this.placeId = placeId;
        this.placeName = placeName;
    }

    public void markOutsideBusinessHours() {
        this.outsideBusinessHours = true;
    }

    public boolean isOutsideBusinessHours() {
        return this.outsideBusinessHours;
    }
}
