package com.skicoach.backend.worker.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skicoach.backend.client.PythonAiClient;
import com.skicoach.backend.client.dto.AnalyzeResponse;
import com.skicoach.backend.common.enums.TaskTypeEnum;
import com.skicoach.backend.common.enums.VideoStatusEnum;
import com.skicoach.backend.entity.AnalysisTask;
import com.skicoach.backend.entity.Video;
import com.skicoach.backend.mapper.VideoMapper;
import com.skicoach.backend.service.AnalysisTaskService;
import com.skicoach.backend.service.FileStorageService;
import com.skicoach.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 单视频分析任务处理器
 *
 * 完整流程:
 * 1. 取出视频路径 → 解析为绝对路径
 * 2. video.analysis_status = analyzing
 * 3. 调用 Python /api/v1/analyze
 * 4. 把 analysis_data 写到 videos 表
 * 5. 把 report 写到 reports 表
 * 6. 关联 task.report_id, video.analysis_status = analyzed
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SingleAnalysisHandler implements TaskHandler {

    private final VideoMapper videoMapper;
    private final FileStorageService fileStorageService;
    private final PythonAiClient pythonAiClient;
    private final ReportService reportService;
    private final AnalysisTaskService taskService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(AnalysisTask task) {
        return TaskTypeEnum.SINGLE.getValue().equals(task.getTaskType());
    }

    @Override
    public void handle(AnalysisTask task) {
        Long videoId = task.getVideoId();
        log.info("[单次分析] taskId={}, videoId={} 开始处理", task.getId(), videoId);

        // 1. 取视频
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            throw new RuntimeException("视频不存在: videoId=" + videoId);
        }

        // 2. 视频状态置为 analyzing
        Video update = new Video();
        update.setId(videoId);
        update.setAnalysisStatus(VideoStatusEnum.ANALYZING.getValue());
        update.setAnalysisStartedTime(LocalDateTime.now());
        videoMapper.updateById(update);

        // 3. 解析绝对路径并调用 Python
        String absolutePath = fileStorageService.resolveAbsolutePath(video.getFilePath());
        log.info("[单次分析] taskId={}, 调用Python: {}", task.getId(), absolutePath);

        AnalyzeResponse response;
        try {
            response = pythonAiClient.analyze(absolutePath);
        } catch (Exception e) {
            // Python 调用失败,把视频状态置为 failed
            Video failedUpdate = new Video();
            failedUpdate.setId(videoId);
            failedUpdate.setAnalysisStatus(VideoStatusEnum.FAILED.getValue());
            failedUpdate.setAnalysisErrorMessage(e.getMessage());
            failedUpdate.setAnalysisFinishedTime(LocalDateTime.now());
            videoMapper.updateById(failedUpdate);
            throw e;  // 继续抛给 Worker 标记 task 为 failed
        }

        // 4. 解析 Python 返回的数据,提取关键统计指标
        Map<String, Object> analysisData = response.getAnalysisData();
        VideoAnalysisSnapshot snapshot = extractSnapshot(analysisData);

        // 5. 把 analysis_data 写回 videos
        Video successUpdate = new Video();
        successUpdate.setId(videoId);
        successUpdate.setAnalysisStatus(VideoStatusEnum.ANALYZED.getValue());
        successUpdate.setAnalysisFinishedTime(LocalDateTime.now());
        try {
            successUpdate.setAnalysisDataJson(objectMapper.writeValueAsString(analysisData));
        } catch (Exception e) {
            log.error("[单次分析] analysis_data 序列化失败", e);
            throw new RuntimeException("分析数据序列化失败");
        }
        successUpdate.setDurationSeconds(snapshot.duration);
        successUpdate.setWidth(snapshot.width);
        successUpdate.setHeight(snapshot.height);
        successUpdate.setFps(snapshot.fps);
        successUpdate.setDetectionRate(snapshot.detectionRate);
        successUpdate.setTurnLeftCount(snapshot.turnLeft);
        successUpdate.setTurnRightCount(snapshot.turnRight);
        videoMapper.updateById(successUpdate);

        // 6. 写 reports 表
        Long reportId = reportService.createReport(
                task.getId(),
                videoId,
                video.getUserId(),
                response.getReportMarkdown(),
                response.getLlmInputTokens(),
                response.getLlmOutputTokens(),
                response.getLlmCostYuan()
        );

        // 7. 标记任务成功
        taskService.markSuccess(
                task.getId(),
                reportId,
                response.getLlmInputTokens(),
                response.getLlmOutputTokens(),
                response.getLlmCostYuan()
        );

        log.info("[单次分析] taskId={} 完成: reportId={}, detectionRate={}%, turn=L{}/R{}",
                task.getId(), reportId,
                snapshot.detectionRate != null ? snapshot.detectionRate.movePointRight(2) : null,
                snapshot.turnLeft, snapshot.turnRight);
    }

    /**
     * 从 analysis_data 中提取关键快照,冗余存到 videos 表方便列表展示
     */
    @SuppressWarnings("unchecked")
    private VideoAnalysisSnapshot extractSnapshot(Map<String, Object> analysisData) {
        VideoAnalysisSnapshot s = new VideoAnalysisSnapshot();
        try {
            Map<String, Object> videoInfo = (Map<String, Object>) analysisData.get("video_info");
            if (videoInfo != null) {
                s.duration = toBigDecimal(videoInfo.get("duration"));
                s.fps = toBigDecimal(videoInfo.get("fps"));
                s.width = toInt(videoInfo.get("width"));
                s.height = toInt(videoInfo.get("height"));
                s.detectionRate = toBigDecimal(videoInfo.get("detection_rate"));
            }
            Map<String, Object> actionCounts = (Map<String, Object>) analysisData.get("action_counts");
            if (actionCounts != null) {
                s.turnLeft = toInt(actionCounts.get("turn_left"));
                s.turnRight = toInt(actionCounts.get("turn_right"));
            }
        } catch (Exception e) {
            log.warn("[单次分析] 提取快照字段失败,继续: {}", e.getMessage());
        }
        return s;
    }

    private BigDecimal toBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal bd) return bd;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(o.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception e) {
            return null;
        }
    }

    /** 内部用的快照对象 */
    private static class VideoAnalysisSnapshot {
        BigDecimal duration;
        Integer width;
        Integer height;
        BigDecimal fps;
        BigDecimal detectionRate;
        Integer turnLeft;
        Integer turnRight;
    }
}
