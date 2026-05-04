// 对比报告列表项 VO
package com.skicoach.backend.dto.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminComparisonListItemVO {
    private Long id;
    private Long userId;
    private String phone;
    private String nickname;
    private Long prevVideoId;
    private Long currVideoId;
    private String prevFilename;    // 上次视频文件名
    private String currFilename;    // 本次视频文件名
    private String reportSummary;   // 报告摘要
    private Integer improvedCount;  // 进步指标数
    private Integer declinedCount;  // 退步指标数
    private BigDecimal llmCostYuan;
    private LocalDateTime createdTime;
}
