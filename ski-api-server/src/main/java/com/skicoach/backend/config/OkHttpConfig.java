package com.skicoach.backend.config;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * OkHttp 客户端配置
 *
 * 主要用于调用 ski-ai-server。
 */
@Configuration
public class OkHttpConfig {

    @Value("${ski.ai.analyze-timeout-ms:600000}")
    private long analyzeTimeoutMs;

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(analyzeTimeoutMs, TimeUnit.MILLISECONDS)   // AI接口可能需要数分钟
                .writeTimeout(60, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(5, 60, TimeUnit.SECONDS))
                .retryOnConnectionFailure(true)
                .build();
    }
}
