package com.travelplanner.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 토큰 즉시 무효화 및 재발급 응답 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@Builder
public class TokenInvalidateResponse {

    /** 새로 발급된 JWT Access Token */
    @JsonProperty("access_token")
    private String accessToken;

    /** 변경된 구독 등급 */
    private String tier;

    /**
     * Access Token과 구독 등급으로 응답 DTO를 생성한다.
     *
     * @param accessToken 새 Access Token
     * @param tier        변경된 구독 등급
     * @return TokenInvalidateResponse 인스턴스
     */
    public static TokenInvalidateResponse of(String accessToken, String tier) {
        return TokenInvalidateResponse.builder()
            .accessToken(accessToken)
            .tier(tier)
            .build();
    }
}
