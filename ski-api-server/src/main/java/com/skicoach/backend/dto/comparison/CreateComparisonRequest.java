package com.skicoach.backend.dto.comparison;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建对比报告请求")
public class CreateComparisonRequest {

    @Schema(description = "上次的视频ID(对比基准)", example = "1")
    @NotNull(message = "上次视频ID不能为空")
    private Long prevVideoId;

    @Schema(description = "本次的视频ID(对比对象)", example = "2")
    @NotNull(message = "本次视频ID不能为空")
    private Long currVideoId;
}
