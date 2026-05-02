package com.skicoach.backend.service;

import com.skicoach.backend.common.result.PageResult;
import com.skicoach.backend.dto.report.ReportDetailVO;
import com.skicoach.backend.entity.Report;

import java.math.BigDecimal;

public interface ReportService {

    /** 创建单次报告(供 Worker 调用) */
    Long createReport(Long taskId, Long videoId, Long userId, String reportMarkdown,
                      Integer inputTokens, Integer outputTokens, BigDecimal costYuan);

    /** 获取报告详情(校验所属) */
    ReportDetailVO getDetail(Long userId, Long reportId);

    /** 我的报告列表(分页) */
    PageResult<ReportDetailVO> list(Long userId, Long pageNum, Long pageSize);

    /** 内部使用:根据视频ID查找报告 */
    Report findByVideoId(Long videoId);
}
