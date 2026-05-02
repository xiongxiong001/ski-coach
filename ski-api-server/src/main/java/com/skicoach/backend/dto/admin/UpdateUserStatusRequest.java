package com.skicoach.backend.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "更新用户状态请求")
public class UpdateUserStatusRequest {

    @Schema(description = "状态: 1=正常 0=封禁", example = "0")
    @NotNull(message = "状态不能为空")
    private Integer status;
}
