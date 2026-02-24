package com.travelplanner.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그아웃 요청 DTO.
 *
 * <p>API 설계서 AUTH-03: POST /api/v1/auth/logout</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class LogoutRequest {

    /** 무효화할 리프레시 토큰 */
    @NotBlank(message = "refresh_token은 필수입니다.")
    @JsonProperty("refresh_token")
    private String refreshToken;
}
