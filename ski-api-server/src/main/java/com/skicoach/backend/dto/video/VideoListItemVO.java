package com.skicoach.backend.dto.video;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 视频列表项 VO
 * 不包含 analysisDataJson(数据量大),需要时通过详情接口获取
 */
@Data
@Schema(description = "视频列表项")
public class VideoListItemVO {

    @Schema(description = "视频ID")
    private Long id;

    @Schema(description = "原始文件名")
    private String originalFilename;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @Schema(description = "视频时长(秒)")
    private BigDecimal durationSeconds;

    @Schema(description = "分析状态")
    private String analysisStatus;

    @Schema(description = "姿态检测率")
    private BigDecimal detectionRate;

    @Schema(description = "左转次数")
    private Integer turnLeftCount;

    @Schema(description = "右转次数")
    private Integer turnRightCount;

    @Schema(description = "上传时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
}
