package com.skicoach.backend.common.enums;

import lombok.Getter;

/**
 * 分析任务类型
 */
@Getter
public enum TaskTypeEnum {

    SINGLE("single", "单次分析"),
    COMPARISON("comparison", "对比分析");

    private final String value;
    private final String description;

    TaskTypeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
