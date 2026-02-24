package com.travelplanner.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * JWT 인증 실패 처리 EntryPoint (공용).
 *
 * <p>인증이 필요한 리소스에 인증 없이 접근하거나 유효하지 않은 JWT 토큰으로
 * 접근하는 경우 401 Unauthorized 응답을 반환한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JwtAuthenticationEntryPoint() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");

        Map<String, Object> body = Map.of(
            "error", "UNAUTHORIZED",
            "message", "인증이 필요합니다. 유효한 Access Token을 제공해주세요.",
            "timestamp", LocalDateTime.now().toString()
        );

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
