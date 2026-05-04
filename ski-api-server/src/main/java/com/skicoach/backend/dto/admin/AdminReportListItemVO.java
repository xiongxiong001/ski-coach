package com.skicoach.backend.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端单次报告列表项
 * 不含完整 Markdown,只有摘要(前 200 字)
 */
@Data
@Schema(description = "管理后台-单次报告列表项")
public class AdminReportListItemVO {

    @Schema(description = "报告ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户手机号(脱敏)")
    private String userPhone;

    @Schema(description = "用户昵称")
    private String userNickname;

    @Schema(description = "视频ID")
    private Long videoId;

    @Schema(description = "视频文件名")
    private String videoFilename;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "报告摘要(前200字)")
    private String reportPreview;

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
