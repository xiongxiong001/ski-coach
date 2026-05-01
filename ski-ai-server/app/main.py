"""
FastAPI 主入口

启动方式:
    uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
"""
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.logger import setup_logging, get_logger
from app.api.routes import api_router, health_router
from app.api.handlers import register_exception_handlers

# 初始化日志
setup_logging()
logger = get_logger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # 启动时
    logger.info("=" * 60)
    logger.info("Ski Coach AI Server 启动")
    logger.info(f"监听地址: {settings.host}:{settings.port}")
    logger.info(f"LLM Provider: {settings.use_provider}")
    logger.info(f"视频存储基础路径: {settings.video_storage_base_path}")
    logger.info(f"API文档: http://localhost:{settings.port}/docs")
    logger.info("=" * 60)
    yield
    # 关闭时
    logger.info("Ski Coach AI Server 关闭")


# 创建 FastAPI 应用
app = FastAPI(
    title="Ski Coach AI Server",
    description="滑雪AI推理服务 - 提供单视频分析和进步对比报告生成",
    version="1.0.0",
    docs_url="/docs",      # Swagger UI
    redoc_url="/redoc",    # ReDoc
    lifespan=lifespan,
)

# 注册异常处理器
register_exception_handlers(app)

# CORS(主要给开发环境用,生产环境通过Nginx统一处理)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册路由
app.include_router(health_router)
app.include_router(api_router)


# 直接运行(开发用)
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.host,
        port=settings.port,
        reload=True,
    )
