package com.skicoach.backend.config;

import com.skicoach.backend.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 *
 * 包含:
 * - CORS 跨域
 * - JWT 拦截器(用户端 /api/**)
 * - 管理端拦截器(P2.5 添加)
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

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

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 用户端 JWT 拦截器
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")             // 拦截所有用户接口
                .excludePathPatterns(
                        "/api/auth/register",           // 注册不需要鉴权
                        "/api/auth/login"               // 登录不需要鉴权
                );

        // P2.5 阶段会在这里添加管理端 AdminAuthInterceptor
    }
}
