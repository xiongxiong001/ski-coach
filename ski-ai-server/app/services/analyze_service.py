"""
单视频分析编排
1. 校验视频路径(沙箱安全)
2. 调用VideoProcessor提取姿态
3. 调用PoseAnalyzer计算指标
4. 调用ActionSegmenter切分动作
5. 调用LLMCoach生成报告
6. 组装结果返回
"""
import os
from pathlib import Path

from app.config import settings
from app.core.video_processor import VideoProcessor
from app.core.pose_analyzer import PoseAnalyzer
from app.core.action_segmenter import ActionSegmenter
from app.core.llm_coach import LLMCoach
from app.logger import get_logger

logger = get_logger(__name__)


class AnalyzeService:
    """单视频分析服务"""

    def analyze(self, video_path: str) -> dict:
        """
        执行完整的单视频分析流程

        参数:
            video_path: 视频文件路径(必须在 VIDEO_STORAGE_BASE_PATH 下)

        返回:
            {
                "analysis_data": { ... },     # 完整分析数据
                "report_markdown": str,        # 单次教练报告
                "llm_input_tokens": int,
                "llm_output_tokens": int,
                "llm_cost_yuan": float,
            }

        异常:
            ValueError:    路径校验失败、检测率过低
            FileNotFoundError: 视频不存在
            RuntimeError:  视频无法打开、模型推理失败
        """
        # 步骤1: 路径安全校验
        abs_video_path = self._validate_video_path(video_path)

        # 步骤2: 提取姿态
        logger.info(f"=== 开始分析视频: {abs_video_path} ===")
        processor = VideoProcessor(
            model_complexity=settings.pose_model_complexity,
            min_detection_confidence=settings.pose_min_detection_confidence,
        )
        try:
            video_result = processor.process_video(abs_video_path)
        finally:
            processor.close()

        # 步骤3: 校验检测率
        if video_result["detection_rate"] < settings.min_detection_rate:
            raise ValueError(
                f"姿态检测率过低 ({video_result['detection_rate'] * 100:.1f}%),"
                f"低于最低阈值 {settings.min_detection_rate * 100:.0f}%。"
                f"请确保视频是第三人称视角、单人画面、人物清晰。"
            )

        # 步骤4: 计算指标
        logger.info("计算姿态业务指标...")
        analyzer = PoseAnalyzer(video_result["keypoints_per_frame"], video_result["fps"])
        metrics = analyzer.analyze_all()
        summary = analyzer.summarize(metrics)

        # 步骤5: 动作分割
        logger.info("切分动作单元...")
        segmenter = ActionSegmenter(metrics, video_result["fps"])
        segments = segmenter.segment()
        action_counts = segmenter.count_actions(segments)
        logger.info(f"识别到 {len(segments)} 个动作片段, 分布: {action_counts}")

        # 步骤6: 组装分析数据
        duration = video_result["frame_count"] / video_result["fps"] if video_result["fps"] > 0 else 0
        analysis_data = {
            "video_info": {
                "duration": duration,
                "fps": video_result["fps"],
                "frame_count": video_result["frame_count"],
                "width": video_result["width"],
                "height": video_result["height"],
                "detection_rate": video_result["detection_rate"],
            },
            "summary": summary,
            "action_counts": action_counts,
            "segments": segments,
        }

        # 步骤7: 调用LLM生成报告
        coach = LLMCoach()
        llm_result = coach.generate_report(analysis_data)

        logger.info("=== 单视频分析完成 ===")

        return {
            "analysis_data": analysis_data,
            "report_markdown": llm_result["report_markdown"],
            "llm_input_tokens": llm_result["input_tokens"],
            "llm_output_tokens": llm_result["output_tokens"],
            "llm_cost_yuan": llm_result["cost_yuan"],
        }

    def _validate_video_path(self, video_path: str) -> str:
        """
        校验视频路径,防止恶意路径注入。

        规则:
        1. 文件必须存在
        2. 必须是配置的 VIDEO_STORAGE_BASE_PATH 的子路径
        3. 解析后的真实路径不能含有路径穿越(..)
        """
        if not video_path or not isinstance(video_path, str):
            raise ValueError("视频路径不能为空")

        try:
            abs_path = Path(video_path).resolve()
            base_path = Path(settings.video_storage_base_path).resolve()
        except Exception as e:
            raise ValueError(f"路径解析失败: {e}")

        # 必须是 base_path 的子路径
        try:
            abs_path.relative_to(base_path)
        except ValueError:
            raise ValueError(
                f"视频路径不在允许的存储目录下。"
                f"允许目录: {base_path}, 实际路径: {abs_path}"
            )

        if not abs_path.exists():
            raise FileNotFoundError(f"视频文件不存在: {abs_path}")

        if not abs_path.is_file():
            raise ValueError(f"路径不是文件: {abs_path}")

        return str(abs_path)
