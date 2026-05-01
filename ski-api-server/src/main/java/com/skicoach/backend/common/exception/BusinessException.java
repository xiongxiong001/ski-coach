package com.skicoach.backend.common.exception;

import com.skicoach.backend.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常
 *
 * 业务层抛出此异常,会被全局异常处理器捕获,
 * 转换成统一的 ApiResult 错误响应。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;
    private final String message;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    public BusinessException(ResultCode resultCode, String customMessage) {
        super(customMessage);
        this.code = resultCode.getCode();
        this.message = customMessage;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}
