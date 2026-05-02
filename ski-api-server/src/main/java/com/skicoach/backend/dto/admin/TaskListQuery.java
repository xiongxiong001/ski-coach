package com.skicoach.backend.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "任务列表查询(管理后台)")
public class TaskListQuery {

    @Min(1) private Long pageNum = 1L;

    @Min(1) @Max(100) private Long pageSize = 20L;

    @Schema(description = "任务类型: single / comparison")
    private String taskType;

    @Schema(description = "状态: pending/running/success/failed")
    private String status;

    @Schema(description = "用户ID过滤")
    private Long userId;
}
