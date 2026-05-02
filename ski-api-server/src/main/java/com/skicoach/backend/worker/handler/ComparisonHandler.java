package com.skicoach.backend.worker.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skicoach.backend.client.PythonAiClient;
import com.skicoach.backend.client.dto.CompareResponse;
import com.skicoach.backend.common.enums.TaskTypeEnum;
import com.skicoach.backend.entity.AnalysisTask;
import com.skicoach.backend.entity.Video;
import com.skicoach.backend.mapper.VideoMapper;
import com.skicoach.backend.service.AnalysisTaskService;
import com.skicoach.backend.service.ComparisonReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 对比报告任务处理器
 *
 * 完整流程:
 * 1. 取出两个视频的 analysis_data_json
 * 2. 反序列化为 Map
 * 3. 调用 Python /api/v1/compare(只跑 LLM,几秒搞定)
 * 4. 写入 comparison_reports
 * 5. 标记任务成功
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComparisonHandler implements TaskHandler {

    private final VideoMapper videoMapper;
    private final PythonAiClient pythonAiClient;
    private final ComparisonReportService comparisonReportService;
    private final AnalysisTaskService taskService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(AnalysisTask task) {
        return TaskTypeEnum.COMPARISON.getValue().equals(task.getTaskType());
    }

    @Override
    public void handle(AnalysisTask task) {
        Long prevVideoId = task.getPrevVideoId();
        Long currVideoId = task.getCurrVideoId();
        log.info("[对比分析] taskId={}, prev={}, curr={} 开始处理",
                task.getId(), prevVideoId, currVideoId);

        // 1. 取两个视频
        Video prev = videoMapper.selectById(prevVideoId);
        Video curr = videoMapper.selectById(currVideoId);
        if (prev == null || curr == null) {
            throw new RuntimeException("视频不存在: prevId=" + prevVideoId + ", currId=" + currVideoId);
        }
        if (prev.getAnalysisDataJson() == null || curr.getAnalysisDataJson() == null) {
            throw new RuntimeException("视频还未完成分析,无法对比");
        }

        // 2. 反序列化 analysis_data_json
        Map<String, Object> prevAnalysis;
        Map<String, Object> currAnalysis;
        try {
            prevAnalysis = objectMapper.readValue(prev.getAnalysisDataJson(),
                    new TypeReference<Map<String, Object>>() {});
            currAnalysis = objectMapper.readValue(curr.getAnalysisDataJson(),
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("分析数据反序列化失败: " + e.getMessage());
        }

        // 3. 调用 Python /compare
        log.info("[对比分析] taskId={}, 调用Python /compare", task.getId());
        CompareResponse response = pythonAiClient.compare(prevAnalysis, currAnalysis);

        // 4. 写入 comparison_reports
        String comparisonDataJson;
        try {
            comparisonDataJson = objectMapper.writeValueAsString(response.getComparisonData());
        } catch (Exception e) {
            throw new RuntimeException("comparison_data 序列化失败: " + e.getMessage());
        }

        Long reportId = comparisonReportService.createReportRecord(
                task.getId(),
                task.getUserId(),
                prevVideoId,
                currVideoId,
                comparisonDataJson,
                response.getReportMarkdown(),
                response.getImprovedCount(),
                response.getDeclinedCount(),
                response.getStabilityImprovedCount(),
                response.getLlmInputTokens(),
                response.getLlmOutputTokens(),
                response.getLlmCostYuan()
        );

        // 5. 标记任务成功
        taskService.markSuccess(
                task.getId(),
                reportId,
                response.getLlmInputTokens(),
                response.getLlmOutputTokens(),
                response.getLlmCostYuan()
        );

        log.info("[对比分析] taskId={} 完成: reportId={}, improved={}, declined={}",
                task.getId(), reportId,
                response.getImprovedCount(), response.getDeclinedCount());
    }
}
