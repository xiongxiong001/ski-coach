package com.skicoach.backend.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "LLM 成本统计")
public class LlmCostStatsVO {

    @Schema(description = "任务类型")
    private String taskType;

    @Schema(description = "调用次数")
    private Long callCount;

    @Schema(description = "总输入 tokens")
    private Long totalInputTokens;

    @Schema(description = "总输出 tokens")
    private Long totalOutputTokens;

    @Schema(description = "总花费(元)")
    private BigDecimal totalCost;

    @Schema(description = "平均单次花费(元)")
    private BigDecimal avgCost;
}
