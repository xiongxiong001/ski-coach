package com.skicoach.backend.controller.admin;

import com.skicoach.backend.common.result.ApiResult;
import com.skicoach.backend.dto.admin.DailyStatsVO;
import com.skicoach.backend.dto.admin.LlmCostStatsVO;
import com.skicoach.backend.dto.admin.OverviewStatsVO;
import com.skicoach.backend.dto.admin.StorageStatsVO;
import com.skicoach.backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "管理后台-数据统计")
@RestController
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminService adminService;

    @Operation(summary = "总览统计")
    @GetMapping("/overview")
    public ApiResult<OverviewStatsVO> overview() {
        return ApiResult.success(adminService.getOverview());
    }

    @Operation(summary = "每日统计(默认最近7天)")
    @GetMapping("/daily")
    public ApiResult<List<DailyStatsVO>> daily(
            @Parameter(description = "开始日期 yyyy-MM-dd")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResult.success(adminService.getDailyStats(startDate, endDate));
    }

    @Operation(summary = "LLM 成本明细(按任务类型聚合)")
    @GetMapping("/llm-cost")
    public ApiResult<List<LlmCostStatsVO>> llmCost() {
        return ApiResult.success(adminService.getLlmCostStats());
    }

    @Operation(summary = "存储使用情况")
    @GetMapping("/storage")
    public ApiResult<StorageStatsVO> storage() {
        return ApiResult.success(adminService.getStorageStats());
    }
}
