"""
几何计算工具
MediaPipe关键点常量、向量/角度计算、信号平滑等
"""
import numpy as np


class KP:
    """MediaPipe Pose 33个关键点索引常量"""
    NOSE = 0
    LEFT_SHOULDER = 11
    RIGHT_SHOULDER = 12
    LEFT_ELBOW = 13
    RIGHT_ELBOW = 14
    LEFT_WRIST = 15
    RIGHT_WRIST = 16
    LEFT_HIP = 23
    RIGHT_HIP = 24
    LEFT_KNEE = 25
    RIGHT_KNEE = 26
    LEFT_ANKLE = 27
    RIGHT_ANKLE = 28
    LEFT_HEEL = 29
    RIGHT_HEEL = 30
    LEFT_FOOT_INDEX = 31
    RIGHT_FOOT_INDEX = 32


def landmarks_to_array(landmarks):
    """把MediaPipe的landmark对象转成 (33, 4) numpy数组"""
    if landmarks is None:
        return None
    arr = np.zeros((33, 4), dtype=np.float32)
    for i, lm in enumerate(landmarks.landmark):
        arr[i] = [lm.x, lm.y, lm.z, lm.visibility]
    return arr


def calc_angle_2d(p1, p2, p3):
    """计算 p2 处由 p1-p2-p3 构成的夹角(度)"""
    p1 = np.array(p1[:2])
    p2 = np.array(p2[:2])
    p3 = np.array(p3[:2])
    v1 = p1 - p2
    v2 = p3 - p2
    cos_angle = np.dot(v1, v2) / (np.linalg.norm(v1) * np.linalg.norm(v2) + 1e-8)
    cos_angle = np.clip(cos_angle, -1.0, 1.0)
    return float(np.degrees(np.arccos(cos_angle)))


def calc_line_angle(p1, p2):
    """计算两点连线相对水平方向的角度(度)"""
    p1 = np.array(p1[:2])
    p2 = np.array(p2[:2])
    delta = p2 - p1
    return float(np.degrees(np.arctan2(delta[1], delta[0])))


def midpoint(p1, p2):
    return ((p1[0] + p2[0]) / 2, (p1[1] + p2[1]) / 2)


def smooth_signal(signal, window_size=5):
    """滑动平均平滑"""
    signal = np.asarray(signal, dtype=np.float32)
    if len(signal) < window_size:
        return signal
    kernel = np.ones(window_size) / window_size
    return np.convolve(signal, kernel, mode='same')


def safe_divide(a, b, default=0.0):
    if abs(b) < 1e-8:
        return default
    return a / b
