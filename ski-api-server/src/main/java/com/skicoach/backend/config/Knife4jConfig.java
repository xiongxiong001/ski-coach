package com.skicoach.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j (基于 OpenAPI 3) 文档配置
 *
 * 访问地址:
 *   - http://localhost:8080/doc.html  (Knife4j增强UI,推荐)
 *   - http://localhost:8080/swagger-ui/index.html  (原生Swagger UI)
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ski Coach API")
                        .version("1.0.0")
                        .description("Ski Coach 主业务API服务接口文档")
                        .contact(new Contact().name("ski-coach team"))
                        .license(new License().name("MIT")))
                // 全局Authorization头(JWT)
                .components(new Components()
                        .addSecuritySchemes("Authorization",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Bearer Token,格式: Bearer xxxxx")))
                .addSecurityItem(new SecurityRequirement().addList("Authorization"));
    }

    /** 用户端API分组 */
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("用户端 API")
                .pathsToMatch("/api/**", "/health")
                .build();
    }

    /** 管理端API分组 */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("管理端 API")
                .pathsToMatch("/admin/**")
                .build();
    }
}
