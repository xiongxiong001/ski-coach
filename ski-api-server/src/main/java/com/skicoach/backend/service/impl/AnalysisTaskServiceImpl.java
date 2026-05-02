package com.skicoach.backend.service.impl;

import com.skicoach.backend.common.constant.RedisKeyConstant;
import com.skicoach.backend.common.enums.TaskStatusEnum;
import com.skicoach.backend.common.enums.TaskTypeEnum;
import com.skicoach.backend.common.exception.BusinessException;
import com.skicoach.backend.common.result.ResultCode;
import com.skicoach.backend.entity.AnalysisTask;
import com.skicoach.backend.mapper.AnalysisTaskMapper;
import com.skicoach.backend.service.AnalysisTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisTaskServiceImpl implements AnalysisTaskService {

    private final AnalysisTaskMapper taskMapper;
    private final RedissonClient redissonClient;

    @Override
    @Transactional
    public Long enqueueSingleAnalysis(Long userId, Long videoId) {
        AnalysisTask task = new AnalysisTask();
        task.setUserId(userId);
        task.setTaskType(TaskTypeEnum.SINGLE.getValue());
        task.setVideoId(videoId);
        task.setStatus(TaskStatusEnum.PENDING.getValue());
        task.setRetryCount(0);

        taskMapper.insert(task);

        // 推送到Redis队列(事务提交后再推送可能更稳妥,这里简化处理)
        pushToQueue(task.getId());
        log.info("[任务入队] taskId={}, type=single, videoId={}", task.getId(), videoId);

        return task.getId();
    }

    @Override
    @Transactional
    public Long enqueueComparison(Long userId, Long prevVideoId, Long currVideoId) {
        AnalysisTask task = new AnalysisTask();
        task.setUserId(userId);
        task.setTaskType(TaskTypeEnum.COMPARISON.getValue());
        task.setPrevVideoId(prevVideoId);
        task.setCurrVideoId(currVideoId);
        task.setStatus(TaskStatusEnum.PENDING.getValue());
        task.setRetryCount(0);

        taskMapper.insert(task);
        pushToQueue(task.getId());
        log.info("[任务入队] taskId={}, type=comparison, prev={}, curr={}",
                task.getId(), prevVideoId, currVideoId);

        return task.getId();
    }

    @Override
    public void markRunning(Long taskId) {
        AnalysisTask update = new AnalysisTask();
        update.setId(taskId);
        update.setStatus(TaskStatusEnum.RUNNING.getValue());
        update.setStartTime(LocalDateTime.now());
        taskMapper.updateById(update);
    }

    @Override
    public void markSuccess(Long taskId, Long reportId,
                             Integer inputTokens, Integer outputTokens,
                             BigDecimal costYuan) {
        AnalysisTask update = new AnalysisTask();
        update.setId(taskId);
        update.setStatus(TaskStatusEnum.SUCCESS.getValue());
        update.setReportId(reportId);
        update.setLlmInputTokens(inputTokens);
        update.setLlmOutputTokens(outputTokens);
        update.setLlmCostYuan(costYuan);
        update.setFinishTime(LocalDateTime.now());
        taskMapper.updateById(update);
    }

    @Override
    public void markFailed(Long taskId, String errorMessage) {
        AnalysisTask update = new AnalysisTask();
        update.setId(taskId);
        update.setStatus(TaskStatusEnum.FAILED.getValue());
        // 截断过长的错误信息
        if (errorMessage != null && errorMessage.length() > 1000) {
            errorMessage = errorMessage.substring(0, 1000) + "...";
        }
        update.setErrorMessage(errorMessage);
        update.setFinishTime(LocalDateTime.now());
        taskMapper.updateById(update);
    }

    @Override
    public AnalysisTask getOwnedTaskOrThrow(Long userId, Long taskId) {
        AnalysisTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "任务不存在");
        }
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "任务不属于当前用户");
        }
        return task;
    }

    @Override
    public AnalysisTask getById(Long taskId) {
        return taskMapper.selectById(taskId);
    }

    // -------- 私有工具 --------

    private void pushToQueue(Long taskId) {
        RBlockingQueue<Long> queue = redissonClient.getBlockingQueue(RedisKeyConstant.TASK_QUEUE);
        queue.offer(taskId);
    }
}
