# ski-api-server

> Ski Coach 主业务API服务,基于 JDK 21 + SpringBoot 3.2 + MyBatis-Plus 实现。

## 当前进度

🟢 **P2.1 完成** - 项目骨架与基础设施
🟢 **P2.2 完成** - 用户与鉴权
🟢 **P2.3 完成** - 视频上传 + 文件存储
🟢 **P2.4 完成** - 异步任务 + AI集成
- ✅ Redis 任务队列(Redisson RBlockingQueue)
- ✅ TaskWorker 后台线程消费任务
- ✅ PythonAiClient(OkHttp 调用 ski-ai-server)
- ✅ SingleAnalysisHandler(单视频分析全流程)
- ✅ 视频上传后自动触发分析
- ✅ analysis_data 写到 videos 表(供后续对比复用)
- ✅ GET /api/tasks/{id}        查询任务状态(供前端轮询)
- ✅ GET /api/reports            报告列表
- ✅ GET /api/reports/{id}       报告详情

⚪ P2.5 - 对比报告 + 管理后台

## 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| JDK | 21 | 运行时 |
| SpringBoot | 3.2.5 | 主框架 |
| MyBatis-Plus | 3.5.5 | ORM |
| MySQL | 8.0+ | 数据库 |
| Druid | 1.2.23 | 数据库连接池 |
| Redis | 7.0+ | 缓存 + 任务队列 |
| Redisson | 3.27.2 | Redis 客户端 |
| jjwt | 0.12.5 | JWT 鉴权 |
| OkHttp | 4.12.0 | 调用 ski-ai-server |
| Knife4j | 4.4.0 | API 文档 |
| Hutool | 5.8.27 | 工具库 |
| Lombok | 1.18.x | 样板代码消除 |

## 快速开始

### 1. 准备环境

| 工具 | 版本 |
|------|------|
| JDK | 21 |
| Maven | 3.9+ |
| MySQL | 8.0+ |
| Redis | 7.0+ |

### 2. 初始化数据库

执行项目根目录的 `deploy/init.sql`:

```bash
mysql -uroot -px123456 < ../deploy/init.sql
```

会创建 `ski_coach` 数据库及6张业务表。

### 3. 启动 Redis

确保 Redis 在 6379 端口运行(本地无密码)。

### 4. 检查配置

`src/main/resources/application-dev.yml` 默认配置:
- MySQL: `localhost:3306`,用户名 `root`,密码 `x123456`
- Redis: `localhost:6379`,无密码

如果你的环境不同,修改这个文件。

### 5. 启动 ski-ai-server(可选)

P2.1 阶段不直接调用 AI 服务,但建议保持 ski-ai-server 也在运行(默认端口 8001),方便后续阶段联调。

### 6. 启动本服务

**方式一:命令行**

```bash
mvn clean spring-boot:run
```

**方式二:IDEA**

直接运行 `SkiApiServerApplication` 主类。

启动成功后:
- 服务: http://localhost:8080
- API 文档: http://localhost:8080/doc.html
- 健康检查: http://localhost:8080/health
- Druid 监控: http://localhost:8080/druid/login.html (账号 `admin` / 密码 `druid_admin_123`)

## 验证

### 1. 健康检查

```bash
curl http://localhost:8080/health
```

期望返回:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "service": "ski-api-server",
    "status": "ok",
    "timestamp": "2026-05-01T15:30:00",
    "mysql": { "status": "ok", "queryResult": 1 },
    "redis": { "status": "ok", "pong": "PONG" }
  }
}
```

如果 mysql 或 redis 显示 `fail`,看 `error` 字段排查。

### 2. API 文档

浏览器打开 http://localhost:8080/doc.html ,应该看到 Knife4j 的 UI,左侧能看到"用户端 API"分组下的"健康检查"接口。

### 3. Druid 监控

http://localhost:8080/druid/login.html ,用 `admin` / `druid_admin_123` 登录,可以看到所有数据库连接和SQL执行情况。

## 项目结构

```
ski-api-server/
├── pom.xml                          # Maven 配置
├── README.md
├── .gitignore
└── src/
    ├── main/
    │   ├── java/com/skicoach/backend/
    │   │   ├── SkiApiServerApplication.java   # 启动类
    │   │   ├── config/                         # 配置类
    │   │   │   ├── MybatisPlusConfig.java
    │   │   │   ├── RedisConfig.java
    │   │   │   ├── WebConfig.java
    │   │   │   ├── Knife4jConfig.java
    │   │   │   └── AsyncConfig.java
    │   │   ├── common/                         # 通用组件
    │   │   │   ├── result/        # ApiResult, PageResult, ResultCode
    │   │   │   ├── exception/     # 业务异常 + 全局处理器
    │   │   │   ├── enums/         # 业务枚举
    │   │   │   ├── constant/      # 常量(Redis Key)
    │   │   │   └── util/          # JwtUtil, PasswordUtil, SecurityUtil
    │   │   ├── interceptor/                    # P2.2 添加
    │   │   ├── controller/                     # P2.1 仅有 HealthController
    │   │   ├── service/                        # P2.2+ 添加
    │   │   ├── worker/                         # P2.4 添加
    │   │   ├── client/                         # P2.4 添加
    │   │   ├── mapper/                         # P2.2+ 添加
    │   │   ├── entity/                         # P2.2+ 添加
    │   │   └── dto/                            # P2.2+ 添加
    │   └── resources/
    │       ├── application.yml          # 主配置
    │       ├── application-dev.yml      # 开发环境
    │       ├── application-prod.yml     # 生产环境
    │       └── mapper/                  # MyBatis XML
    └── test/
        └── java/com/skicoach/backend/
            └── SkiApiServerApplicationTests.java
```

## IDE 打开

推荐用 **IntelliJ IDEA**:
- File → Open → 选择 `ski-api-server` 目录
- 等待 Maven 下载依赖(首次较慢,后续从本地仓库读取)
- 设置 Project SDK 为 21
- 启用 Lombok 插件(IDEA 2023.x 已内置)
- 启动 `SkiApiServerApplication`

## 常见问题

**Q: 启动报错 "Communications link failure"?**
A: MySQL 连接失败。检查 MySQL 是否启动、密码是否正确(默认配置 `root`/`x123456`)。

**Q: 启动报错 "Unable to connect to Redis"?**
A: Redis 未启动或端口不对。Windows 可下载 Memurai 或 Redis Stack 用作 Redis;或用 Docker `docker run -d -p 6379:6379 redis:7`。

**Q: Maven 下载依赖很慢?**
A: 已配置阿里云镜像。如果还是慢,检查网络。也可以全局配置 Maven 镜像。

**Q: IDEA 报 Lombok 注解找不到?**
A: 确认装了 Lombok 插件。Settings → Build → Compiler → Annotation Processors,勾选 "Enable annotation processing"。

**Q: 启动后 /doc.html 404?**
A: 检查 `application.yml` 里 `knife4j.production` 是否为 `false`(开发环境)。
