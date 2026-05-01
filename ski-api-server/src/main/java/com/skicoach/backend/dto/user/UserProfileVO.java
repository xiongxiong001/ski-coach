package com.skicoach.backend.dto.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "用户个人资料")
public class UserProfileVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "手机号(脱敏)")
    private String phone;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "状态: 1=正常 0=封禁")
    private Integer status;

    @Schema(description = "注册时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
}
