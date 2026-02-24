package com.travelplanner.schedule.domain;

import com.travelplanner.common.enums.TripStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 여행 일정 도메인.
 *
 * <p>사용자가 생성한 여행의 헤더 정보(여행명, 기간, 도시, 상태)를 보관한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Entity
@Table(
    name = "trips",
    indexes = {
        @Index(name = "idx_trips_user_id", columnList = "user_id"),
        @Index(name = "idx_trips_user_id_status", columnList = "user_id, status"),
        @Index(name = "idx_trips_start_date_end_date", columnList = "start_date, end_date")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trip {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TripStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "tripId", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("visitDatetime ASC")
    private List<ScheduleItem> scheduleItems = new ArrayList<>();

    public Trip(String id, String userId, String name,
                LocalDate startDate, LocalDate endDate, String city) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.city = city;
        this.status = TripStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = TripStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = TripStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return TripStatus.ACTIVE.equals(this.status);
    }

    public int getDurationDays() {
        return (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
