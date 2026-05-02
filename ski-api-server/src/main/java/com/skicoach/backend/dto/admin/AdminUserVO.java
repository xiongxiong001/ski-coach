package com.skicoach.backend.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "管理后台-用户视图")
public class AdminUserVO {

    private Long id;

    @Schema(description = "手机号(完整,管理员能看)")
    private String phone;

    private String nickname;

    @Schema(description = "状态: 1=正常 0=封禁")
    private Integer status;

    @Schema(description = "上传视频数")
    private Integer videoCount;

    @Schema(description = "已生成报告数")
    private Integer reportCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
