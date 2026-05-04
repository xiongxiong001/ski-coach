package com.skicoach.backend.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "管理后台-对比报告详情")
public class AdminComparisonDetailVO {

    @Schema(description = "对比报告ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户手机号")
    private String userPhone;

    @Schema(description = "用户昵称")
    private String userNickname;

    @Schema(description = "上次视频ID")
    private Long prevVideoId;

    @Schema(description = "上次视频文件名")
    private String prevVideoFilename;

    @Schema(description = "本次视频ID")
    private Long currVideoId;

    @Schema(description = "本次视频文件名")
    private String currVideoFilename;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "完整对比报告(Markdown)")
    private String reportMarkdown;

    @Schema(description = "差异数据 JSON 字符串")
    private String comparisonDataJson;

    @Schema(description = "进步指标数")
    private Integer improvedCount;

    @Schema(description = "退步指标数")
    private Integer declinedCount;

    @Schema(description = "稳定性提升数")
    private Integer stabilityImprovedCount;

    @Schema(description = "LLM 花费(元)")
    private BigDecimal llmCostYuan;

    @Schema(description = "输入 tokens")
    private Integer llmInputTokens;

    @Schema(description = "输出 tokens")
    private Integer llmOutputTokens;

    @Schema(description = "生成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
}
