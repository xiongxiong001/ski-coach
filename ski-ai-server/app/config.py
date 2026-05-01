"""
应用配置模块
使用 pydantic-settings 从 .env 文件和环境变量加载配置
"""
from pydantic_settings import BaseSettings, SettingsConfigDict
from typing import Optional


class Settings(BaseSettings):
    """应用配置(从 .env 和环境变量自动加载)"""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",  # 忽略未定义的环境变量
    )

    # ----------- 服务配置 -----------
    host: str = "0.0.0.0"
    port: int = 8000
    log_level: str = "INFO"

    # 视频存储路径(安全沙箱)
    video_storage_base_path: str = "D:\works\ski-data"

    # ----------- LLM 配置 -----------
    use_provider: str = "deepseek"  # deepseek / openai / dashscope

    # DeepSeek
    deepseek_api_key: Optional[str] = None
    deepseek_base_url: str = "https://api.deepseek.com"
    deepseek_model: str = "deepseek-chat"

    # OpenAI
    openai_api_key: Optional[str] = None
    openai_model: str = "gpt-4o-mini"

    # 通义千问
    dashscope_api_key: Optional[str] = None
    dashscope_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    dashscope_model: str = "qwen-plus"

    # ----------- 算法配置 -----------
    pose_model_complexity: int = 1
    pose_min_detection_confidence: float = 0.5
    min_detection_rate: float = 0.2


# 全局单例
settings = Settings()
