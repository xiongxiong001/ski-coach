package com.skicoach.backend.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Python /api/v1/compare 接口的响应数据
 */
@Data
public class CompareResponse {

    /** 差异计算数据(metric_diffs, action_diff, improved_count 等) */
    @JsonProperty("comparison_data")
    private Map<String, Object> comparisonData;

    @JsonProperty("report_markdown")
    private String reportMarkdown;

    @JsonProperty("improved_count")
    private Integer improvedCount;

    @JsonProperty("declined_count")
    private Integer declinedCount;

    @JsonProperty("stability_improved_count")
    private Integer stabilityImprovedCount;

    @JsonProperty("llm_input_tokens")
    private Integer llmInputTokens;

    @JsonProperty("llm_output_tokens")
    private Integer llmOutputTokens;

    @JsonProperty("llm_cost_yuan")
    private BigDecimal llmCostYuan;
}
