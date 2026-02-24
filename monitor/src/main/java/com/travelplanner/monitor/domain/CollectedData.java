package com.travelplanner.monitor.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 외부 API 수집 원본 데이터 도메인.
 *
 * <p>판정 근거 보존 및 폴백 캐시 역할.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Entity
@Table(
    name = "collected_data",
    indexes = {
        @Index(name = "idx_collected_data_place_id_collected_at",
               columnList = "place_id, collected_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectedData {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "place_id", nullable = false, length = 200)
    private String placeId;

    @Column(name = "business_status", length = 20)
    private String businessStatus;

    @Column(name = "precipitation_prob")
    private Integer precipitationProb;

    @Column(name = "weather_condition", length = 50)
    private String weatherCondition;

    @Column(name = "walking_minutes")
    private Integer walkingMinutes;

    @Column(name = "transit_minutes")
    private Integer transitMinutes;

    @Column(name = "distance_m")
    private Integer distanceM;

    @Column(name = "congestion_level", length = 20)
    private String congestionLevel;

    @Column(name = "has_fallback", nullable = false)
    private boolean hasFallback;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    public CollectedData(String id, String placeId, LocalDateTime collectedAt) {
        this.id = id;
        this.placeId = placeId;
        this.hasFallback = false;
        this.collectedAt = collectedAt;
    }

    public boolean isComplete() {
        return businessStatus != null && precipitationProb != null && walkingMinutes != null;
    }
}
