"""
姿态分析模块
基于关键点序列计算业务指标:
- 肩髋分离角(身体扭转程度)
- 膝盖弯曲度(降重心程度)
- 躯干前倾角
- 重心前后平衡
- 髋部Y坐标(起伏运动)
"""
import numpy as np

from app.core.utils import (
    KP, calc_angle_2d, calc_line_angle, midpoint, smooth_signal, safe_divide,
)


class PoseAnalyzer:
    def __init__(self, keypoints_per_frame, fps):
        self.keypoints = keypoints_per_frame
        self.fps = fps
        self.n_frames = len(keypoints_per_frame)

    def analyze_all(self):
        """计算每一帧的所有业务指标"""
        metrics = {
            "shoulder_hip_separation": [],
            "knee_flexion_left": [],
            "knee_flexion_right": [],
            "torso_lean": [],
            "weight_balance": [],
            "hip_y": [],
        }

        for kp in self.keypoints:
            if kp is None:
                for k in metrics:
                    metrics[k].append(np.nan)
                continue

            metrics["shoulder_hip_separation"].append(self._shoulder_hip_separation(kp))
            metrics["knee_flexion_left"].append(self._knee_flexion(kp, side="left"))
            metrics["knee_flexion_right"].append(self._knee_flexion(kp, side="right"))
            metrics["torso_lean"].append(self._torso_lean(kp))
            metrics["weight_balance"].append(self._weight_balance(kp))
            metrics["hip_y"].append(self._hip_y(kp))

        # 转numpy数组并平滑
        for k in metrics:
            arr = np.array(metrics[k], dtype=np.float32)
            arr = self._interpolate_nan(arr)
            arr = smooth_signal(arr, window_size=5)
            metrics[k] = arr

        return metrics

    def _shoulder_hip_separation(self, kp):
        ls, rs = kp[KP.LEFT_SHOULDER], kp[KP.RIGHT_SHOULDER]
        lh, rh = kp[KP.LEFT_HIP], kp[KP.RIGHT_HIP]
        shoulder_angle = calc_line_angle(ls, rs)
        hip_angle = calc_line_angle(lh, rh)
        diff = abs(shoulder_angle - hip_angle)
        if diff > 90:
            diff = 180 - diff
        return diff

    def _knee_flexion(self, kp, side="left"):
        if side == "left":
            hip, knee, ankle = kp[KP.LEFT_HIP], kp[KP.LEFT_KNEE], kp[KP.LEFT_ANKLE]
        else:
            hip, knee, ankle = kp[KP.RIGHT_HIP], kp[KP.RIGHT_KNEE], kp[KP.RIGHT_ANKLE]
        return calc_angle_2d(hip, knee, ankle)

    def _torso_lean(self, kp):
        shoulder_mid = midpoint(kp[KP.LEFT_SHOULDER], kp[KP.RIGHT_SHOULDER])
        hip_mid = midpoint(kp[KP.LEFT_HIP], kp[KP.RIGHT_HIP])
        dx = shoulder_mid[0] - hip_mid[0]
        dy = hip_mid[1] - shoulder_mid[1]
        return float(np.degrees(np.arctan2(dx, dy)))

    def _weight_balance(self, kp):
        hip_mid = midpoint(kp[KP.LEFT_HIP], kp[KP.RIGHT_HIP])
        ankle_mid = midpoint(kp[KP.LEFT_ANKLE], kp[KP.RIGHT_ANKLE])
        offset = hip_mid[0] - ankle_mid[0]
        foot_distance = abs(kp[KP.LEFT_ANKLE][0] - kp[KP.RIGHT_ANKLE][0])
        normalized = safe_divide(offset, foot_distance + 0.05, default=0.0)
        return float(np.clip(normalized, -1.0, 1.0))

    def _hip_y(self, kp):
        hip_mid = midpoint(kp[KP.LEFT_HIP], kp[KP.RIGHT_HIP])
        return hip_mid[1]

    def _interpolate_nan(self, arr):
        nans = np.isnan(arr)
        if nans.all():
            return np.zeros_like(arr)
        if not nans.any():
            return arr
        x = np.arange(len(arr))
        arr[nans] = np.interp(x[nans], x[~nans], arr[~nans])
        return arr

    def summarize(self, metrics):
        """汇总每个指标的统计量"""
        summary = {}
        for key, arr in metrics.items():
            if len(arr) == 0:
                continue
            summary[key] = {
                "mean": float(np.mean(arr)),
                "std": float(np.std(arr)),
                "min": float(np.min(arr)),
                "max": float(np.max(arr)),
                "median": float(np.median(arr)),
            }
        return summary
