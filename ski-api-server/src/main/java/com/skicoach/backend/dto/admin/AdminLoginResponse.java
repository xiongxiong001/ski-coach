package com.skicoach.backend.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理员登录响应")
public class AdminLoginResponse {

    @Schema(description = "JWT Token")
    private String token;

    @Schema(description = "Token过期时间(小时)")
    private Long expireHours;

    @Schema(description = "管理员ID")
    private Long adminId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;
}
