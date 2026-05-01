"""
API 路由定义
- POST /api/v1/analyze: 单视频分析
- POST /api/v1/compare: 对比报告生成
- GET  /health: 健康检查
"""
from fastapi import APIRouter

from app.api.schemas import (
    ApiResponse, AnalyzeRequest, AnalyzeResult,
    CompareRequest, CompareResult, HealthResult,
)
from app.config import settings
from app.services.analyze_service import AnalyzeService
from app.services.compare_service import CompareService
from app.logger import get_logger

logger = get_logger(__name__)


# ============================================================
# 健康检查路由(不带版本前缀)
# ============================================================
health_router = APIRouter(tags=["健康检查"])


@health_router.get("/health", response_model=ApiResponse, summary="健康检查")
async def health_check():
    """供运维和负载均衡探活使用"""
    return ApiResponse(
        code=0,
        message="success",
        data=HealthResult(llm_provider=settings.use_provider).model_dump(),
    )


# ============================================================
# 业务路由(带 /api/v1 前缀)
# ============================================================
api_router = APIRouter(prefix="/api/v1", tags=["AI推理"])


@api_router.post(
    "/analyze",
    response_model=ApiResponse,
    summary="单视频分析",
    description="""
    对单个滑雪视频做完整的AI分析:
    1. MediaPipe 提取人体姿态
    2. 计算业务指标(肩髋分离角、膝盖弯曲度等)
    3. 切分动作单元(转弯/直行)
    4. 调用LLM生成中文教练报告

    注意: 视频文件必须已经在服务器的 VIDEO_STORAGE_BASE_PATH 目录下,
    通过 video_path 参数告知Python去哪里读取(不通过HTTP上传视频)。
    """,
)
async def analyze_video(req: AnalyzeRequest):
    """单视频分析接口"""
    logger.info(f"[API] /analyze, video_path={req.video_path}")
    service = AnalyzeService()
    result = service.analyze(req.video_path)
    return ApiResponse(
        code=0,
        message="success",
        data=AnalyzeResult(**result).model_dump(),
    )


@api_router.post(
    "/compare",
    response_model=ApiResponse,
    summary="对比报告生成",
    description="""
    对比两次分析结果,生成进步对比报告。

    注意: 此接口不接收视频文件,只接收已经计算好的分析数据JSON。
    Java那边应该从数据库读取两次的 analysis_data,然后传给Python。
    这是为了避免重复跑MediaPipe(单视频处理要1-3分钟)。
    """,
)
async def compare_videos(req: CompareRequest):
    """对比报告生成接口"""
    logger.info("[API] /compare")
    service = CompareService()
    result = service.compare(req.prev_analysis_data, req.curr_analysis_data)
    return ApiResponse(
        code=0,
        message="success",
        data=CompareResult(**result).model_dump(),
    )
