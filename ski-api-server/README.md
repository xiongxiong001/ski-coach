# ski-api-server

> Ski Coach 主业务API服务 - 基于 Java 21 + SpringBoot 3 实现

## 状态

🚧 **待开发(P2阶段)** - 此目录目前为空,P1完成后开始填充。

## 计划技术栈

- **JDK**: 21
- **框架**: SpringBoot 3.2.x
- **ORM**: MyBatis-Plus 3.5.x
- **数据库**: MySQL 8.0
- **缓存**: Redis 7.0 (Redisson客户端)
- **鉴权**: JWT (jjwt)
- **HTTP客户端**: OkHttp(调用ski-ai-server)
- **API文档**: Knife4j
- **构建工具**: Maven

## 启动方式(待P2完成后填充)

```bash
mvn clean install
mvn spring-boot:run
```

服务默认端口: `8080`

## IDE打开

推荐用 **IntelliJ IDEA** 打开此目录(`ski-coach/ski-api-server`),IDEA会识别到 `pom.xml` 自动作为Maven项目导入。

不要用一个IDE打开整个 `ski-coach/` 顶层目录。
