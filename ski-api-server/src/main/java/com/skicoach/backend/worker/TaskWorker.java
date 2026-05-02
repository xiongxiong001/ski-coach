package com.skicoach.backend.worker;

import com.skicoach.backend.common.constant.RedisKeyConstant;
import com.skicoach.backend.entity.AnalysisTask;
import com.skicoach.backend.service.AnalysisTaskService;
import com.skicoach.backend.worker.handler.TaskHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 后台任务消费者
 *
 * 应用启动后,启动N个worker线程从Redis队列拉取任务执行。
 * 关键设计:
 * - 用 Redisson 的 RBlockingQueue,take() 会阻塞直到有任务
 * - 重启不丢任务(任务在Redis里持久化)
 * - 优雅关闭:接收停止信号后,等待当前任务执行完
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskWorker {

    private final RedissonClient redissonClient;
    private final AnalysisTaskService taskService;
    private final List<TaskHandler> handlers;

    @Value("${ski.worker.thread-count:2}")
    private int workerCount;

    private ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @PostConstruct
    public void start() {
        log.info("[TaskWorker] 启动 {} 个worker线程", workerCount);
        running.set(true);
        executor = Executors.newFixedThreadPool(workerCount, r -> {
            Thread t = new Thread(r);
            t.setName("task-worker-" + t.getId());
            t.setDaemon(false);
            return t;
        });
        for (int i = 0; i < workerCount; i++) {
            final int workerId = i;
            executor.submit(() -> consume(workerId));
        }
    }

    @PreDestroy
    public void stop() {
        log.info("[TaskWorker] 接收停止信号,等待当前任务完成...");
        running.set(false);
        if (executor != null) {
            executor.shutdown();
            try {
                // 给正在执行的任务最多 30 秒完成
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.warn("[TaskWorker] 30秒内未结束,强制中断");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("[TaskWorker] 已停止");
    }

    private void consume(int workerId) {
        log.info("[Worker-{}] 启动", workerId);
        RBlockingQueue<Long> queue = redissonClient.getBlockingQueue(RedisKeyConstant.TASK_QUEUE);

        while (running.get()) {
            Long taskId = null;
            try {
                // 阻塞等待,但有超时,避免shutdown时卡住
                taskId = queue.poll(5, TimeUnit.SECONDS);
                if (taskId == null) continue;

                handleTask(workerId, taskId);

            } catch (InterruptedException e) {
                log.info("[Worker-{}] 被中断,退出", workerId);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("[Worker-{}] 处理任务异常: taskId={}", workerId, taskId, e);
            }
        }
        log.info("[Worker-{}] 退出", workerId);
    }

    private void handleTask(int workerId, Long taskId) {
        AnalysisTask task = taskService.getById(taskId);
        if (task == null) {
            log.warn("[Worker-{}] 任务不存在: taskId={}", workerId, taskId);
            return;
        }

        // 防御性检查: 已成功的任务不重复处理
        if ("success".equals(task.getStatus())) {
            log.warn("[Worker-{}] 任务已成功,跳过: taskId={}", workerId, taskId);
            return;
        }

        // 找到对应的Handler
        TaskHandler handler = handlers.stream()
                .filter(h -> h.supports(task))
                .findFirst()
                .orElse(null);
        if (handler == null) {
            log.error("[Worker-{}] 找不到对应的Handler: taskType={}", workerId, task.getTaskType());
            taskService.markFailed(taskId, "找不到对应的处理器");
            return;
        }

        // 标记 running
        taskService.markRunning(taskId);

        // 执行
        try {
            log.info("[Worker-{}] 开始处理 taskId={}, type={}", workerId, taskId, task.getTaskType());
            handler.handle(task);
            log.info("[Worker-{}] 任务成功 taskId={}", workerId, taskId);
        } catch (Exception e) {
            log.error("[Worker-{}] 任务执行失败 taskId={}", workerId, taskId, e);
            taskService.markFailed(taskId, e.getMessage());
        }
    }
}
