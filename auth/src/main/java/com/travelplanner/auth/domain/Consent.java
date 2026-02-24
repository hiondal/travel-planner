package com.travelplanner.auth.domain;

import com.travelplanner.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 동의 이력 엔티티.
 *
 * <p>위치정보 수집 및 Push 알림 권한 동의 결과를 이력(append-only) 방식으로 저장한다.
 * 최신 동의 정보는 {@code findLatestByUserId}로 조회한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Entity
@Table(
    name = "consents",
    indexes = {
        @Index(name = "idx_consents_user_id_created_at", columnList = "user_id, created_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Consent extends BaseEntity {

    /** 사용자 ID (users.id 참조) */
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    /** 위치정보 수집 동의 여부 */
    @Column(name = "location", nullable = false)
    private boolean location;

    /** Push 알림 동의 여부 */
    @Column(name = "push", nullable = false)
    private boolean push;

    /** 동의 일시 (사용자 입력값) */
    @Column(name = "consented_at", nullable = false)
    private LocalDateTime consentedAt;

    /** 레코드 생성 일시 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 동의 레코드 생성 팩토리 메서드.
     *
     * @param userId      사용자 ID
     * @param location    위치정보 동의 여부
     * @param push        Push 알림 동의 여부
     * @param consentedAt 동의 일시
     * @return 생성된 Consent 엔티티
     */
    public static Consent create(String userId, boolean location, boolean push, LocalDateTime consentedAt) {
        Consent consent = new Consent();
        consent.userId = userId;
        consent.location = location;
        consent.push = push;
        consent.consentedAt = consentedAt;
        consent.createdAt = LocalDateTime.now();
        return consent;
    }

    /**
     * 위치정보 수집이 허용되었는지 확인한다.
     *
     * @return 위치정보 동의 여부
     */
    public boolean isLocationGranted() {
        return this.location;
    }

    /**
     * Push 알림이 허용되었는지 확인한다.
     *
     * @return Push 알림 동의 여부
     */
    public boolean isPushGranted() {
        return this.push;
    }

    @Override
    protected String getPrefix() {
        return "cns";
    }
}
