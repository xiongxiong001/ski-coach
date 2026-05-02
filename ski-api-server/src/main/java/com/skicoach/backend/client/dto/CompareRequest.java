package com.skicoach.backend.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Python /api/v1/compare 接口的请求
 *
 * 注意: 此接口不接收视频文件,只接收两份已经计算好的 analysis_data
 * (从 videos.analysisDataJson 字段读出来的)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompareRequest {

    @JsonProperty("prev_analysis_data")
    private Map<String, Object> prevAnalysisData;

    @JsonProperty("curr_analysis_data")
    private Map<String, Object> currAnalysisData;
}
