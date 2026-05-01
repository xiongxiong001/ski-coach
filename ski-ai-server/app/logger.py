"""
日志配置模块
统一日志格式,便于Java端和运维查看
"""
import logging
import sys
from app.config import settings


def setup_logging():
    """配置全局日志"""
    log_format = "[%(asctime)s] [%(levelname)s] [%(name)s] %(message)s"
    date_format = "%Y-%m-%d %H:%M:%S"

    # 解析日志级别
    level = getattr(logging, settings.log_level.upper(), logging.INFO)

    # 配置root logger
    logging.basicConfig(
        level=level,
        format=log_format,
        datefmt=date_format,
        handlers=[logging.StreamHandler(sys.stdout)],
        force=True,  # 覆盖uvicorn的默认配置
    )

    # 调低第三方库的日志级别(避免太吵)
    logging.getLogger("uvicorn.access").setLevel(logging.WARNING)
    logging.getLogger("httpx").setLevel(logging.WARNING)
    logging.getLogger("openai").setLevel(logging.WARNING)


def get_logger(name: str) -> logging.Logger:
    """获取一个 logger 实例"""
    return logging.getLogger(name)
