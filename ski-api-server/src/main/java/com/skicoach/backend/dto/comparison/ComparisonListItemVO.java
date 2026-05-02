package com.skicoach.backend.dto.comparison;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对比报告列表项 (不含 reportMarkdown 和 JSON,数据量小)
 */
@Data
@Schema(description = "对比报告列表项")
public class ComparisonListItemVO {

    private Long id;

    private Long prevVideoId;

    private Long currVideoId;

    private Integer improvedCount;

    private Integer declinedCount;

    private Integer stabilityImprovedCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
}
