package com.skicoach.backend.dto.feedback;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "反馈详情")
public class FeedbackVO {

    @Schema(description = "反馈ID")
    private Long id;

    @Schema(description = "反馈类型")
    private String type;

    @Schema(description = "反馈内容")
    private String content;

    @Schema(description = "联系方式")
    private String contact;

    @Schema(description = "图片数量")
    private Integer imageCount;

    @Schema(description = "APP版本号")
    private String appVersion;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "官方回复")
    private String reply;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
}
