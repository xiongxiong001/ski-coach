package com.skicoach.backend.controller.admin;

import com.skicoach.backend.common.result.ApiResult;
import com.skicoach.backend.common.result.PageResult;
import com.skicoach.backend.dto.admin.TaskListItemVO;
import com.skicoach.backend.dto.admin.TaskListQuery;
import com.skicoach.backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理后台-任务管理")
@RestController
@RequestMapping("/admin/tasks")
@RequiredArgsConstructor
public class AdminTaskController {

    private final AdminService adminService;

    @Operation(summary = "任务列表")
    @GetMapping
    public ApiResult<PageResult<TaskListItemVO>> list(@Valid TaskListQuery query) {
        return ApiResult.success(adminService.listTasks(query));
    }

    @Operation(summary = "任务详情")
    @GetMapping("/{id}")
    public ApiResult<TaskListItemVO> detail(@PathVariable("id") Long id) {
        return ApiResult.success(adminService.getTaskDetail(id));
    }

    @Operation(summary = "重试失败任务")
    @PostMapping("/{id}/retry")
    public ApiResult<Void> retry(@PathVariable("id") Long id) {
        adminService.retryTask(id);
        return ApiResult.success();
    }
}
