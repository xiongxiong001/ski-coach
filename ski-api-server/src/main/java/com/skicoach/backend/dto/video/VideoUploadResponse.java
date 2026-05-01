package com.skicoach.backend.dto.video;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "视频上传响应")
public class VideoUploadResponse {

    @Schema(description = "视频ID")
    private Long videoId;

    @Schema(description = "原始文件名")
    private String originalFilename;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @Schema(description = "分析状态: pending=等待分析 analyzing=分析中 analyzed=已完成 failed=分析失败")
    private String analysisStatus;

    @Schema(description = "是否秒传命中(true=该用户已上传过相同文件,直接复用)")
    private Boolean instantUpload;

    @Schema(description = "提示信息")
    private String message;
}
