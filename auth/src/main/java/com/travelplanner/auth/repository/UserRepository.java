package com.travelplanner.auth.repository;

import com.travelplanner.auth.domain.User;
import com.travelplanner.common.enums.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 JPA 레포지토리.
 *
 * <p>OAuth 프로바이더 + 프로바이더 ID 조합으로 사용자를 식별하며,
 * 최초 로그인 시 신규 사용자 생성에 활용된다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * 프로바이더와 프로바이더 ID로 사용자를 조회한다.
     *
     * @param provider   OAuth 프로바이더
     * @param providerId 프로바이더 고유 식별자
     * @return 조회된 사용자 (없으면 빈 Optional)
     */
    Optional<User> findByProviderAndProviderId(OAuthProvider provider, String providerId);

    /**
     * 특정 프로바이더 ID를 가진 사용자가 존재하는지 확인한다.
     *
     * @param providerId 프로바이더 고유 식별자
     * @return 존재 여부
     */
    boolean existsByProviderId(String providerId);
}
