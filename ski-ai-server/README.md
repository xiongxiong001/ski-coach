# ski-ai-server

> Ski Coach AI推理服务 - 基于 Python 3.12 + FastAPI 实现

## 状态

🚧 **待开发(P1阶段)** - 此目录目前为空,即将开始填充。

## 计划技术栈

- **Python**: 3.12
- **Web框架**: FastAPI
- **姿态识别**: MediaPipe Pose
- **视频处理**: OpenCV
- **数值计算**: NumPy
- **LLM调用**: openai SDK(兼容DeepSeek/通义千问/OpenAI)
- **服务器**: Uvicorn

## 核心接口(待P1完成后实现)

| 接口 | 说明 |
|------|------|
| `POST /api/v1/analyze` | 单视频分析(返回姿态数据 + 单次教练报告) |
| `POST /api/v1/compare` | 对比报告生成(基于已有的分析数据) |
| `GET /health` | 健康检查 |

## 启动方式(待P1完成后填充)

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

服务默认端口: `8000`

## IDE打开

推荐用 **PyCharm** 或 **VSCode** 打开此目录(`ski-coach/ski-ai-server`)。

不要用一个IDE打开整个 `ski-coach/` 顶层目录。
