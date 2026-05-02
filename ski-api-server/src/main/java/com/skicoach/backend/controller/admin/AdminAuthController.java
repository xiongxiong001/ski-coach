package com.skicoach.backend.controller.admin;

import com.skicoach.backend.common.result.ApiResult;
import com.skicoach.backend.dto.admin.AdminLoginRequest;
import com.skicoach.backend.dto.admin.AdminLoginResponse;
import com.skicoach.backend.interceptor.AdminAuthInterceptor;
import com.skicoach.backend.service.AdminService;
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

@Tag(name = "管理后台-认证")
@Slf4j
@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminService adminService;

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public ApiResult<AdminLoginResponse> login(@RequestBody @Valid AdminLoginRequest request) {
        return ApiResult.success(adminService.login(request));
    }

    @Operation(summary = "管理员登出")
    @PostMapping("/logout")
    public ApiResult<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader(AdminAuthInterceptor.AUTH_HEADER);
        if (authHeader != null && authHeader.startsWith(AdminAuthInterceptor.BEARER_PREFIX)) {
            String token = authHeader.substring(AdminAuthInterceptor.BEARER_PREFIX.length()).trim();
            adminService.logout(token);
        }
        return ApiResult.success();
    }
}
