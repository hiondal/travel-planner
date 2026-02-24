package com.travelplanner.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.auth.domain.User;
import lombok.Builder;
import lombok.Getter;

/**
 * 사용자 프로파일 응답 DTO.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@Builder
public class UserProfileDto {

    /** 사용자 ID */
    @JsonProperty("user_id")
    private String userId;

    /** 닉네임 */
    private String nickname;

    /** 프로필 이미지 URL */
    @JsonProperty("avatar_url")
    private String avatarUrl;

    /** 구독 등급 */
    private String tier;

    /** 최초 로그인 여부 */
    @JsonProperty("is_new_user")
    private boolean isNewUser;

    /**
     * User 엔티티와 신규 사용자 여부로 DTO를 생성한다.
     *
     * @param user      사용자 엔티티
     * @param isNewUser 신규 사용자 여부
     * @return UserProfileDto 인스턴스
     */
    public static UserProfileDto from(User user, boolean isNewUser) {
        return UserProfileDto.builder()
            .userId(user.getId())
            .nickname(user.getNickname())
            .avatarUrl(user.getAvatarUrl())
            .tier(user.getTier().name())
            .isNewUser(isNewUser)
            .build();
    }
}
