package com.travelplanner.alternative.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 대안 장소 엔티티.
 *
 * <p>추천된 대안 장소 정보 스냅샷.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Entity
@Table(name = "alternatives",
        indexes = {
                @Index(name = "idx_alternatives_user_id", columnList = "userId"),
                @Index(name = "idx_alternatives_original_place_id", columnList = "originalPlaceId"),
                @Index(name = "idx_alternatives_place_id", columnList = "placeId")
        })
@Getter
@NoArgsConstructor
public class Alternative {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "original_place_id", nullable = false, length = 200)
    private String originalPlaceId;

    @Column(name = "place_id", nullable = false, length = 200)
    private String placeId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "distance_m", nullable = false)
    private int distanceM;

    @Column(name = "rating", precision = 3, scale = 1)
    private Float rating;

    @Column(name = "congestion", length = 20)
    private String congestion;

    @Column(name = "reason", length = 200)
    private String reason;

    @Column(name = "status_label", length = 50)
    private String statusLabel;

    @Column(name = "lat", nullable = false, precision = 10, scale = 7)
    private double lat;

    @Column(name = "lng", nullable = false, precision = 10, scale = 7)
    private double lng;

    @Column(name = "walking_minutes")
    private Integer walkingMinutes;

    @Column(name = "transit_minutes")
    private Integer transitMinutes;

    @Column(name = "score", nullable = false, precision = 5, scale = 4)
    private double score;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static Alternative create(String id, String userId, String originalPlaceId,
                                      PlaceCandidate candidate, String reason, double score) {
        Alternative alt = new Alternative();
        alt.id = id;
        alt.userId = userId;
        alt.originalPlaceId = originalPlaceId;
        alt.placeId = candidate.getPlaceId();
        alt.name = candidate.getName();
        alt.distanceM = candidate.getDistanceM();
        alt.rating = candidate.getRating();
        alt.congestion = candidate.getCongestion();
        alt.reason = reason;
        alt.statusLabel = candidate.getStatusLabel();
        alt.lat = candidate.getLat();
        alt.lng = candidate.getLng();
        alt.walkingMinutes = candidate.getWalkingMinutes();
        alt.transitMinutes = candidate.getTransitMinutes();
        alt.score = score;
        alt.createdAt = LocalDateTime.now();
        return alt;
    }

    public boolean hasStatusLabel() {
        return statusLabel != null && !statusLabel.isEmpty();
    }
}
