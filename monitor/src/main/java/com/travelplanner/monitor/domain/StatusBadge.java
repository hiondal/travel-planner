package com.travelplanner.monitor.domain;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 상태 배지 도메인.
 *
 * <p>색상+아이콘 조합으로 색약 사용자를 지원한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
public class StatusBadge {

    private final String placeId;
    private final PlaceStatusEnum status;
    private final BadgeIcon icon;
    private final String label;
    private final String colorHex;
    private final LocalDateTime updatedAt;

    public StatusBadge(String placeId, PlaceStatusEnum status, LocalDateTime updatedAt) {
        this.placeId = placeId;
        this.status = status;
        this.icon = resolveIcon(status);
        this.label = resolveLabel(status);
        this.colorHex = resolveColorHex(status);
        this.updatedAt = updatedAt;
    }

    public boolean hasAlert() {
        return status.isAlert();
    }

    public boolean shouldShowAlternativeButton() {
        return status == PlaceStatusEnum.YELLOW || status == PlaceStatusEnum.RED;
    }

    private BadgeIcon resolveIcon(PlaceStatusEnum status) {
        return switch (status) {
            case GREEN -> BadgeIcon.CHECK;
            case YELLOW -> BadgeIcon.EXCLAMATION;
            case RED -> BadgeIcon.X;
            case GREY -> BadgeIcon.QUESTION;
        };
    }

    private String resolveLabel(PlaceStatusEnum status) {
        return switch (status) {
            case GREY -> "데이터 미확인";
            default -> null;
        };
    }

    private String resolveColorHex(PlaceStatusEnum status) {
        return switch (status) {
            case GREEN -> "#4CAF50";
            case YELLOW -> "#FFC107";
            case RED -> "#F44336";
            case GREY -> "#9E9E9E";
        };
    }
}
