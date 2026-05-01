package com.skicoach.backend.common.result;

import lombok.Data;
import lombok.Getter;

/**
 * 统一错误码
 *
 * 错误码约定:
 *   0       = 成功
 *   4xxx    = 客户端错误(参数、鉴权等)
 *   5xxx    = 服务端错误(系统异常、业务异常)
 *   6xxx    = 第三方服务错误(AI服务、LLM等)
 */
@Getter
public enum ResultCode {

    SUCCESS(0, "success"),

    // ============== 4xxx 客户端错误 ==============
    PARAM_ERROR(4001, "参数错误"),
    PARAM_MISSING(4002, "缺少必需参数"),
    PARAM_INVALID(4003, "参数格式不正确"),
    FILE_NOT_FOUND(4004, "文件不存在"),
    FILE_TOO_LARGE(4005, "文件过大"),
    FILE_TYPE_NOT_ALLOWED(4006, "不允许的文件类型"),

    // 鉴权
    UNAUTHORIZED(4010, "未登录或登录已过期"),
    TOKEN_INVALID(4011, "Token无效"),
    TOKEN_EXPIRED(4012, "Token已过期"),
    FORBIDDEN(4013, "无访问权限"),

    // 业务-用户
    USER_NOT_FOUND(4101, "用户不存在"),
    USER_DISABLED(4102, "账号已被禁用"),
    USER_PHONE_EXISTS(4103, "手机号已注册"),
    USER_PASSWORD_WRONG(4104, "密码错误"),

    // 业务-视频
    VIDEO_NOT_FOUND(4201, "视频不存在"),
    VIDEO_NOT_OWNED(4202, "视频不属于当前用户"),
    VIDEO_NOT_ANALYZED(4203, "视频还未完成分析"),
    VIDEO_DURATION_TOO_LONG(4204, "视频时长超过限制"),

    // 业务-报告
    REPORT_NOT_FOUND(4301, "报告不存在"),
    COMPARISON_ALREADY_EXISTS(4302, "对比报告已存在"),

    // ============== 5xxx 服务端错误 ==============
    SYSTEM_ERROR(5000, "系统异常"),
    BUSINESS_ERROR(5001, "业务异常"),
    DATABASE_ERROR(5002, "数据库异常"),
    REDIS_ERROR(5003, "缓存异常"),

    // ============== 6xxx 第三方服务错误 ==============
    AI_SERVICE_ERROR(6001, "AI服务调用失败"),
    AI_SERVICE_TIMEOUT(6002, "AI服务调用超时"),
    LLM_ERROR(6003, "LLM服务异常");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
