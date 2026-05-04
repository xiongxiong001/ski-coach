// 对比报告详情 VO
package com.skicoach.backend.dto.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminComparisonDetailVO {
    private Long id;
    private Long taskId;
    private Long userId;
    private String phone;
    private String nickname;
    private Long prevVideoId;
    private Long currVideoId;
    private String prevFilename;
    private String currFilename;
    private String comparisonDataJson;
    private String reportMarkdown;
    private Integer improvedCount;
    private Integer declinedCount;
    private Integer stabilityImprovedCount;
    private BigDecimal llmCostYuan;
    private Integer llmInputTokens;
    private Integer llmOutputTokens;
    private LocalDateTime createdTime;
}
