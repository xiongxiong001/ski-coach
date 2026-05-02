package com.skicoach.backend.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "用户列表查询(管理后台)")
public class UserListQuery {

    @Min(1) private Long pageNum = 1L;

    @Min(1) @Max(100) private Long pageSize = 20L;

    @Schema(description = "手机号搜索(模糊)")
    private String phone;

    @Schema(description = "状态过滤: 1=正常 0=封禁")
    private Integer status;
}
