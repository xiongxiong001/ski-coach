package com.skicoach.backend.controller;

import com.skicoach.backend.common.result.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查接口
 *
 * 供运维探活、负载均衡、监控用。
 * 同时验证 MySQL/Redis 连接是否正常。
 */
@Tag(name = "健康检查", description = "服务可用性探针")
@Slf4j
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${spring.application.name}")
    private String appName;

    @Operation(summary = "健康检查", description = "返回服务和依赖组件的健康状态")
    @GetMapping("/health")
    public ApiResult<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("service", appName);
        result.put("status", "ok");
        result.put("timestamp", LocalDateTime.now());

        // 检查 MySQL
        Map<String, Object> mysqlStatus = checkMysql();
        result.put("mysql", mysqlStatus);

        // 检查 Redis
        Map<String, Object> redisStatus = checkRedis();
        result.put("redis", redisStatus);

        return ApiResult.success(result);
    }

    private Map<String, Object> checkMysql() {
        Map<String, Object> status = new HashMap<>();
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            status.put("status", "ok");
            status.put("queryResult", one);
        } catch (Exception e) {
            log.error("MySQL健康检查失败", e);
            status.put("status", "fail");
            status.put("error", e.getMessage());
        }
        return status;
    }

    private Map<String, Object> checkRedis() {
        Map<String, Object> status = new HashMap<>();
        try {
            String pong = stringRedisTemplate.execute(RedisConnectionCommands::ping);
            status.put("status", "ok");
            status.put("pong", pong);
        } catch (Exception e) {
            log.error("Redis健康检查失败", e);
            status.put("status", "fail");
            status.put("error", e.getMessage());
        }
        return status;
    }
}
