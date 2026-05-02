package com.skicoach.backend.dto.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "任务状态")
public class TaskStatusVO {

    @Schema(description = "任务ID")
    private Long id;

    @Schema(description = "任务类型: single / comparison")
    private String taskType;

    @Schema(description = "状态: pending / running / success / failed")
    private String status;

    @Schema(description = "关联的视频ID(单次任务)")
    private Long videoId;

    @Schema(description = "上次视频ID(对比任务)")
    private Long prevVideoId;

    @Schema(description = "本次视频ID(对比任务)")
    private Long currVideoId;

    @Schema(description = "成功后的报告ID")
    private Long reportId;

    @Schema(description = "失败原因")
    private String errorMessage;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    @Schema(description = "开始执行时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;
}
