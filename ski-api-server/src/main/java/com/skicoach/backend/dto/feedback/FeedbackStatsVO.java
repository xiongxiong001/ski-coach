package com.skicoach.backend.dto.feedback;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "反馈统计")
public class FeedbackStatsVO {

    @Schema(description = "反馈总数")
    private Long totalCount;

    @Schema(description = "待处理数量")
    private Long pendingCount;

    @Schema(description = "已查看数量")
    private Long viewedCount;

    @Schema(description = "已回复数量")
    private Long repliedCount;
}
