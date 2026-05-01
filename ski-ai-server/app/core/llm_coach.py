"""
单次教练话术 LLM 模块
改造点(相对POC):
1. 返回结构化结果(报告 + token消耗 + 估算成本)
2. 根据config单例初始化,而不是从os.getenv读取
3. 异常透传给上层
"""
import json
from typing import Optional
from openai import OpenAI

from app.config import settings
from app.logger import get_logger

logger = get_logger(__name__)


# 各provider的token价格(人民币/百万token)
# 仅用于成本估算,实际价格以官方为准
TOKEN_PRICES = {
    "deepseek": {"input": 1.0, "output": 2.0},     # DeepSeek V3
    "openai": {"input": 1.05, "output": 4.20},     # gpt-4o-mini 折算
    "dashscope": {"input": 4.0, "output": 12.0},   # qwen-plus
}


SYSTEM_PROMPT = """你是一位资深的单板滑雪教练,拥有 AASI Level 2 认证,有10年以上单板教学经验。

你的任务:基于一份"AI视频分析报告"中的技术指标数据,为学员生成一份专业、温暖、可执行的中文教练点评。

## 输出要求

1. 用对话化的口吻,像在雪场和学员当面交流(不要像写论文)
2. 不要堆砌专业术语,用学员能理解的话
3. 要具体,不能说"你需要改进姿态"这种空话,必须给出明确的练习方法
4. 整体语气积极正向,先肯定亮点再指出问题
5. 每条建议都要让学员"下次上山就能试"

## 输出结构(用 Markdown 格式)

# 滑雪技术分析报告

## 本次滑行概况
(1-2 句话总结)

## ✨ 做得不错的地方
- (1-2 条优点)

## 🎯 需要重点改进的 3 个方面

### 1. [问题简短标题]
- **现象**:(基于数据具体描述,例如"你的肩髋分离角平均只有X度,说明...")
- **原因**:(为什么会这样)
- **下次试试**:(具体的、可操作的练习方法)

### 2. ...
### 3. ...

## 📅 下次训练重点
(给出 2-3 条简短的训练目标)

## 数据说明

输入的数据中包含以下指标:
- shoulder_hip_separation: 肩线和髋线的分离角度(度)。理想值通常在 15-30度之间。
- knee_flexion_left/right: 左右膝盖弯曲角度(180度=直立,90度=深蹲)。滑雪中通常在 130-160度之间。
- torso_lean: 躯干前倾角(正值=前倾,负值=后倾)。理想值通常 5-15度的轻微前倾。
- weight_balance: 重心前后平衡(-1到1,负值=偏后,正值=偏前)。理想值在 0 到 0.2 之间。
- 动作分割结果会告诉你滑行中有多少次左转、右转、直行。

## 重要原则

- 如果数据显示某项指标方差(std)很大,说明动作不稳定,这本身就是一个问题
- 如果数据显示左右两侧不对称(比如左膝弯曲度和右膝差距大),要指出来
- 如果转弯次数太少或不平衡(只左转不右转),要提示
- 不要编造数据中没有的事情
"""


class LLMCoach:
    """单次教练报告生成器"""

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

    def generate_report(self, analysis_data: dict) -> dict:
        """
        生成单次教练报告

        参数:
            analysis_data: 包含video_info, summary, action_counts的字典

        返回:
            {
                "report_markdown": str,
                "input_tokens": int,
                "output_tokens": int,
                "cost_yuan": float,
            }
        """
        user_message = self._build_user_message(analysis_data)
        logger.info(f"调用 LLM 生成单次报告 (provider={self.provider}, model={self.model})")

        response = self.client.chat.completions.create(
            model=self.model,
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": user_message},
            ],
            temperature=0.7,
            max_tokens=2000,
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

    def _build_user_message(self, analysis_data: dict) -> str:
        video_info = analysis_data.get("video_info", {})
        summary = analysis_data.get("summary", {})
        action_counts = analysis_data.get("action_counts", {})

        message = f"""请基于下面的滑雪视频AI分析数据,生成一份教练点评报告。

## 视频基本信息
- 时长: {video_info.get('duration', 0):.1f} 秒
- 帧率: {video_info.get('fps', 0):.1f} fps
- 姿态检测率: {video_info.get('detection_rate', 0) * 100:.1f}%

## 动作分布
- 左转次数: {action_counts.get('turn_left', 0)}
- 右转次数: {action_counts.get('turn_right', 0)}
- 直行片段: {action_counts.get('straight', 0)}

## 各项指标统计(整段视频)

```json
{json.dumps(summary, indent=2, ensure_ascii=False)}
```

请按照系统提示中的结构生成报告。"""
        return message

    def _estimate_cost(self, input_tokens: int, output_tokens: int) -> float:
        prices = TOKEN_PRICES.get(self.provider, {"input": 0, "output": 0})
        cost = (input_tokens * prices["input"] + output_tokens * prices["output"]) / 1_000_000
        return round(cost, 6)
