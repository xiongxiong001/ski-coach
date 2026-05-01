"""
全局异常处理
把各种异常转换为统一的 ApiResponse 格式
"""
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.api.schemas import ApiResponse
from app.logger import get_logger

logger = get_logger(__name__)


# 错误码约定
class ErrorCode:
    PARAM_ERROR = 4001         # 参数错误
    FILE_NOT_FOUND = 4004      # 文件不存在
    AUTH_ERROR = 4010          # 鉴权失败(预留)
    BUSINESS_ERROR = 5000      # 业务异常
    LLM_ERROR = 5001           # LLM调用失败
    SYSTEM_ERROR = 5500        # 未知系统异常


def register_exception_handlers(app: FastAPI):
    """注册所有异常处理器到FastAPI实例"""

    @app.exception_handler(ValueError)
    async def value_error_handler(request: Request, exc: ValueError):
        logger.warning(f"参数错误: {exc}")
        return JSONResponse(
            status_code=200,  # 业务异常用HTTP 200,通过code区分
            content=ApiResponse(
                code=ErrorCode.PARAM_ERROR,
                message=str(exc),
            ).model_dump(),
        )

    @app.exception_handler(FileNotFoundError)
    async def file_not_found_handler(request: Request, exc: FileNotFoundError):
        logger.warning(f"文件不存在: {exc}")
        return JSONResponse(
            status_code=200,
            content=ApiResponse(
                code=ErrorCode.FILE_NOT_FOUND,
                message=str(exc),
            ).model_dump(),
        )

    @app.exception_handler(RequestValidationError)
    async def validation_error_handler(request: Request, exc: RequestValidationError):
        # FastAPI 的请求体校验错误
        errors = exc.errors()
        msg = errors[0].get("msg", "请求参数校验失败") if errors else "请求参数校验失败"
        logger.warning(f"请求校验失败: {errors}")
        return JSONResponse(
            status_code=200,
            content=ApiResponse(
                code=ErrorCode.PARAM_ERROR,
                message=msg,
                data={"errors": errors},
            ).model_dump(),
        )

    @app.exception_handler(StarletteHTTPException)
    async def http_exception_handler(request: Request, exc: StarletteHTTPException):
        logger.warning(f"HTTP异常 {exc.status_code}: {exc.detail}")
        return JSONResponse(
            status_code=exc.status_code,
            content=ApiResponse(
                code=exc.status_code,
                message=str(exc.detail),
            ).model_dump(),
        )

    @app.exception_handler(Exception)
    async def general_exception_handler(request: Request, exc: Exception):
        # 兜底:任何未捕获的异常
        logger.error(f"未捕获异常: {type(exc).__name__}: {exc}", exc_info=True)
        # 区分一下LLM调用失败
        msg = str(exc)
        if "api" in msg.lower() and ("key" in msg.lower() or "token" in msg.lower()):
            code = ErrorCode.LLM_ERROR
        else:
            code = ErrorCode.SYSTEM_ERROR
        return JSONResponse(
            status_code=200,
            content=ApiResponse(
                code=code,
                message=f"系统异常: {msg}",
            ).model_dump(),
        )
