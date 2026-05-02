package com.skicoach.backend.client;

import com.skicoach.backend.client.dto.AnalyzeResponse;
import com.skicoach.backend.client.dto.CompareResponse;

import java.util.Map;

/**
 * 调用 ski-ai-server (Python AI推理服务) 的客户端
 */
public interface PythonAiClient {

    /**
     * 单视频分析
     *
     * @param videoAbsolutePath 视频文件的绝对路径(必须在Python端的VIDEO_STORAGE_BASE_PATH下)
     * @return 分析结果(包含 analysis_data 和 教练报告)
     * @throws RuntimeException 调用失败时抛出
     */
    AnalyzeResponse analyze(String videoAbsolutePath);

    /**
     * 生成对比报告
     *
     * @param prevAnalysisData 上次视频的分析数据
     * @param currAnalysisData 本次视频的分析数据
     * @return 对比结果
     */
    CompareResponse compare(Map<String, Object> prevAnalysisData, Map<String, Object> currAnalysisData);
}
