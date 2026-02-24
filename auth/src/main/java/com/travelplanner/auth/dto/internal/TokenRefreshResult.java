package com.travelplanner.auth.dto.internal;

import lombok.Getter;

/**
 * 토큰 갱신 서비스 레이어 내부 결과 VO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
public class TokenRefreshResult {

    /** 새로 발급된 Access Token */
    private final String accessToken;

    /** 만료까지 남은 초 */
    private final int expiresIn;

    /**
     * TokenRefreshResult 생성자.
     *
     * @param accessToken 새 Access Token
     * @param expiresIn   만료까지 남은 초
     */
    public TokenRefreshResult(String accessToken, int expiresIn) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
    }
}
