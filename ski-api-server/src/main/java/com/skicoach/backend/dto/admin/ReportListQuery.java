// 报告列表查询参数
package com.skicoach.backend.dto.admin;

import lombok.Data;

@Data
public class ReportListQuery {
    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private Long userId;      // 用户ID筛选
    private String keyword;   // 关键词搜索(搜索报告内容)
}
