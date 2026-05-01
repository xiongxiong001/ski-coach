package com.skicoach.backend.common.enums;

import lombok.Getter;

/**
 * 视频分析状态
 */
@Getter
public enum VideoStatusEnum {

    PENDING("pending", "等待分析"),
    ANALYZING("analyzing", "分析中"),
    ANALYZED("analyzed", "已完成"),
    FAILED("failed", "分析失败");

    private final String value;
    private final String description;

    VideoStatusEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static VideoStatusEnum of(String value) {
        for (VideoStatusEnum e : values()) {
            if (e.value.equals(value)) {
                return e;
            }
        }
        return null;
    }
}
