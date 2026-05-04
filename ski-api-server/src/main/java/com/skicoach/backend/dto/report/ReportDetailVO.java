package com.skicoach.backend.dto.report;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "教练报告详情")
public class ReportDetailVO {

    @Schema(description = "报告ID")
    private Long id;

    @Schema(description = "关联的视频ID")
    private Long videoId;

    @Schema(description = "关联的视频文件名")
    private String videoFilename;

    @Schema(description = "中文教练报告(Markdown)")
    private String reportMarkdown;

    @Schema(description = "生成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
}
