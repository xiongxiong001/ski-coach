package com.skicoach.backend.controller.user;

import com.skicoach.backend.common.result.ApiResult;
import com.skicoach.backend.common.result.PageResult;
import com.skicoach.backend.common.util.JwtUtil;
import com.skicoach.backend.common.util.SecurityUtil;
import com.skicoach.backend.dto.feedback.FeedbackCreateRequest;
import com.skicoach.backend.dto.feedback.FeedbackStatsVO;
import com.skicoach.backend.dto.feedback.FeedbackVO;
import com.skicoach.backend.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 用户反馈接口
 */
@Tag(name = "用户反馈")
@Slf4j
@RestController
@RequestMapping("/api/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "提交反馈")
    @PostMapping
    public ApiResult<Void> submit(
            @Valid FeedbackCreateRequest request,
            @Parameter(description = "截图(最多5张)")
            @RequestParam(value = "images", required = false) List<MultipartFile> images) throws IOException {
        Long userId = SecurityUtil.getUserIdOrThrow();
        feedbackService.submit(userId, request, images);
        return ApiResult.success();
    }

    @Operation(summary = "反馈统计")
    @GetMapping("/stats")
    public ApiResult<FeedbackStatsVO> stats() {
        Long userId = SecurityUtil.getUserIdOrThrow();
        return ApiResult.success(feedbackService.getStats(userId));
    }

    @Operation(summary = "我的反馈列表")
    @GetMapping
    public ApiResult<PageResult<FeedbackVO>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Long pageSize,
            @Parameter(description = "筛选类型") @RequestParam(required = false) String type) {
        Long userId = SecurityUtil.getUserIdOrThrow();
        PageResult<FeedbackVO> page = feedbackService.list(userId, pageNum, pageSize, type);
        return ApiResult.success(page);
    }

    @Operation(summary = "反馈详情")
    @GetMapping("/{id}")
    public ApiResult<FeedbackVO> detail(@PathVariable("id") Long id) {
        Long userId = SecurityUtil.getUserIdOrThrow();
        FeedbackVO vo = feedbackService.getDetail(userId, id);
        return ApiResult.success(vo);
    }

    @Operation(summary = "查看反馈图片", description = "流式返回反馈图片,通过 ?token= 鉴权(浏览器img标签无法发送自定义Header)")
    @GetMapping("/{id}/images/{index}")
    public void image(
            @PathVariable("id") Long id,
            @PathVariable("index") int index,
            @RequestParam("token") String token,
            HttpServletResponse response) throws IOException {
        Long userId = jwtUtil.parseUserToken(token);
        Path path = feedbackService.resolveImagePath(userId, id, index);

        if (!Files.exists(path)) {
            response.setStatus(404);
            return;
        }

        String contentType = Files.probeContentType(path);
        if (contentType == null) {
            contentType = "image/jpeg";
        }
        response.setContentType(contentType);
        response.setContentLengthLong(Files.size(path));
        Files.copy(path, response.getOutputStream());
        response.getOutputStream().flush();
    }
}
