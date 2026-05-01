"""
进步对比 LLM 模块
基于两次视频的差异数据,生成中文进步对比报告
"""
import json
from openai import OpenAI

from app.config import settings
from app.core.llm_coach import TOKEN_PRICES
from app.logger import get_logger

logger = get_logger(__name__)


COMPARE_SYSTEM_PROMPT = """你是一位资深的单板滑雪教练,正在帮学员对比两次滑行视频的进步情况。

你的任务:基于两次视频的AI分析数据差异,生成一份"进步对比报告"——明确告诉学员进步了什么、需要继续努力什么、下次重点是什么。

## 这份报告和单次报告最大的区别

- **重点是"变化"**,不是当前状态
- **要量化进步**:用具体数字说"从X提升到Y"
- **要鼓励**:即使是小进步也要肯定
- **要诚实**:如果某些指标退步了,要直接指出但温和处理
- **要有策略性**:基于本次和上次的差异,制定下次训练计划

## 输出结构(用 Markdown 格式)

# 滑雪进步对比报告

## 📊 整体进步评估
(1-2 句话总结)

## 🚀 显著进步的地方
- **[指标名]**:从 X 提升到 Y

## ⚠️ 需要继续努力的地方
- **[指标名]**:从 X 变为 Y

## 🔍 教练观察
(基于数据综合判断)

## 📅 下次训练重点(基于这次对比)
1. **优先级1:[最该改进的项]** —— 具体怎么练
2. **优先级2:[第二该改进的项]** —— 具体怎么练
3. **保持优势:[已经做得好的项]** —— 怎么保持

## ⭐ 鼓励的话

## 重要原则

1. 不要重复单次报告的内容,核心是"变化"
2. 数字要准确,不要编造
3. 不要假设训练历史
4. 如果两次视频时长或检测率差异很大,要提醒
"""


class ProgressCoach:
    """进步对比报告生成器"""

    def __init__(self):
        self.provider = settings.use_provider.lower()
        self._init_client()

    def _init_client(self):
        if self.provider == "deepseek":
            if not settings.deepseek_api_key:
                raise ValueError("DEEPSEEK_API_KEY 未配置")
            self.client = OpenAI(
                api_key=settings.deepseek_api_key,
                base_url=settings.deepseek_base_url,
            )
            self.model = settings.deepseek_model
        elif self.provider == "openai":
            if not settings.openai_api_key:
                raise ValueError("OPENAI_API_KEY 未配置")
            self.client = OpenAI(api_key=settings.openai_api_key)
            self.model = settings.openai_model
        elif self.provider == "dashscope":
            if not settings.dashscope_api_key:
                raise ValueError("DASHSCOPE_API_KEY 未配置")
            self.client = OpenAI(
                api_key=settings.dashscope_api_key,
                base_url=settings.dashscope_base_url,
            )
            self.model = settings.dashscope_model
        else:
            raise ValueError(f"不支持的 provider: {self.provider}")

    def generate_report(self, comparison_data: dict) -> dict:
        """生成进步对比报告"""
        user_message = self._build_user_message(comparison_data)
        logger.info(f"调用 LLM 生成对比报告 (provider={self.provider}, model={self.model})")

        response = self.client.chat.completions.create(
            model=self.model,
            messages=[
                {"role": "system", "content": COMPARE_SYSTEM_PROMPT},
                {"role": "user", "content": user_message},
            ],
            temperature=0.7,
            max_tokens=2500,
        )

        report_text = response.choices[0].message.content
        usage = response.usage
        input_tokens = usage.prompt_tokens if usage else 0
        output_tokens = usage.completion_tokens if usage else 0
        cost_yuan = self._estimate_cost(input_tokens, output_tokens)

        logger.info(
            f"LLM 调用完成: input_tokens={input_tokens}, "
            f"output_tokens={output_tokens}, 估算成本={cost_yuan:.4f}元"
        )

        return {
            "report_markdown": report_text,
            "input_tokens": input_tokens,
            "output_tokens": output_tokens,
            "cost_yuan": cost_yuan,
        }

    def _build_user_message(self, data: dict) -> str:
        prev_video = data.get("prev_video", {})
        curr_video = data.get("curr_video", {})
        action_diff = data.get("action_diff", {})
        metric_diffs = data.get("metric_diffs", {})

        metrics_for_llm = {}
        for key, d in metric_diffs.items():
            metrics_for_llm[d["name"]] = {
                "上次均值": f"{d['prev_mean']}",
                "本次均值": f"{d['curr_mean']}",
                "均值变化": f"{'+' if d['mean_change'] > 0 else ''}{d['mean_change']}",
                "上次标准差": f"{d['prev_std']}",
                "本次标准差": f"{d['curr_std']}",
                "稳定性变化": (
                    "更稳定" if d["stability_verdict"] == "improved"
                    else ("更不稳" if d["stability_verdict"] == "declined" else "持平")
                ),
                "均值评估": (
                    "进步" if d["mean_verdict"] == "improved"
                    else ("退步" if d["mean_verdict"] == "declined" else "持平")
                ),
                "指标说明": d["description"],
            }

        prev_lr = action_diff.get("prev_left_right", (0, 0))
        curr_lr = action_diff.get("curr_left_right", (0, 0))

        message = f"""请基于以下两次滑雪视频的对比数据,生成进步对比报告。

## 视频信息
- 上次视频:时长 {prev_video.get('duration', 0):.1f}秒,姿态检测率 {prev_video.get('detection_rate', 0) * 100:.1f}%
- 本次视频:时长 {curr_video.get('duration', 0):.1f}秒,姿态检测率 {curr_video.get('detection_rate', 0) * 100:.1f}%

## 动作分布对比
- 上次:左转{prev_lr[0]}次, 右转{prev_lr[1]}次
- 本次:左转{curr_lr[0]}次, 右转{curr_lr[1]}次

## 各项指标对比

```json
{json.dumps(metrics_for_llm, indent=2, ensure_ascii=False)}
```

## 整体统计
- 进步指标数: {data.get('improved_count', 0)}
- 退步指标数: {data.get('declined_count', 0)}
- 稳定性提升数: {data.get('stability_improved_count', 0)}

请按照系统提示中的结构生成报告。"""
        return message

    def _estimate_cost(self, input_tokens: int, output_tokens: int) -> float:
        prices = TOKEN_PRICES.get(self.provider, {"input": 0, "output": 0})
        cost = (input_tokens * prices["input"] + output_tokens * prices["output"]) / 1_000_000
        return round(cost, 6)
