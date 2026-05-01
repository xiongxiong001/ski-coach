# ski-ai-server

> Ski Coach AI推理服务,基于 FastAPI + MediaPipe + LLM 实现。

## 功能

- **POST /api/v1/analyze**:对单个滑雪视频做完整分析(姿态识别 → 指标计算 → 动作分割 → LLM教练报告)
- **POST /api/v1/compare**:基于两份已有的分析数据生成进步对比报告(不重新跑视频)
- **GET /health**:健康检查

## 项目结构

```
ski-ai-server/
├── app/
│   ├── main.py              # FastAPI 入口
│   ├── config.py            # 配置(从 .env 加载)
│   ├── logger.py            # 日志配置
│   ├── api/                 # 接口层
│   │   ├── routes.py        # 路由
│   │   ├── schemas.py       # Pydantic 数据模型
│   │   └── handlers.py      # 异常处理
│   ├── services/            # 业务编排层
│   │   ├── analyze_service.py
│   │   └── compare_service.py
│   └── core/                # 核心算法
│       ├── video_processor.py
│       ├── pose_analyzer.py
│       ├── action_segmenter.py
│       ├── progress_comparator.py
│       ├── llm_coach.py
│       ├── progress_coach.py
│       └── utils.py
├── tests/
├── requirements.txt
├── Dockerfile
├── .env.example
└── README.md
```

## 快速开始

### 1. 准备环境

需要 **Python 3.12**。

```bash
# 创建虚拟环境
python -m venv .venv

# 激活
# Windows:
.venv\Scripts\activate
# Mac/Linux:
source .venv/bin/activate
```

### 2. 安装依赖

```bash
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple
```

### 3. 配置 .env

```bash
# 复制配置模板
# Windows:
copy .env.example .env
# Mac/Linux:
cp .env.example .env
```

编辑 `.env`,**至少配置**:
- `DEEPSEEK_API_KEY`:你的DeepSeek API Key
- `VIDEO_STORAGE_BASE_PATH`:视频存储目录(开发时可改成本地目录,如 `D:\ski-data`)

### 4. 启动服务

```bash
# 方式1: 直接运行 main.py
python -m app.main

# 方式2: 用 uvicorn(推荐,支持热重载)
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

启动成功后:
- 服务地址: http://localhost:8000
- API文档(Swagger): http://localhost:8000/docs
- 健康检查: http://localhost:8000/health

## API 调用示例

### 健康检查

```bash
curl http://localhost:8000/health
```

响应:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "status": "ok",
    "service": "ski-ai-server",
    "version": "1.0.0",
    "llm_provider": "deepseek"
  }
}
```

### 单视频分析

```bash
curl -X POST http://localhost:8000/api/v1/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "video_path": "/data/ski_videos/test.mp4"
  }'
```

> 注意:`video_path` 必须是 `VIDEO_STORAGE_BASE_PATH` 下的文件,且必须存在。

### 对比报告

```bash
curl -X POST http://localhost:8000/api/v1/compare \
  -H "Content-Type: application/json" \
  -d '{
    "prev_analysis_data": { "summary": {...}, "action_counts": {...} },
    "curr_analysis_data": { "summary": {...}, "action_counts": {...} }
  }'
```

`prev_analysis_data` 和 `curr_analysis_data` 是之前 `/analyze` 接口返回的 `analysis_data` 字段内容。

## 测试

```bash
pytest
```

测试不会调用真实LLM API,只验证业务逻辑和接口。

## Docker 部署

```bash
# 构建镜像
docker build -t ski-coach/ai-server:latest .

# 运行(需要挂载视频目录和 .env)
docker run -d --name ski-ai-server \
  -p 8000:8000 \
  -v /data/ski_videos:/data/ski_videos:ro \
  --env-file .env \
  ski-coach/ai-server:latest
```

## 开发提示

### IDE 打开

推荐用 **PyCharm**(File → Open → 选 `ski-ai-server` 目录)或 **VSCode** 打开此目录。

不要用一个IDE打开整个 `ski-coach/` 顶层目录。

### 修改 LLM Prompt

教练话术的质量取决于 Prompt:

- 单次报告:`app/core/llm_coach.py` 的 `SYSTEM_PROMPT`
- 对比报告:`app/core/progress_coach.py` 的 `COMPARE_SYSTEM_PROMPT`

### 添加新指标

如果想加新的姿态指标(比如手臂角度),修改:

1. `app/core/pose_analyzer.py`:在 `analyze_all` 里加新指标计算
2. `app/core/progress_comparator.py`:在 `METRIC_CONFIG` 里加该指标的"理想值"配置

## 路径安全机制

`/api/v1/analyze` 接口接受 `video_path` 参数,但严格校验:

1. 路径必须存在
2. 必须是 `VIDEO_STORAGE_BASE_PATH` 配置目录的子路径
3. 不能含有 `..` 等路径穿越

防止恶意构造路径读取系统敏感文件。

## 性能参考(单台普通服务器)

| 操作 | 时长(纯CPU) | LLM调用成本 |
|------|------|-----|
| 1分钟视频的姿态识别 | 2-3分钟 | - |
| 单次报告LLM | 3-10秒 | ~¥0.002 |
| 对比报告LLM | 5-15秒 | ~¥0.005 |

总成本: 1次完整分析(分析+报告)约 **¥0.002-0.005**。
