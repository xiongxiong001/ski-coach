package com.skicoach.backend.common.exception;

import com.skicoach.backend.common.result.ApiResult;
import com.skicoach.backend.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理器
 *
 * 所有Controller抛出的异常,统一在这里处理,转换成ApiResult格式。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常 - 主动抛出的可预期异常
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResult<Void> handleBusiness(BusinessException e, HttpServletRequest request) {
        log.warn("[业务异常] {} - {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return ApiResult.error(e.getCode(), e.getMessage());
    }

    /**
     * 鉴权异常 - JWT校验失败
     */
    @ExceptionHandler(AuthException.class)
    public ApiResult<Void> handleAuth(AuthException e, HttpServletRequest request) {
        log.warn("[鉴权异常] {} - {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return ApiResult.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常 - @Valid 注解校验失败(@RequestBody场景)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError != null
                ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                : "参数校验失败";
        log.warn("[参数校验失败] {}", msg);
        return ApiResult.error(ResultCode.PARAM_ERROR, msg);
    }

    /**
     * 参数校验异常 - 表单/查询参数场景
     */
    @ExceptionHandler(BindException.class)
    public ApiResult<Void> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError != null
                ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                : "参数校验失败";
        log.warn("[参数绑定失败] {}", msg);
        return ApiResult.error(ResultCode.PARAM_ERROR, msg);
    }

    /**
     * 缺少必需的请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResult<Void> handleMissingParam(MissingServletRequestParameterException e) {
        String msg = "缺少必需参数: " + e.getParameterName();
        log.warn("[参数缺失] {}", msg);
        return ApiResult.error(ResultCode.PARAM_MISSING, msg);
    }

    /**
     * JSON 解析失败
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResult<Void> handleJsonParseError(HttpMessageNotReadableException e) {
        log.warn("[JSON解析失败] {}", e.getMessage());
        return ApiResult.error(ResultCode.PARAM_INVALID, "请求体格式错误,无法解析JSON");
    }

    /**
     * 不支持的HTTP方法
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResult<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        String msg = "不支持的HTTP方法: " + e.getMethod();
        log.warn("[Method不支持] {}", msg);
        return ApiResult.error(ResultCode.PARAM_ERROR, msg);
    }

    /**
     * 文件上传超过大小限制
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResult<Void> handleFileSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn("[文件过大] {}", e.getMessage());
        return ApiResult.error(ResultCode.FILE_TOO_LARGE);
    }

    /**
     * 兜底:任何未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleUnknown(Exception e, HttpServletRequest request) {
        log.error("[未捕获异常] {} - {}: ", request.getMethod(), request.getRequestURI(), e);
        return ApiResult.error(ResultCode.SYSTEM_ERROR, "系统异常,请稍后重试");
    }
}
