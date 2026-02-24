package com.travelplanner.briefing.config;

import com.travelplanner.common.security.JwtAuthenticationEntryPoint;
import com.travelplanner.common.security.JwtAuthenticationFilter;
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
 * BRIEFING 서비스 Security 설정.
 *
 * <p>JWT 기반 Stateless 인증을 사용한다.
 * 브리핑 생성 트리거는 내부 스케줄러 호출이므로 인증 불필요.
 * 브리핑 조회/목록 조회는 인증 필요.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex ->
                ex.authenticationEntryPoint(new JwtAuthenticationEntryPoint()))
            .authorizeHttpRequests(auth -> auth
                // 브리핑 생성 트리거는 내부 스케줄러 호출 (인증 불필요)
                .requestMatchers(HttpMethod.POST, "/api/v1/briefings/generate").permitAll()
                // Swagger / Actuator
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                    "/v3/api-docs/**", "/api-docs/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // 나머지 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtProvider),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
