package com.travelplanner.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토큰 즉시 무효화 및 재발급 요청 DTO.
 *
 * <p>API 설계서 AUTH-04: POST /api/v1/auth/token/invalidate
 * PAY 서비스에서 구독 티어 변경 후 내부 호출한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class TokenInvalidateRequest {

    /** 대상 사용자 ID */
    @NotBlank(message = "user_id는 필수입니다.")
    @JsonProperty("user_id")
    private String userId;

    /** 변경될 구독 등급 */
    @NotBlank(message = "new_tier는 필수입니다.")
    @Pattern(regexp = "^(FREE|TRIP_PASS|PRO)$", message = "new_tier는 FREE, TRIP_PASS, PRO 중 하나여야 합니다.")
    @JsonProperty("new_tier")
    private String newTier;
}
