package com.skicoach.backend.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "每日统计")
public class DailyStatsVO {

    @Schema(description = "日期")
    private LocalDate statDate;

    @Schema(description = "新增用户数")
    private Long newUsers;

    @Schema(description = "上传视频数")
    private Long videoCount;

    @Schema(description = "成功的分析任务数")
    private Long taskSuccess;

    @Schema(description = "失败的分析任务数")
    private Long taskFailed;

    @Schema(description = "LLM 总调用花费")
    private BigDecimal llmCost;
}
