"""
动作分割模块
基于规则法把视频切分成"转弯/直行/跳跃"等动作单元
"""
import numpy as np

from app.core.utils import smooth_signal


class ActionSegmenter:
    def __init__(self, metrics, fps):
        self.metrics = metrics
        self.fps = fps

    def segment(self):
        """切分动作单元"""
        separation = self.metrics["shoulder_hip_separation"]
        torso_lean = self.metrics["torso_lean"]

        sep_smooth = smooth_signal(separation, window_size=int(self.fps * 0.5))
        lean_smooth = smooth_signal(torso_lean, window_size=int(self.fps * 0.5))

        turn_threshold = 15.0
        segments = []
        current_state = None
        current_start = 0

        for i in range(len(sep_smooth)):
            if sep_smooth[i] > turn_threshold:
                state = "turn_left" if lean_smooth[i] < 0 else "turn_right"
            else:
                state = "straight"

            if state != current_state:
                if current_state is not None:
                    segments.append({
                        "type": current_state,
                        "start_frame": current_start,
                        "end_frame": i - 1,
                        "start_time": current_start / self.fps,
                        "end_time": (i - 1) / self.fps,
                    })
                current_state = state
                current_start = i

        # 收尾
        if current_state is not None:
            segments.append({
                "type": current_state,
                "start_frame": current_start,
                "end_frame": len(sep_smooth) - 1,
                "start_time": current_start / self.fps,
                "end_time": (len(sep_smooth) - 1) / self.fps,
            })

        # 合并过短片段
        segments = self._merge_short_segments(segments, min_duration=0.5)

        # 为每个片段计算指标
        for seg in segments:
            seg["metrics"] = self._segment_metrics(seg["start_frame"], seg["end_frame"])

        return segments

    def _merge_short_segments(self, segments, min_duration=0.5):
        if len(segments) <= 1:
            return segments
        merged = []
        for seg in segments:
            duration = seg["end_time"] - seg["start_time"]
            if duration < min_duration and merged:
                merged[-1]["end_frame"] = seg["end_frame"]
                merged[-1]["end_time"] = seg["end_time"]
            else:
                merged.append(seg)
        return merged

    def _segment_metrics(self, start, end):
        result = {}
        for key, arr in self.metrics.items():
            data = arr[start:end + 1]
            if len(data) == 0:
                continue
            result[key] = {
                "mean": float(np.mean(data)),
                "std": float(np.std(data)),
                "min": float(np.min(data)),
                "max": float(np.max(data)),
            }
        return result

    def count_actions(self, segments):
        counts = {"turn_left": 0, "turn_right": 0, "straight": 0, "jump": 0}
        for seg in segments:
            counts[seg["type"]] = counts.get(seg["type"], 0) + 1
        return counts
