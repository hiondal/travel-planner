package com.travelplanner.auth.config;

import com.travelplanner.auth.config.jwt.JwtAuthenticationEntryPoint;
import com.travelplanner.auth.config.jwt.JwtAuthenticationFilter;
import com.travelplanner.auth.repository.AuthSessionRedisRepository;
import com.travelplanner.common.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정.
 *
 * <p>JWT 기반 Stateless 인증을 사용한다. OAuth2 소셜 로그인 엔드포인트와
 * 토큰 갱신 엔드포인트는 인증을 요구하지 않는다.</p>
 *
 * <p>인증 흐름:</p>
 * <ol>
 *   <li>클라이언트 → POST /api/v1/auth/social-login (Google OAuth Code 전달)</li>
 *   <li>서버 → Google API 토큰 교환 → JWT Access Token + Refresh Token 발급</li>
 *   <li>이후 요청 → Authorization: Bearer {access_token} 헤더 포함</li>
 * </ol>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final AuthSessionRedisRepository authSessionRedisRepository;

    /**
     * Spring Security 필터 체인을 설정한다.
     *
     * @param http HttpSecurity 빌더
     * @return SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            // CSRF 비활성화 (Stateless JWT 사용)
            .csrf(AbstractHttpConfigurer::disable)
            // Stateless 세션 관리
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 인증 실패 처리
            .exceptionHandling(ex ->
                ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            // 엔드포인트별 접근 권한 설정
            .authorizeHttpRequests(auth -> auth
                // 인증 불필요 엔드포인트
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/social-login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/token/refresh").permitAll()
                // Swagger UI / OpenAPI 문서
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/api-docs/**"
                ).permitAll()
                // Actuator 헬스체크
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // 나머지 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )
            // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * JWT 인증 필터 빈을 생성한다.
     *
     * @return JwtAuthenticationFilter 인스턴스
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtProvider, authSessionRedisRepository);
    }
}
