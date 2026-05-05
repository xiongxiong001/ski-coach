package com.skicoach.backend.dto.feedback;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "提交反馈请求")
public class FeedbackCreateRequest {

    @Schema(description = "反馈类型", example = "bug", allowableValues = {"bug", "feature", "performance", "other"})
    @NotBlank(message = "反馈类型不能为空")
    private String type;

    @Schema(description = "反馈内容", example = "视频上传后一直显示分析中...")
    @NotBlank(message = "反馈内容不能为空")
    private String content;

    @Schema(description = "联系方式(选填)", example = "13800138000")
    private String contact;

    @Schema(description = "APP版本号", example = "1.0.0")
    private String appVersion;
}
