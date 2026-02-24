package com.travelplanner.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 소셜 로그인 요청 DTO.
 *
 * <p>API 설계서 AUTH-01: POST /api/v1/auth/social-login</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class SocialLoginRequest {

    /** OAuth 프로바이더 (google 또는 apple) */
    @NotBlank(message = "provider는 필수입니다.")
    @Pattern(regexp = "^(google|apple)$", message = "provider는 google 또는 apple이어야 합니다.")
    private String provider;

    /** OAuth Authorization Code */
    @NotBlank(message = "oauth_code는 필수입니다.")
    @JsonProperty("oauth_code")
    private String oauthCode;
}
