package com.skicoach.backend.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skicoach.backend.client.dto.AnalyzeRequest;
import com.skicoach.backend.client.dto.AnalyzeResponse;
import com.skicoach.backend.client.dto.CompareRequest;
import com.skicoach.backend.client.dto.CompareResponse;
import com.skicoach.backend.common.exception.BusinessException;
import com.skicoach.backend.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Python AI Server 的 HTTP 客户端实现(基于OkHttp)
 *
 * 错误处理策略:
 * - 网络错误:抛 BusinessException(AI_SERVICE_ERROR)
 * - 超时:抛 BusinessException(AI_SERVICE_TIMEOUT)
 * - Python 返回非 0 code:抛 BusinessException 携带 Python 的错误信息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PythonAiClientImpl implements PythonAiClient {

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    @Value("${ski.ai.python-base-url}")
    private String baseUrl;

    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    @Override
    public AnalyzeResponse analyze(String videoAbsolutePath) {
        AnalyzeRequest req = new AnalyzeRequest(videoAbsolutePath);
        String url = baseUrl + "/api/v1/analyze";

        log.info("[AI调用] /analyze, videoPath={}", videoAbsolutePath);
        long start = System.currentTimeMillis();

        try {
            String responseBody = doPost(url, req);
            log.info("[AI调用] /analyze 完成, 耗时={}ms", System.currentTimeMillis() - start);
            return parseAndExtractData(responseBody, AnalyzeResponse.class);

        } catch (IOException e) {
            // OkHttp 的 IOException 涵盖网络错误和超时
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("timeout") || msg.contains("Read timed out")) {
                log.error("[AI调用] /analyze 超时", e);
                throw new BusinessException(ResultCode.AI_SERVICE_TIMEOUT, "AI分析超时");
            }
            log.error("[AI调用] /analyze 网络异常", e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR,
                    "AI服务调用失败: " + e.getMessage());
        }
    }

    @Override
    public CompareResponse compare(Map<String, Object> prevAnalysisData, Map<String, Object> currAnalysisData) {
        CompareRequest req = new CompareRequest(prevAnalysisData, currAnalysisData);
        String url = baseUrl + "/api/v1/compare";

        log.info("[AI调用] /compare");
        long start = System.currentTimeMillis();

        try {
            String responseBody = doPost(url, req);
            log.info("[AI调用] /compare 完成, 耗时={}ms", System.currentTimeMillis() - start);
            return parseAndExtractData(responseBody, CompareResponse.class);

        } catch (IOException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("timeout")) {
                throw new BusinessException(ResultCode.AI_SERVICE_TIMEOUT, "AI对比超时");
            }
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR,
                    "AI服务调用失败: " + e.getMessage());
        }
    }

    // -------- 私有工具 --------

    private String doPost(String url, Object body) throws IOException {
        String json = objectMapper.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(json, JSON_TYPE))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                log.error("[AI调用] HTTP状态码异常: status={}, body={}",
                        response.code(), responseBody);
                throw new BusinessException(ResultCode.AI_SERVICE_ERROR,
                        "AI服务返回HTTP " + response.code());
            }
            return response.body() != null ? response.body().string() : "";
        }
    }

    /**
     * 解析 Python 返回的 ApiResponse 格式,提取 data 字段并转成目标类型
     */
    private <T> T parseAndExtractData(String responseBody, Class<T> targetType) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            int code = root.path("code").asInt(-1);
            String message = root.path("message").asText("");

            if (code != 0) {
                log.warn("[AI调用] 业务返回失败: code={}, message={}", code, message);
                throw new BusinessException(ResultCode.AI_SERVICE_ERROR,
                        "AI服务: " + message);
            }

            JsonNode dataNode = root.path("data");
            if (dataNode.isMissingNode() || dataNode.isNull()) {
                throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI服务返回data为空");
            }

            return objectMapper.treeToValue(dataNode, targetType);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AI调用] 响应解析失败: body={}", responseBody, e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI响应解析失败");
        }
    }
}
