package com.skicoach.backend.controller.user;

import com.skicoach.backend.common.result.ApiResult;
import com.skicoach.backend.common.util.SecurityUtil;
import com.skicoach.backend.dto.task.TaskStatusVO;
import com.skicoach.backend.entity.AnalysisTask;
import com.skicoach.backend.service.AnalysisTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务状态接口
 *
 * 主要供前端轮询使用——上传视频后,前端每3-5秒调一次,直到 status 变成 success/failed
 */
@Tag(name = "任务状态")
@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final AnalysisTaskService taskService;

    @Operation(summary = "查询任务状态", description = "供前端轮询任务进度")
    @GetMapping("/{id}")
    public ApiResult<TaskStatusVO> getStatus(@PathVariable("id") Long id) {
        Long userId = SecurityUtil.getUserIdOrThrow();
        AnalysisTask task = taskService.getOwnedTaskOrThrow(userId, id);
        TaskStatusVO vo = new TaskStatusVO();
        BeanUtils.copyProperties(task, vo);
        return ApiResult.success(vo);
    }
}
