"""
对比报告接口测试
使用伪数据测试差异计算逻辑(不调用真实LLM)
"""
from app.core.progress_comparator import compare_analyses


def make_fake_analysis(separation_mean=10, knee_left_mean=160, knee_right_mean=160,
                       torso_lean=20, weight_balance=-0.2,
                       turn_left=2, turn_right=4, duration=30, detection_rate=0.9):
    """生成伪分析数据"""
    return {
        "video_info": {
            "duration": duration,
            "detection_rate": detection_rate,
        },
        "summary": {
            "shoulder_hip_separation": {
                "mean": separation_mean, "std": 30, "min": 0, "max": 80, "median": separation_mean
            },
            "knee_flexion_left": {
                "mean": knee_left_mean, "std": 15, "min": 100, "max": 180, "median": knee_left_mean
            },
            "knee_flexion_right": {
                "mean": knee_right_mean, "std": 15, "min": 100, "max": 180, "median": knee_right_mean
            },
            "torso_lean": {
                "mean": torso_lean, "std": 20, "min": -10, "max": 50, "median": torso_lean
            },
            "weight_balance": {
                "mean": weight_balance, "std": 0.4, "min": -1, "max": 1, "median": weight_balance
            },
        },
        "action_counts": {
            "turn_left": turn_left,
            "turn_right": turn_right,
            "straight": 3,
            "jump": 0,
        },
    }


def test_compare_all_improved():
    """所有指标都进步的情况"""
    prev = make_fake_analysis(
        separation_mean=5, knee_left_mean=170, knee_right_mean=170,
        torso_lean=30, weight_balance=-0.3
    )
    curr = make_fake_analysis(
        separation_mean=20, knee_left_mean=140, knee_right_mean=140,
        torso_lean=10, weight_balance=0.1
    )
    result = compare_analyses(prev, curr)
    assert result["improved_count"] == 5
    assert result["declined_count"] == 0


def test_compare_all_declined():
    """所有指标都退步的情况"""
    prev = make_fake_analysis(
        separation_mean=20, knee_left_mean=140, knee_right_mean=140,
        torso_lean=10, weight_balance=0.1
    )
    curr = make_fake_analysis(
        separation_mean=5, knee_left_mean=170, knee_right_mean=170,
        torso_lean=30, weight_balance=-0.3
    )
    result = compare_analyses(prev, curr)
    assert result["declined_count"] == 5
    assert result["improved_count"] == 0


def test_compare_action_balance():
    """动作平衡性测试"""
    prev = make_fake_analysis(turn_left=1, turn_right=10)
    curr = make_fake_analysis(turn_left=5, turn_right=5)
    result = compare_analyses(prev, curr)
    # 上次很不平衡,本次完美平衡
    assert result["action_diff"]["prev_imbalance"] > 0.5
    assert result["action_diff"]["curr_imbalance"] == 0.0


def test_metric_diffs_structure():
    """对比结果结构完整性"""
    prev = make_fake_analysis()
    curr = make_fake_analysis()
    result = compare_analyses(prev, curr)
    
    # 关键字段都在
    assert "metric_diffs" in result
    assert "action_diff" in result
    assert "improved_count" in result
    
    # 每个指标的对比项结构正确
    for key, diff in result["metric_diffs"].items():
        assert "prev_mean" in diff
        assert "curr_mean" in diff
        assert "mean_change" in diff
        assert "mean_verdict" in diff
        assert diff["mean_verdict"] in ("improved", "declined", "stable")
