package com.skicoach.backend.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "管理后台-任务列表项")
public class TaskListItemVO {

    private Long id;

    private Long userId;

    private String taskType;

    private String status;

    private Long videoId;

    private Long prevVideoId;

    private Long currVideoId;

    private Long reportId;

    private String errorMessage;

    private BigDecimal llmCostYuan;

    private Integer llmInputTokens;

    private Integer llmOutputTokens;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
}
