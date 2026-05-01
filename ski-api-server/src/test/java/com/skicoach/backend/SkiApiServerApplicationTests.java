package com.skicoach.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * SpringBoot 上下文加载测试
 *
 * 此测试只验证 Spring 容器能否正常启动,所有 Bean 能否正确装配。
 * 注意: 运行此测试需要 MySQL 和 Redis 已启动。
 *
 * 如果不想依赖外部服务,可以加 @ActiveProfiles("test") 切换到测试环境配置(后续添加)。
 */
@SpringBootTest
public class SkiApiServerApplicationTests {

    @Test
    void contextLoads() {
        // 这个空测试方法的作用是: 验证 SpringBoot 应用上下文能正常加载
        // 如果有任何 Bean 配置错误,这里会失败
    }
}
