package com.skicoach.backend.controller.auth;

import com.skicoach.backend.common.result.ApiResult;
import com.skicoach.backend.dto.auth.LoginRequest;
import com.skicoach.backend.dto.auth.LoginResponse;
import com.skicoach.backend.dto.auth.RegisterRequest;
import com.skicoach.backend.dto.auth.SendSmsCodeRequest;
import com.skicoach.backend.dto.auth.SmsLoginRequest;
import com.skicoach.backend.interceptor.JwtInterceptor;
import com.skicoach.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口
 * - POST /api/auth/register        注册
 * - POST /api/auth/login           登录
 * - POST /api/auth/send-sms-code   发送短信验证码
 * - POST /api/auth/sms-login       验证码登录/注册
 * - POST /api/auth/logout          登出(需要登录)
 */
@Tag(name = "认证", description = "注册、登录、登出")
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "用户注册", description = "手机号+密码注册,成功直接返回登录Token")
    @PostMapping("/register")
    public ApiResult<LoginResponse> register(@RequestBody @Valid RegisterRequest request) {
        LoginResponse resp = userService.register(request);
        return ApiResult.success(resp);
    }

    @Operation(summary = "发送短信验证码", description = "向指定手机号发送6位数字验证码,60秒内不可重复发送")
    @PostMapping("/send-sms-code")
    public ApiResult<Void> sendSmsCode(@RequestBody @Valid SendSmsCodeRequest request) {
        userService.sendSmsCode(request.getPhone());
        return ApiResult.success();
    }

    @Operation(summary = "验证码登录/注册", description = "验证码校验通过后,已注册用户直接登录,新用户自动注册并登录")
    @PostMapping("/sms-login")
    public ApiResult<LoginResponse> smsLogin(@RequestBody @Valid SmsLoginRequest request) {
        LoginResponse resp = userService.smsLogin(request);
        return ApiResult.success(resp);
    }

    @Operation(summary = "用户登录", description = "手机号+密码登录,成功返回JWT Token")
    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse resp = userService.login(request);
        return ApiResult.success(resp);
    }

    @Operation(summary = "用户登出", description = "把当前Token加入黑名单(需登录)")
    @PostMapping("/logout")
    public ApiResult<Void> logout(HttpServletRequest request) {
        // 从请求头里取出Token
        String authHeader = request.getHeader(JwtInterceptor.AUTH_HEADER);
        if (authHeader != null && authHeader.startsWith(JwtInterceptor.BEARER_PREFIX)) {
            String token = authHeader.substring(JwtInterceptor.BEARER_PREFIX.length()).trim();
            userService.logout(token);
        }
        return ApiResult.success();
    }
}
