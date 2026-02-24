package com.travelplanner.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.auth.domain.User;
import com.travelplanner.common.security.JwtToken;
import lombok.Builder;
import lombok.Getter;

/**
 * 소셜 로그인 응답 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@Builder
public class SocialLoginResponse {

    /** JWT Access Token (30분 만료) */
    @JsonProperty("access_token")
    private String accessToken;

    /** Refresh Token (30일 만료) */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /** 사용자 프로파일 */
    @JsonProperty("user_profile")
    private UserProfileDto userProfile;

    /**
     * JwtToken과 User 엔티티로 응답 DTO를 생성한다.
     *
     * @param token     JWT 토큰 쌍
     * @param user      사용자 엔티티
     * @param isNewUser 신규 사용자 여부
     * @return SocialLoginResponse 인스턴스
     */
    public static SocialLoginResponse of(JwtToken token, User user, boolean isNewUser) {
        return SocialLoginResponse.builder()
            .accessToken(token.getAccessToken())
            .refreshToken(token.getRefreshToken())
            .userProfile(UserProfileDto.from(user, isNewUser))
            .build();
    }
}
