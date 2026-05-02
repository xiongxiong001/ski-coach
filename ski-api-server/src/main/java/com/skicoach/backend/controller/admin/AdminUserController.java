package com.skicoach.backend.controller.admin;

import com.skicoach.backend.common.result.ApiResult;
import com.skicoach.backend.common.result.PageResult;
import com.skicoach.backend.dto.admin.AdminUserVO;
import com.skicoach.backend.dto.admin.UpdateUserStatusRequest;
import com.skicoach.backend.dto.admin.UserListQuery;
import com.skicoach.backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理后台-用户管理")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminService adminService;

    @Operation(summary = "用户列表(支持手机号搜索、状态筛选)")
    @GetMapping
    public ApiResult<PageResult<AdminUserVO>> list(@Valid UserListQuery query) {
        return ApiResult.success(adminService.listUsers(query));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    public ApiResult<AdminUserVO> detail(@PathVariable("id") Long id) {
        return ApiResult.success(adminService.getUserDetail(id));
    }

    @Operation(summary = "启用/封禁用户")
    @PutMapping("/{id}/status")
    public ApiResult<Void> updateStatus(@PathVariable("id") Long id,
                                         @RequestBody @Valid UpdateUserStatusRequest request) {
        adminService.updateUserStatus(id, request.getStatus());
        return ApiResult.success();
    }
}
