package com.skicoach.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 分析任务实体(对应表 analysis_tasks)
 *
 * 同时承载 single 和 comparison 两种任务,通过 task_type 区分。
 */
@Data
@TableName("analysis_tasks")
public class AnalysisTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 任务类型: single / comparison */
    private String taskType;

    /** 单次任务用 */
    private Long videoId;

    /** 对比任务用 */
    private Long prevVideoId;
    private Long currVideoId;

    /** 任务状态: pending / running / success / failed */
    private String status;

    private String errorMessage;

    private Integer retryCount;

    /** LLM 调用成本统计 */
    private BigDecimal llmCostYuan;
    private Integer llmInputTokens;
    private Integer llmOutputTokens;

    /** 任务成功后填充: 关联的报告ID(reports.id 或 comparison_reports.id) */
    private Long reportId;

    /** 任务开始执行时间 */
    private LocalDateTime startTime;

    /** 任务结束时间 */
    private LocalDateTime finishTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
