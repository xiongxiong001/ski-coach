// 单次报告详情 VO
package com.skicoach.backend.dto.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminReportDetailVO {
    private Long id;
    private Long taskId;
    private Long videoId;
    private Long userId;
    private String phone;
    private String nickname;
    private String originalFilename;
    private String fileMd5;
    private String reportMarkdown;  // 完整报告内容
    private BigDecimal llmCostYuan;
    private Integer llmInputTokens;
    private Integer llmOutputTokens;
    private LocalDateTime createdTime;
}
