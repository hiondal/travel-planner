package com.travelplanner.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.auth.domain.Consent;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자 동의 저장 응답 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@Builder
public class ConsentResponse {

    /** 생성된 동의 레코드 ID */
    @JsonProperty("consent_id")
    private String consentId;

    /** 위치정보 수집 동의 여부 */
    private boolean location;

    /** Push 알림 동의 여부 */
    private boolean push;

    /** 동의 일시 */
    @JsonProperty("consented_at")
    private LocalDateTime consentedAt;

    /**
     * Consent 엔티티로 응답 DTO를 생성한다.
     *
     * @param consent 동의 엔티티
     * @return ConsentResponse 인스턴스
     */
    public static ConsentResponse from(Consent consent) {
        return ConsentResponse.builder()
            .consentId(consent.getId())
            .location(consent.isLocation())
            .push(consent.isPush())
            .consentedAt(consent.getConsentedAt())
            .build();
    }
}
