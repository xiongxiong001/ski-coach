package com.skicoach.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 *
 * P2.1 阶段:只配置 CORS
 * P2.2 阶段:会注册 JwtInterceptor / AdminAuthInterceptor
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * CORS 跨域配置
     * 开发环境允许所有源,生产环境通过Nginx统一处理
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Disposition")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /*
     * P2.2 阶段会在这里注册:
     *
     * @Override
     * public void addInterceptors(InterceptorRegistry registry) {
     *     registry.addInterceptor(jwtInterceptor)
     *             .addPathPatterns("/api/**")
     *             .excludePathPatterns("/api/auth/**");
     *
     *     registry.addInterceptor(adminAuthInterceptor)
     *             .addPathPatterns("/admin/**")
     *             .excludePathPatterns("/admin/auth/login");
     * }
     */
}
