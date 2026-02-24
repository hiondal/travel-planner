package com.travelplanner.auth.domain;

import com.travelplanner.common.domain.BaseTimeEntity;
import com.travelplanner.common.enums.OAuthProvider;
import com.travelplanner.common.enums.SubscriptionTier;
import com.travelplanner.common.security.UserPrincipal;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 도메인 엔티티.
 *
 * <p>OAuth 프로바이더와 프로바이더 고유 ID로 사용자를 식별한다.
 * 최초 소셜 로그인 시 자동 생성되며, 닉네임은 OAuth 프로파일의 name을 사용한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_provider_provider_id", columnList = "provider, provider_id", unique = true),
        @Index(name = "idx_users_email", columnList = "email")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    /** OAuth 프로바이더 (GOOGLE, APPLE) */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private OAuthProvider provider;

    /** 프로바이더 고유 식별자 */
    @Column(name = "provider_id", nullable = false, length = 200, unique = true)
    private String providerId;

    /** 이메일 주소 */
    @Column(name = "email", nullable = false, length = 200)
    private String email;

    /** 닉네임 (OAuth 프로파일에서 자동 설정) */
    @Column(name = "nickname", nullable = false, length = 100)
    private String nickname;

    /** 프로필 이미지 URL */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /** 구독 등급 (기본값: FREE) */
    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 20)
    private SubscriptionTier tier = SubscriptionTier.FREE;

    /**
     * 신규 사용자 생성 팩토리 메서드.
     *
     * @param provider   OAuth 프로바이더
     * @param providerId 프로바이더 고유 식별자
     * @param email      이메일
     * @param nickname   닉네임
     * @param avatarUrl  프로필 이미지 URL
     * @return 생성된 User 엔티티
     */
    public static User create(OAuthProvider provider, String providerId,
                              String email, String nickname, String avatarUrl) {
        User user = new User();
        user.provider = provider;
        user.providerId = providerId;
        user.email = email;
        user.nickname = nickname;
        user.avatarUrl = avatarUrl;
        user.tier = SubscriptionTier.FREE;
        return user;
    }

    /**
     * 구독 등급을 변경한다.
     *
     * @param newTier 새 구독 등급
     */
    public void updateTier(SubscriptionTier newTier) {
        this.tier = newTier;
    }

    /**
     * 프로필 정보를 업데이트한다 (닉네임, 아바타 URL).
     *
     * @param nickname  새 닉네임
     * @param avatarUrl 새 프로필 이미지 URL
     */
    public void updateProfile(String nickname, String avatarUrl) {
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
    }

    /**
     * Spring Security에서 사용하는 UserPrincipal로 변환한다.
     *
     * @return UserPrincipal 인스턴스
     */
    public UserPrincipal toUserPrincipal() {
        return new UserPrincipal(this.id, this.email, this.tier);
    }

    @Override
    protected String getPrefix() {
        return "usr";
    }
}
