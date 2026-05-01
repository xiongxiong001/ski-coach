package com.skicoach.backend.common.enums;

import lombok.Getter;

/**
 * 任务执行状态
 */
@Getter
public enum TaskStatusEnum {

    PENDING("pending", "等待执行"),
    RUNNING("running", "执行中"),
    SUCCESS("success", "执行成功"),
    FAILED("failed", "执行失败");

    private final String value;
    private final String description;

    TaskStatusEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
