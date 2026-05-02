package com.skicoach.backend.worker.handler;

import com.skicoach.backend.entity.AnalysisTask;

/**
 * 任务处理器接口
 *
 * 不同类型的任务(single/comparison)有不同的实现类。
 * Worker 根据 task_type 派发到对应的 Handler。
 */
public interface TaskHandler {

    /** 是否能处理某个任务 */
    boolean supports(AnalysisTask task);

    /** 处理任务(成功/失败由 Worker 统一记录) */
    void handle(AnalysisTask task);
}
