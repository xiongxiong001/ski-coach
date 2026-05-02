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
 * 对比教练报告(对应表 comparison_reports)
 *
 * 表上有 UNIQUE KEY(prev_video_id, curr_video_id),保证同一对视频只生成一份对比报告。
 */
@Data
@TableName("comparison_reports")
public class ComparisonReport implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long userId;

    private Long prevVideoId;

    private Long currVideoId;

    /** 差异计算数据JSON(改进的指标、退步的指标等) */
    private String comparisonDataJson;

    /** 中文对比报告(Markdown) */
    private String reportMarkdown;

    /** 关键统计快照 */
    private Integer improvedCount;
    private Integer declinedCount;
    private Integer stabilityImprovedCount;

    private BigDecimal llmCostYuan;
    private Integer llmInputTokens;
    private Integer llmOutputTokens;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
