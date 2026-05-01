# Ski Coach - AI滑雪教练平台

> 基于AI的滑雪视频分析与教练点评系统,提供单次视频分析和跨视频进步对比功能。

## 项目结构

```
ski-coach/
├── ski-api-server/          # 主业务API服务(Java + SpringBoot 3 + JDK 21)
├── ski-ai-server/           # AI推理服务(Python 3.12 + FastAPI + MediaPipe)
├── ski-user-web/            # 用户端Web应用(Vue 3 + Vite + Element Plus)
├── ski-admin-web/           # 管理后台Web应用(Vue 3 + Vite + Element Plus)
├── deploy/                  # 部署相关(Nginx配置、数据库初始化SQL)
├── docs/                    # 项目文档
├── docker-compose.yml       # 一键启动所有服务
└── README.md
```

## 技术架构

```
         ┌─────────────────────┐    ┌──────────────────────┐
         │   ski-user-web      │    │   ski-admin-web      │
         │   (用户端Web)       │    │   (管理后台)         │
         └──────────┬──────────┘    └──────────┬───────────┘
                    │                          │
                    └──────────┬───────────────┘
                               │ HTTP/JSON
                    ┌──────────▼──────────┐
                    │   ski-api-server    │
                    │   (主业务服务)      │
                    │   Java + SpringBoot │
                    └──────────┬──────────┘
                               │ HTTP内部调用
                    ┌──────────▼──────────┐    ┌─────────────────┐
                    │   ski-ai-server     │───▶│  LLM API        │
                    │   (AI推理服务)      │    │  (DeepSeek等)   │
                    │   Python + FastAPI  │    └─────────────────┘
                    └─────────────────────┘
                               
                    存储: MySQL 8.0 + Redis 7.0 + 本地磁盘
```

## 各子项目的IDE打开方式

每个子项目都是**独立的工程**,推荐用对应的IDE分别打开,而不是把整个 `ski-coach/` 用一个IDE打开。

| 子项目 | 推荐IDE | 打开方式 |
|--------|---------|---------|
| `ski-api-server/` | IntelliJ IDEA | File → Open → 选择 `ski-coach/ski-api-server` 目录,IDEA识别到 `pom.xml` 自动作为Maven项目导入 |
| `ski-ai-server/` | PyCharm 或 VSCode | File → Open → 选择 `ski-coach/ski-ai-server` 目录 |
| `ski-user-web/` | VSCode 或 WebStorm | 选择 `ski-coach/ski-user-web` 目录 |
| `ski-admin-web/` | VSCode 或 WebStorm | 选择 `ski-coach/ski-admin-web` 目录 |

> **不推荐**用一个IDE打开整个 `ski-coach/`——会让IDE索引大量不同语言的文件,影响性能且容易混乱。

## 开发流程

1. 克隆仓库
   ```bash
   git clone <repo-url>
   cd ski-coach
   ```

2. 各子项目独立开发(每个子项目有自己的README)
   - 后端Java: 看 `ski-api-server/README.md`
   - AI服务: 看 `ski-ai-server/README.md`
   - 用户端: 看 `ski-user-web/README.md`
   - 管理后台: 看 `ski-admin-web/README.md`

3. 一键启动整体环境(开发完成后)
   ```bash
   docker-compose up -d
   ```

## 端口约定

开发环境各服务的默认端口:

| 服务 | 端口 | 说明 |
|------|------|------|
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 + 任务队列 |
| ski-api-server | 8080 | 主业务API |
| ski-ai-server | 8000 | Python AI服务 |
| ski-user-web (dev) | 5173 | Vite开发服务器 |
| ski-admin-web (dev) | 5174 | Vite开发服务器 |
| Nginx (生产) | 80/443 | 反向代理,所有流量入口 |

## 开发顺序

整个项目按以下顺序开发,每一步都可独立验证:

- [ ] **P1**: ski-ai-server (Python AI服务) - 基础推理能力
- [ ] **P2**: ski-api-server (Java后端) - 业务逻辑与数据持久化
- [ ] **P3**: ski-user-web (用户端) - 用户交互
- [ ] **P4**: ski-admin-web (管理后台) - 运营管理
- [ ] **P5**: Docker Compose 整合 + 部署文档

## 数据库

数据库初始化脚本在 `deploy/init.sql`,首次部署执行此脚本创建所有表。

## 环境要求

| 工具 | 版本 |
|------|------|
| JDK | 21 |
| Maven | 3.9+ |
| Python | 3.12 |
| Node.js | 18+ |
| MySQL | 8.0 |
| Redis | 7.0+ |
| Docker | 24+ (可选,用于一键部署) |

## 联系

项目维护者: (待填写)
