"""
进步对比模块
对比两份分析数据,计算各项指标的差异,生成结构化的差异数据
"""

# 指标方向性配置
METRIC_CONFIG = {
    "shoulder_hip_separation": {
        "name": "肩髋分离角",
        "unit": "度",
        "direction": "target",
        "target_range": (15, 30),
        "description": "上下半身扭转幅度,理想15-30度",
    },
    "knee_flexion_left": {
        "name": "左膝弯曲度",
        "unit": "度",
        "direction": "target",
        "target_range": (130, 150),
        "description": "左膝弯曲角度,理想130-150度(180=直立)",
    },
    "knee_flexion_right": {
        "name": "右膝弯曲度",
        "unit": "度",
        "direction": "target",
        "target_range": (130, 150),
        "description": "右膝弯曲角度,理想130-150度",
    },
    "torso_lean": {
        "name": "躯干前倾",
        "unit": "度",
        "direction": "target",
        "target_range": (5, 15),
        "description": "躯干相对垂直的倾角,理想轻微前倾5-15度",
    },
    "weight_balance": {
        "name": "重心平衡",
        "unit": "",
        "direction": "absolute_close",
        "target_value": 0.1,
        "description": "前后重心,理想接近0.1(略微前压)",
    },
}


def compare_analyses(prev_analysis: dict, curr_analysis: dict) -> dict:
    """
    对比两份分析数据,生成差异数据

    参数:
        prev_analysis: 上次的分析数据(包含 summary, action_counts, video_info)
        curr_analysis: 本次的分析数据

    返回:
        包含各项指标对比的字典
    """
    prev_summary = prev_analysis.get("summary", {})
    curr_summary = curr_analysis.get("summary", {})
    prev_actions = prev_analysis.get("action_counts", {})
    curr_actions = curr_analysis.get("action_counts", {})

    metric_diffs = {}

    for key, config in METRIC_CONFIG.items():
        if key not in prev_summary or key not in curr_summary:
            continue

        prev_mean = prev_summary[key]["mean"]
        curr_mean = curr_summary[key]["mean"]
        prev_std = prev_summary[key]["std"]
        curr_std = curr_summary[key]["std"]

        verdict = _evaluate_change(prev_mean, curr_mean, config)
        stability_verdict = (
            "improved" if curr_std < prev_std * 0.9
            else ("declined" if curr_std > prev_std * 1.1 else "stable")
        )

        metric_diffs[key] = {
            "name": config["name"],
            "description": config["description"],
            "prev_mean": round(prev_mean, 2),
            "curr_mean": round(curr_mean, 2),
            "mean_change": round(curr_mean - prev_mean, 2),
            "prev_std": round(prev_std, 2),
            "curr_std": round(curr_std, 2),
            "std_change": round(curr_std - prev_std, 2),
            "mean_verdict": verdict,
            "stability_verdict": stability_verdict,
        }

    # 动作平衡性
    def left_right_balance(actions):
        l = actions.get("turn_left", 0)
        r = actions.get("turn_right", 0)
        if l + r == 0:
            return None
        return abs(l - r) / (l + r)

    action_diff = {
        "prev_left_right": (prev_actions.get("turn_left", 0), prev_actions.get("turn_right", 0)),
        "curr_left_right": (curr_actions.get("turn_left", 0), curr_actions.get("turn_right", 0)),
        "prev_imbalance": left_right_balance(prev_actions),
        "curr_imbalance": left_right_balance(curr_actions),
    }

    # 视频信息(可选)
    prev_info = prev_analysis.get("video_info", {})
    curr_info = curr_analysis.get("video_info", {})

    return {
        "prev_video": {
            "duration": prev_info.get("duration", 0),
            "detection_rate": prev_info.get("detection_rate", 0),
        },
        "curr_video": {
            "duration": curr_info.get("duration", 0),
            "detection_rate": curr_info.get("detection_rate", 0),
        },
        "metric_diffs": metric_diffs,
        "action_diff": action_diff,
        "improved_count": sum(1 for d in metric_diffs.values() if d["mean_verdict"] == "improved"),
        "declined_count": sum(1 for d in metric_diffs.values() if d["mean_verdict"] == "declined"),
        "stability_improved_count": sum(
            1 for d in metric_diffs.values() if d["stability_verdict"] == "improved"
        ),
    }


def _evaluate_change(prev, curr, config):
    direction = config["direction"]

    if direction == "target":
        low, high = config["target_range"]
        prev_dist = _distance_to_range(prev, low, high)
        curr_dist = _distance_to_range(curr, low, high)
        if curr_dist < prev_dist * 0.85:
            return "improved"
        elif curr_dist > prev_dist * 1.15:
            return "declined"
        return "stable"

    elif direction == "absolute_close":
        target = config["target_value"]
        prev_dist = abs(prev - target)
        curr_dist = abs(curr - target)
        if curr_dist < prev_dist * 0.85:
            return "improved"
        elif curr_dist > prev_dist * 1.15:
            return "declined"
        return "stable"

    elif direction == "lower_better":
        if curr < prev * 0.9:
            return "improved"
        elif curr > prev * 1.1:
            return "declined"
        return "stable"

    return "stable"


def _distance_to_range(value, low, high):
    if low <= value <= high:
        return 0
    if value < low:
        return low - value
    return value - high
