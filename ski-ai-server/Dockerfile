FROM python:3.12-slim

# 系统依赖(OpenCV/MediaPipe需要)
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 配置pip镜像加速(国内构建用)
RUN pip config set global.index-url https://pypi.tuna.tsinghua.edu.cn/simple

# 先复制 requirements.txt 利用 Docker 缓存
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制应用代码
COPY app/ ./app/

# 创建视频存储目录(实际部署时通过 volume 挂载)
RUN mkdir -p /data/ski_videos

EXPOSE 8000

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health')" || exit 1

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
