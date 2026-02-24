package com.travelplanner.auth.client;

import com.travelplanner.common.enums.OAuthProvider;
import lombok.Getter;

/**
 * OAuth 사용자 프로파일 VO.
 *
 * <p>Google OAuth2 토큰 교환 후 반환되는 사용자 기본 정보를 담는다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
public class OAuthProfile {

    /** 프로바이더 고유 식별자 */
    private final String providerId;

    /** 이메일 주소 */
    private final String email;

    /** 표시 이름 (닉네임으로 사용) */
    private final String name;

    /** 프로필 이미지 URL */
    private final String avatarUrl;

    /** OAuth 프로바이더 종류 */
    private final OAuthProvider provider;

    /**
     * OAuthProfile 생성자.
     *
     * @param providerId 프로바이더 고유 식별자
     * @param email      이메일 주소
     * @param name       표시 이름
     * @param avatarUrl  프로필 이미지 URL
     * @param provider   OAuth 프로바이더
     */
    public OAuthProfile(String providerId, String email, String name,
                        String avatarUrl, OAuthProvider provider) {
        this.providerId = providerId;
        this.email = email;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.provider = provider;
    }
}
