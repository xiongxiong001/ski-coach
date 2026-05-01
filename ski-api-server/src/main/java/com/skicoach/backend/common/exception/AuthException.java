package com.skicoach.backend.common.exception;

import com.skicoach.backend.common.result.ResultCode;
import lombok.Getter;

/**
 * 鉴权异常(JWT校验失败、未登录等)
 */
@Getter
public class AuthException extends RuntimeException {

    private final Integer code;
    private final String message;

    public AuthException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    public AuthException(ResultCode resultCode, String customMessage) {
        super(customMessage);
        this.code = resultCode.getCode();
        this.message = customMessage;
    }
}
