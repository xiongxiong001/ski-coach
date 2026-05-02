package com.skicoach.backend.dto.comparison;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "对比报告详情")
public class ComparisonReportVO {

    @Schema(description = "对比报告ID")
    private Long id;

    @Schema(description = "上次视频ID")
    private Long prevVideoId;

    @Schema(description = "本次视频ID")
    private Long currVideoId;

    @Schema(description = "中文对比报告(Markdown)")
    private String reportMarkdown;

    @Schema(description = "差异数据JSON")
    private String comparisonDataJson;

    @Schema(description = "进步指标数")
    private Integer improvedCount;

    @Schema(description = "退步指标数")
    private Integer declinedCount;

    @Schema(description = "稳定性提升数")
    private Integer stabilityImprovedCount;

    @Schema(description = "生成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
}
