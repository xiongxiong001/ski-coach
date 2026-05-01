package com.skicoach.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ski Coach 主业务API服务启动类
 *
 * @author skicoach
 */
@SpringBootApplication
@MapperScan("com.skicoach.backend.mapper")
@EnableAsync
@EnableScheduling
public class SkiApiServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkiApiServerApplication.class, args);
        System.out.println("""
                
                ========================================================
                   Ski Coach API Server 启动成功!
                   API 文档:  http://localhost:8080/doc.html
                   健康检查:  http://localhost:8080/health
                   Druid监控: http://localhost:8080/druid/login.html
                ========================================================
                """);
    }
}
