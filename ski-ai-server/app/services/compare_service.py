"""
对比报告编排
对比两份已有的分析数据,生成对比教练报告。
注意: 此接口不接收视频文件,只接收已经计算好的analysis_data。
"""
from app.core.progress_comparator import compare_analyses
from app.core.progress_coach import ProgressCoach
from app.logger import get_logger

logger = get_logger(__name__)


class CompareService:
    """对比报告服务"""

    def compare(self, prev_analysis_data: dict, curr_analysis_data: dict) -> dict:
        """
        生成对比报告

        参数:
            prev_analysis_data: 上次的分析数据
            curr_analysis_data: 本次的分析数据

        返回:
            {
                "comparison_data": { ... },
                "report_markdown": str,
                "improved_count": int,
                "declined_count": int,
                "stability_improved_count": int,
                "llm_input_tokens": int,
                "llm_output_tokens": int,
                "llm_cost_yuan": float,
            }
        """
        logger.info("=== 开始生成对比报告 ===")

        # 步骤1: 校验输入
        self._validate_analysis_data(prev_analysis_data, "prev_analysis_data")
        self._validate_analysis_data(curr_analysis_data, "curr_analysis_data")

        # 步骤2: 计算差异
        logger.info("计算指标差异...")
        comparison_data = compare_analyses(prev_analysis_data, curr_analysis_data)
        logger.info(
            f"差异计算完成: 进步{comparison_data['improved_count']}项, "
            f"退步{comparison_data['declined_count']}项, "
            f"稳定性提升{comparison_data['stability_improved_count']}项"
        )

        # 步骤3: 调用LLM
        coach = ProgressCoach()
        llm_result = coach.generate_report(comparison_data)

        logger.info("=== 对比报告生成完成 ===")

        return {
            "comparison_data": comparison_data,
            "report_markdown": llm_result["report_markdown"],
            "improved_count": comparison_data["improved_count"],
            "declined_count": comparison_data["declined_count"],
            "stability_improved_count": comparison_data["stability_improved_count"],
            "llm_input_tokens": llm_result["input_tokens"],
            "llm_output_tokens": llm_result["output_tokens"],
            "llm_cost_yuan": llm_result["cost_yuan"],
        }

    def _validate_analysis_data(self, data: dict, name: str):
        """校验分析数据格式"""
        if not isinstance(data, dict):
            raise ValueError(f"{name} 必须是dict")
        if "summary" not in data:
            raise ValueError(f"{name} 缺少必需字段 'summary'")
        if not isinstance(data["summary"], dict) or len(data["summary"]) == 0:
            raise ValueError(f"{name} 的 summary 不能为空")
