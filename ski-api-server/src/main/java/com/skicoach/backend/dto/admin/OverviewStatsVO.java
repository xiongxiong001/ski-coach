package com.skicoach.backend.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "总览统计")
public class OverviewStatsVO {

    @Schema(description = "总用户数")
    private Long totalUsers;

    @Schema(description = "今日新增用户数")
    private Long newUsersToday;

    @Schema(description = "总视频数")
    private Long totalVideos;

    @Schema(description = "今日上传视频数")
    private Long videosToday;

    @Schema(description = "总报告数(单次+对比)")
    private Long totalReports;

    @Schema(description = "今日生成报告数")
    private Long reportsToday;

    @Schema(description = "进行中任务数")
    private Long runningTasks;

    @Schema(description = "失败任务数")
    private Long failedTasks;

    @Schema(description = "今日 LLM 调用花费(元)")
    private BigDecimal llmCostToday;

    @Schema(description = "累计 LLM 调用花费(元)")
    private BigDecimal llmCostTotal;
}
