package com.skicoach.backend.controller.user;

import com.skicoach.backend.common.result.ApiResult;
import com.skicoach.backend.common.result.PageResult;
import com.skicoach.backend.common.util.SecurityUtil;
import com.skicoach.backend.dto.comparison.ComparisonListItemVO;
import com.skicoach.backend.dto.comparison.ComparisonReportVO;
import com.skicoach.backend.dto.comparison.CreateComparisonRequest;
import com.skicoach.backend.service.ComparisonReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "对比报告")
@Slf4j
@RestController
@RequestMapping("/api/comparisons")
@RequiredArgsConstructor
public class ComparisonController {

    private final ComparisonReportService comparisonReportService;

    @Operation(summary = "创建对比报告", description = "选两个已分析的视频生成对比报告(异步)。如该视频对已有报告,直接返回")
    @PostMapping
    public ApiResult<ComparisonReportService.CreateComparisonResult> create(
            @RequestBody @Valid CreateComparisonRequest request) {
        Long userId = SecurityUtil.getUserIdOrThrow();
        return ApiResult.success(comparisonReportService.create(userId, request));
    }

    @Operation(summary = "我的对比报告列表")
    @GetMapping
    public ApiResult<PageResult<ComparisonListItemVO>> list(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize) {
        Long userId = SecurityUtil.getUserIdOrThrow();
        return ApiResult.success(comparisonReportService.list(userId, pageNum, pageSize));
    }

    @Operation(summary = "对比报告详情")
    @GetMapping("/{id}")
    public ApiResult<ComparisonReportVO> detail(@PathVariable("id") Long id) {
        Long userId = SecurityUtil.getUserIdOrThrow();
        return ApiResult.success(comparisonReportService.getDetail(userId, id));
    }
}
