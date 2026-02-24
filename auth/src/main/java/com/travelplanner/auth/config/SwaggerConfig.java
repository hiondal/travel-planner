package com.travelplanner.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI (Swagger UI) 설정.
 *
 * <p>AUTH 서비스 API 문서를 설정한다.
 * Swagger UI: /swagger-ui/index.html</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Configuration
public class SwaggerConfig {

    /**
     * OpenAPI 빌더를 설정한다.
     *
     * @return OpenAPI 설정 빈
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("AUTH Service API")
                .description("소셜 로그인 및 JWT 토큰 관리 서비스 API")
                .version("1.0.0"))
            .components(new Components()
                .addSecuritySchemes("BearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT Access Token (Authorization: Bearer {token})")
                ));
    }
}
