package com.skicoach.backend.controller.admin;

import com.skicoach.backend.common.result.ApiResult;
import com.skicoach.backend.common.result.PageResult;
import com.skicoach.backend.dto.admin.*;
import com.skicoach.backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
    /**
     * 管理后台-内容审阅
     *
     * 用于管理员浏览所有用户的 AI 教练报告,
     * 主要场景:
     * - 评估 AI 输出质量(发现胡言乱语)
     * - 找差案例改进 prompt
     * - 处理用户投诉(看具体哪份报告有问题)
     */
    @Tag(name = "管理后台-内容审阅")
    @RestController
    @RequestMapping("/admin/reports")
    @RequiredArgsConstructor
    public class AdminReportController {

        private final AdminService adminService;

        @Operation(summary = "单次报告列表", description = "管理员可看到所有用户的 AI 教练报告,支持按用户ID/手机号筛选")
        @GetMapping
        public ApiResult<PageResult<AdminReportListItemVO>> listReports(ReportListQuery query) {
            return ApiResult.success(adminService.listReports(query));
        }

        @Operation(summary = "单次报告详情(完整Markdown + 用户/视频上下文)")
        @GetMapping("/{id}")
        public ApiResult<AdminReportDetailVO> getReportDetail(@PathVariable Long id) {
            return ApiResult.success(adminService.getReportDetail(id));
        }

        @Operation(summary = "对比报告列表")
        @GetMapping("/comparisons")
        public ApiResult<PageResult<AdminComparisonListItemVO>> listComparisonReports(ReportListQuery query) {
            return ApiResult.success(adminService.listComparisonReports(query));
        }

        @Operation(summary = "对比报告详情(完整Markdown + 进步统计)")
        @GetMapping("/comparisons/{id}")
        public ApiResult<AdminComparisonDetailVO> getComparisonReportDetail(@PathVariable Long id) {
            return ApiResult.success(adminService.getComparisonReportDetail(id));
        }

}