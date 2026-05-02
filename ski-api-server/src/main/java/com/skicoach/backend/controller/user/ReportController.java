package com.skicoach.backend.controller.user;

import com.skicoach.backend.common.result.ApiResult;
import com.skicoach.backend.common.result.PageResult;
import com.skicoach.backend.common.util.SecurityUtil;
import com.skicoach.backend.dto.report.ReportDetailVO;
import com.skicoach.backend.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教练报告接口
 *
 * - GET /api/reports          我的报告列表
 * - GET /api/reports/{id}     报告详情
 */
@Tag(name = "教练报告")
@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "我的报告列表")
    @GetMapping
    public ApiResult<PageResult<ReportDetailVO>> list(
            @Parameter(description = "页码,从1开始") @RequestParam(defaultValue = "1") Long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long pageSize) {
        Long userId = SecurityUtil.getUserIdOrThrow();
        return ApiResult.success(reportService.list(userId, pageNum, pageSize));
    }

    @Operation(summary = "报告详情")
    @GetMapping("/{id}")
    public ApiResult<ReportDetailVO> detail(@PathVariable("id") Long id) {
        Long userId = SecurityUtil.getUserIdOrThrow();
        return ApiResult.success(reportService.getDetail(userId, id));
    }
}
