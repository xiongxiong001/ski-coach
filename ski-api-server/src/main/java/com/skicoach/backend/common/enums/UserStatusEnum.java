package com.skicoach.backend.common.enums;

import lombok.Getter;

/**
 * 用户/管理员状态
 */
@Getter
public enum UserStatusEnum {

    DISABLED(0, "禁用"),
    NORMAL(1, "正常");

    private final Integer value;
    private final String description;

    UserStatusEnum(Integer value, String description) {
        this.value = value;
        this.description = description;
    }
}
