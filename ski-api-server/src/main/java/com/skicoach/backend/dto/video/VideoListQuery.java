package com.skicoach.backend.dto.video;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "视频列表查询参数")
public class VideoListQuery {

    @Schema(description = "页码,从1开始", defaultValue = "1")
    @Min(value = 1, message = "页码必须>=1")
    private Long pageNum = 1L;

    @Schema(description = "每页条数,最大50", defaultValue = "10")
    @Min(value = 1, message = "每页条数必须>=1")
    @Max(value = 50, message = "每页条数最大50")
    private Long pageSize = 10L;

    @Schema(description = "分析状态过滤(可选): pending/analyzing/analyzed/failed")
    private String analysisStatus;
}
