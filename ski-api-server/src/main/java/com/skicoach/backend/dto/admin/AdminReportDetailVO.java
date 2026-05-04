package com.skicoach.backend.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端单次报告详情(完整 Markdown + 用户/视频上下文)
 */
@Data
@Schema(description = "管理后台-单次报告详情")
public class AdminReportDetailVO {

    @Schema(description = "报告ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户手机号")
    private String userPhone;

    @Schema(description = "用户昵称")
    private String userNickname;

    @Schema(description = "视频ID")
    private Long videoId;

    @Schema(description = "视频原始文件名")
    private String videoFilename;

    @Schema(description = "视频检测率")
    private BigDecimal videoDetectionRate;

    @Schema(description = "视频时长(秒)")
    private BigDecimal videoDurationSeconds;

    @Schema(description = "左转次数")
    private Integer turnLeftCount;

    @Schema(description = "右转次数")
    private Integer turnRightCount;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "完整教练报告(Markdown)")
    private String reportMarkdown;

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
