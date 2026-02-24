package com.travelplanner.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * Access Token 갱신 응답 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@Builder
public class TokenRefreshResponse {

    /** 새로 발급된 JWT Access Token */
    @JsonProperty("access_token")
    private String accessToken;

    /** 만료까지 남은 초 */
    @JsonProperty("expires_in")
    private int expiresIn;

    /**
     * Access Token과 만료 시간으로 응답 DTO를 생성한다.
     *
     * @param accessToken 새 Access Token
     * @param expiresIn   만료까지 남은 초
     * @return TokenRefreshResponse 인스턴스
     */
    public static TokenRefreshResponse of(String accessToken, int expiresIn) {
        return TokenRefreshResponse.builder()
            .accessToken(accessToken)
            .expiresIn(expiresIn)
            .build();
    }
}
