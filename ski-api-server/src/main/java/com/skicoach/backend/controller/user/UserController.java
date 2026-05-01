package com.skicoach.backend.controller.user;

import com.skicoach.backend.common.result.ApiResult;
import com.skicoach.backend.common.util.SecurityUtil;
import com.skicoach.backend.dto.user.UpdateProfileRequest;
import com.skicoach.backend.dto.user.UserProfileVO;
import com.skicoach.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户资料接口
 * - GET  /api/user/profile  获取当前用户资料
 * - PUT  /api/user/profile  修改昵称
 *
 * 所有接口都需要 Authorization: Bearer xxx
 */
@Tag(name = "用户资料")
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取当前用户资料")
    @GetMapping("/profile")
    public ApiResult<UserProfileVO> getProfile() {
        Long userId = SecurityUtil.getUserIdOrThrow();
        UserProfileVO vo = userService.getProfile(userId);
        return ApiResult.success(vo);
    }

    @Operation(summary = "修改用户资料(昵称)")
    @PutMapping("/profile")
    public ApiResult<Void> updateProfile(@RequestBody @Valid UpdateProfileRequest request) {
        Long userId = SecurityUtil.getUserIdOrThrow();
        userService.updateProfile(userId, request);
        return ApiResult.success();
    }
}
