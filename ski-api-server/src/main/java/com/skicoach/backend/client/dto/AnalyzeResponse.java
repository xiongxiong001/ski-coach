package com.skicoach.backend.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Python /api/v1/analyze 接口的响应数据(对应 ApiResponse.data)
 *
 * Python 端返回结构:
 * {
 *   "code": 0,
 *   "message": "success",
 *   "data": {
 *     "analysis_data": { ... },
 *     "report_markdown": "...",
 *     "llm_input_tokens": 1500,
 *     "llm_output_tokens": 800,
 *     "llm_cost_yuan": 0.0023
 *   }
 * }
 */
@Data
public class AnalyzeResponse {

    /** 完整分析数据(姿态指标、动作分割等)。Map 形式接收,不强类型化 */
    @JsonProperty("analysis_data")
    private Map<String, Object> analysisData;

    /** 中文教练报告 */
    @JsonProperty("report_markdown")
    private String reportMarkdown;

    @JsonProperty("llm_input_tokens")
    private Integer llmInputTokens;

    @JsonProperty("llm_output_tokens")
    private Integer llmOutputTokens;

    @JsonProperty("llm_cost_yuan")
    private BigDecimal llmCostYuan;
}
