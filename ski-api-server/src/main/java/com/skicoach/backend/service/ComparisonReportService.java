package com.skicoach.backend.service;

import com.skicoach.backend.common.result.PageResult;
import com.skicoach.backend.dto.comparison.ComparisonListItemVO;
import com.skicoach.backend.dto.comparison.ComparisonReportVO;
import com.skicoach.backend.dto.comparison.CreateComparisonRequest;
import com.skicoach.backend.entity.ComparisonReport;

import java.math.BigDecimal;

public interface ComparisonReportService {

    /**
     * 创建对比报告(异步)
     * @return Map { "taskId": ..., "reportId": ... 或 null }
     *         如果该视频对已有报告,直接返回 reportId(不创建任务)
     *         否则创建任务并返回 taskId
     */
    CreateComparisonResult create(Long userId, CreateComparisonRequest request);

    /** 报告详情(校验所属) */
    ComparisonReportVO getDetail(Long userId, Long reportId);

    /** 报告列表(分页) */
    PageResult<ComparisonListItemVO> list(Long userId, Long pageNum, Long pageSize);

    /** 内部使用:根据视频对查找(用于幂等) */
    ComparisonReport findByVideoPair(Long prevVideoId, Long currVideoId);

    /** Worker 调用:创建报告记录 */
    Long createReportRecord(Long taskId, Long userId, Long prevVideoId, Long currVideoId,
                             String comparisonDataJson, String reportMarkdown,
                             Integer improvedCount, Integer declinedCount, Integer stabilityImprovedCount,
                             Integer inputTokens, Integer outputTokens, BigDecimal costYuan);

    /** 用于直接命中已有报告的返回 */
    record CreateComparisonResult(Long taskId, Long reportId, boolean cacheHit) {}
}
