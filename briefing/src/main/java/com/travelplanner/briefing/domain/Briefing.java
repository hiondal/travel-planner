package com.travelplanner.briefing.domain;

import com.travelplanner.common.enums.BriefingType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 브리핑 엔티티.
 *
 * <p>출발 전 브리핑(안심/주의) 정보를 담는 도메인 객체.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Entity
@Table(name = "briefings",
        indexes = {
                @Index(name = "idx_briefings_user_id_created_at", columnList = "userId, createdAt DESC"),
                @Index(name = "idx_briefings_schedule_item_id", columnList = "scheduleItemId")
        })
@Getter
@NoArgsConstructor
public class Briefing {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "schedule_item_id", nullable = false, length = 36)
    private String scheduleItemId;

    @Column(name = "place_id", nullable = false, length = 200)
    private String placeId;

    @Column(name = "place_name", nullable = false, length = 200)
    private String placeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private BriefingType type;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 200)
    private String idempotencyKey;

    @Column(name = "summary_text", nullable = false, columnDefinition = "TEXT")
    private String summaryText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_level", nullable = false, length = 10)
    private StatusLevel statusLevel;

    @Column(name = "business_status", length = 50)
    private String businessStatus;

    @Column(name = "congestion", length = 50)
    private String congestion;

    @Column(name = "weather", length = 100)
    private String weather;

    @Column(name = "walking_minutes")
    private Integer walkingMinutes;

    @Column(name = "transit_minutes")
    private Integer transitMinutes;

    @Column(name = "distance_m")
    private Integer distanceM;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "risk_items", columnDefinition = "jsonb")
    private List<RiskItem> riskItems = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static Briefing create(String id, String userId, String scheduleItemId,
                                   String placeId, String placeName, BriefingType type,
                                   LocalDateTime departureTime, String idempotencyKey,
                                   String summaryText, StatusLevel statusLevel,
                                   BriefingContent content, List<RiskItem> riskItems) {
        Briefing briefing = new Briefing();
        briefing.id = id;
        briefing.userId = userId;
        briefing.scheduleItemId = scheduleItemId;
        briefing.placeId = placeId;
        briefing.placeName = placeName;
        briefing.type = type;
        briefing.departureTime = departureTime;
        briefing.idempotencyKey = idempotencyKey;
        briefing.summaryText = summaryText;
        briefing.statusLevel = statusLevel;
        briefing.businessStatus = content.getBusinessStatus();
        briefing.congestion = content.getCongestion();
        briefing.weather = content.getWeather();
        briefing.walkingMinutes = content.getWalkingMinutes();
        briefing.transitMinutes = content.getTransitMinutes();
        briefing.distanceM = content.getDistanceM();
        briefing.riskItems = riskItems != null ? riskItems : new ArrayList<>();
        briefing.createdAt = LocalDateTime.now();
        return briefing;
    }

    /**
     * 브리핑 만료 여부를 판단한다.
     *
     * @return 현재 시각이 departureTime 이후인 경우 true
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(departureTime);
    }

    /**
     * 안전 브리핑 여부를 반환한다.
     *
     * @return SAFE 타입인 경우 true
     */
    public boolean isSafe() {
        return type == BriefingType.SAFE;
    }

    /**
     * 대안 장소 링크를 반환한다.
     *
     * @return WARNING 타입인 경우 대안 링크, 그 외 null
     */
    public String getAlternativeLink() {
        if (type == BriefingType.WARNING) {
            return "/alternatives?place_id=" + placeId;
        }
        return null;
    }

    public BriefingContent getContent() {
        return new BriefingContent(businessStatus, congestion, weather,
                walkingMinutes, transitMinutes, distanceM);
    }
}
