"""
API 数据模型(Pydantic)
所有HTTP接口的请求/响应都用Pydantic定义,自动校验+生成API文档
"""
from typing import Any, Optional
from pydantic import BaseModel, Field


# ============================================================
# 通用响应包装
# ============================================================
class ApiResponse(BaseModel):
    """统一响应格式,与Java端 ApiResult 对齐"""
    code: int = Field(0, description="状态码: 0=成功,非0=失败")
    message: str = Field("success", description="提示信息")
    data: Optional[Any] = Field(None, description="数据负载")


# ============================================================
# 单视频分析接口
# ============================================================
class AnalyzeRequest(BaseModel):
    """单视频分析请求"""
    video_path: str = Field(
        ...,
        description="视频在服务器上的绝对路径,必须在 VIDEO_STORAGE_BASE_PATH 配置目录下",
        examples=["/data/ski_videos/123/2026_05/20260501143025_a3f4e8c2.mp4"],
    )


class AnalyzeResult(BaseModel):
    """单视频分析结果(放在ApiResponse.data中)"""
    analysis_data: dict = Field(..., description="完整分析数据(姿态指标、动作分割等)")
    report_markdown: str = Field(..., description="中文教练报告(Markdown格式)")
    llm_input_tokens: int = Field(0, description="LLM输入token数")
    llm_output_tokens: int = Field(0, description="LLM输出token数")
    llm_cost_yuan: float = Field(0.0, description="LLM调用估算成本(元)")


# ============================================================
# 对比报告接口
# ============================================================
class CompareRequest(BaseModel):
    """对比报告请求"""
    prev_analysis_data: dict = Field(..., description="上次视频的分析数据")
    curr_analysis_data: dict = Field(..., description="本次视频的分析数据")


class CompareResult(BaseModel):
    """对比报告结果"""
    comparison_data: dict = Field(..., description="差异计算结果")
    report_markdown: str = Field(..., description="对比教练报告(Markdown)")
    improved_count: int = Field(0, description="进步指标数")
    declined_count: int = Field(0, description="退步指标数")
    stability_improved_count: int = Field(0, description="稳定性提升数")
    llm_input_tokens: int = Field(0, description="LLM输入token数")
    llm_output_tokens: int = Field(0, description="LLM输出token数")
    llm_cost_yuan: float = Field(0.0, description="LLM调用估算成本(元)")


# ============================================================
# 健康检查
# ============================================================
class HealthResult(BaseModel):
    """健康检查结果"""
    status: str = "ok"
    service: str = "ski-ai-server"
    version: str = "1.0.0"
    llm_provider: str
