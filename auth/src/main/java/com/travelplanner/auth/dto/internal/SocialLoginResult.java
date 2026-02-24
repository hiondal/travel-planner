package com.travelplanner.auth.dto.internal;

import com.travelplanner.auth.domain.User;
import com.travelplanner.common.security.JwtToken;
import lombok.Getter;

/**
 * 소셜 로그인 서비스 레이어 내부 결과 VO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
public class SocialLoginResult {

    /** 발급된 JWT 토큰 쌍 */
    private final JwtToken jwtToken;

    /** 로그인한 사용자 엔티티 */
    private final User user;

    /** 최초 로그인 여부 */
    private final boolean newUser;

    /**
     * SocialLoginResult 생성자.
     *
     * @param jwtToken JWT 토큰 쌍
     * @param user     사용자 엔티티
     * @param newUser  최초 로그인 여부
     */
    public SocialLoginResult(JwtToken jwtToken, User user, boolean newUser) {
        this.jwtToken = jwtToken;
        this.user = user;
        this.newUser = newUser;
    }
}
