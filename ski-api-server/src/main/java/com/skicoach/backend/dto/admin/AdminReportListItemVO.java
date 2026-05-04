// 单次报告列表项 VO
package com.skicoach.backend.dto.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminReportListItemVO {
    private Long id;
    private Long userId;
    private String phone;           // 用户手机号
    private String nickname;        // 用户昵称
    private Long videoId;
    private String originalFilename; // 视频文件名
    private String reportSummary;   // 报告摘要(前200字)
    private BigDecimal llmCostYuan;
    private Integer llmInputTokens;
    private Integer llmOutputTokens;
    private LocalDateTime createdTime;
}
