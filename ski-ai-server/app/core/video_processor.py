"""
视频处理模块
读取视频,逐帧用MediaPipe提取人体姿态关键点
"""
import cv2
import mediapipe as mp
import os

from app.core.utils import landmarks_to_array
from app.logger import get_logger

logger = get_logger(__name__)


class VideoProcessor:
    def __init__(self, model_complexity: int = 1, min_detection_confidence: float = 0.5):
        self.mp_pose = mp.solutions.pose
        self.pose = self.mp_pose.Pose(
            static_image_mode=False,
            model_complexity=model_complexity,
            smooth_landmarks=True,
            min_detection_confidence=min_detection_confidence,
            min_tracking_confidence=0.5,
        )

    def process_video(self, video_path: str) -> dict:
        """
        处理一个视频,提取每一帧的姿态关键点

        参数:
            video_path: 视频文件的绝对路径

        返回:
            dict: {
                "keypoints_per_frame": list,  # 每帧的关键点numpy数组(可能为None)
                "fps": float,
                "frame_count": int,
                "width": int,
                "height": int,
                "detection_rate": float,
            }
        """
        if not os.path.exists(video_path):
            raise FileNotFoundError(f"视频文件不存在: {video_path}")

        cap = cv2.VideoCapture(video_path)
        if not cap.isOpened():
            raise RuntimeError(f"无法打开视频: {video_path}")

        # 视频元信息
        fps = cap.get(cv2.CAP_PROP_FPS)
        frame_count = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))

        logger.info(
            f"开始处理视频: {video_path}, "
            f"分辨率={width}x{height}, fps={fps:.2f}, 总帧数={frame_count}, "
            f"时长={frame_count / fps:.1f}秒"
        )

        keypoints_per_frame = []
        detected_frames = 0
        processed_frames = 0

        while cap.isOpened():
            ret, frame = cap.read()
            if not ret:
                break

            rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            results = self.pose.process(rgb_frame)
            kp_array = landmarks_to_array(results.pose_landmarks)
            keypoints_per_frame.append(kp_array)

            if kp_array is not None:
                detected_frames += 1
            processed_frames += 1

            # 每100帧打一次日志
            if processed_frames % 100 == 0:
                logger.info(f"已处理 {processed_frames}/{frame_count} 帧")

        cap.release()

        detection_rate = detected_frames / frame_count if frame_count > 0 else 0
        logger.info(
            f"视频处理完成: 检测到 {detected_frames}/{frame_count} 帧 "
            f"(检测率 {detection_rate * 100:.1f}%)"
        )

        return {
            "keypoints_per_frame": keypoints_per_frame,
            "fps": fps,
            "frame_count": frame_count,
            "width": width,
            "height": height,
            "detection_rate": detection_rate,
        }

    def close(self):
        """释放MediaPipe资源"""
        self.pose.close()
