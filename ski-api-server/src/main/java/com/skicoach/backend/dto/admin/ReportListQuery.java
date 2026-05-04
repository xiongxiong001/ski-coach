package com.skicoach.backend.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "管理后台-报告列表查询")
public class ReportListQuery {

    @Min(1) private Long pageNum = 1L;

    @Min(1) @Max(100) private Long pageSize = 20L;

    @Schema(description = "用户ID过滤")
    private Long userId;

    @Schema(description = "用户手机号(模糊搜索)")
    private String userPhone;
}
