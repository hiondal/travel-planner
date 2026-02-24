package com.travelplanner.auth.dto.internal;

import lombok.Getter;

/**
 * 토큰 무효화 및 재발급 서비스 레이어 내부 결과 VO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
public class TokenInvalidateResult {

    /** 새로 발급된 Access Token */
    private final String accessToken;

    /** 변경된 구독 등급 */
    private final String tier;

    /**
     * TokenInvalidateResult 생성자.
     *
     * @param accessToken 새 Access Token
     * @param tier        변경된 구독 등급
     */
    public TokenInvalidateResult(String accessToken, String tier) {
        this.accessToken = accessToken;
        this.tier = tier;
    }
}
