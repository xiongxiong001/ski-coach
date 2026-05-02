package com.skicoach.backend.service;

import com.skicoach.backend.entity.AnalysisTask;

/**
 * 分析任务服务
 *
 * 主要负责任务记录的CRUD和入队/出队。
 * 真正的业务执行在 worker 包的 Handler 里。
 */
public interface AnalysisTaskService {

    /**
     * 创建一个单视频分析任务,并推送到Redis队列
     * @return 任务ID
     */
    Long enqueueSingleAnalysis(Long userId, Long videoId);

    /**
     * 创建一个对比分析任务,并推送到Redis队列(P2.5用)
     * @return 任务ID
     */
    Long enqueueComparison(Long userId, Long prevVideoId, Long currVideoId);

    /** 标记任务为running */
    void markRunning(Long taskId);

    /** 标记任务为success(并填充token/cost/reportId) */
    void markSuccess(Long taskId, Long reportId,
                     Integer inputTokens, Integer outputTokens,
                     java.math.BigDecimal costYuan);

    /** 标记任务为failed */
    void markFailed(Long taskId, String errorMessage);

    /** 获取任务,校验所属 */
    AnalysisTask getOwnedTaskOrThrow(Long userId, Long taskId);

    /** 仅根据ID获取(供worker内部使用,不校验userId) */
    AnalysisTask getById(Long taskId);
}
