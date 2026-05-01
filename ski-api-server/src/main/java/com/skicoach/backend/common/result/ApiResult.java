package com.skicoach.backend.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 统一API响应包装
 *
 * 格式:
 * {
 *   "code": 0,
 *   "message": "success",
 *   "data": { ... },
 *   "timestamp": 1714521600000
 * }
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "统一响应包装")
public class ApiResult<T> {

    @Schema(description = "状态码,0=成功,非0=失败")
    private Integer code;

    @Schema(description = "提示信息")
    private String message;

    @Schema(description = "数据负载")
    private T data;

    @Schema(description = "服务端时间戳(毫秒)")
    private Long timestamp;

    public ApiResult() {
        this.timestamp = System.currentTimeMillis();
    }

    public ApiResult(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    // ----------- 成功 -----------
    public static <T> ApiResult<T> success() {
        return new ApiResult<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> ApiResult<T> success(String message, T data) {
        return new ApiResult<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    // ----------- 失败 -----------
    public static <T> ApiResult<T> error(ResultCode resultCode) {
        return new ApiResult<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> ApiResult<T> error(ResultCode resultCode, String customMessage) {
        return new ApiResult<>(resultCode.getCode(), customMessage, null);
    }

    public static <T> ApiResult<T> error(Integer code, String message) {
        return new ApiResult<>(code, message, null);
    }

    /** 判断是否成功 */
    public boolean isSuccess() {
        return code != null && code == ResultCode.SUCCESS.getCode();
    }
}
