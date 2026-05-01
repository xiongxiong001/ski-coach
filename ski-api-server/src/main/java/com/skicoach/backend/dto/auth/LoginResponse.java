package com.skicoach.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "JWT Token,后续请求需在Header携带: Authorization: Bearer {token}")
    private String token;

    @Schema(description = "Token过期时间(小时)")
    private Long expireHours;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "手机号(脱敏)")
    private String phone;
}
