package com.skicoach.backend.controller.user;

import com.skicoach.backend.common.exception.BusinessException;
import com.skicoach.backend.common.result.ApiResult;
import com.skicoach.backend.common.result.PageResult;
import com.skicoach.backend.common.result.ResultCode;
import com.skicoach.backend.common.util.JwtUtil;
import com.skicoach.backend.common.util.SecurityUtil;
import com.skicoach.backend.dto.video.VideoDetailVO;
import com.skicoach.backend.dto.video.VideoListItemVO;
import com.skicoach.backend.dto.video.VideoListQuery;
import com.skicoach.backend.dto.video.VideoUploadResponse;
import com.skicoach.backend.entity.Video;
import com.skicoach.backend.service.FileStorageService;
import com.skicoach.backend.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
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

/**
 * 视频接口
 *
 * - POST   /api/videos/upload   上传视频
 * - GET    /api/videos          视频列表(分页)
 * - GET    /api/videos/{id}     视频详情
 * - GET    /api/videos/{id}/stream  视频流(支持Range/拖拽进度条)
 * - DELETE /api/videos/{id}     删除视频(逻辑删除)
 *
 * 所有接口都需要 Authorization: Bearer xxx
 */
@Tag(name = "视频管理")
@Slf4j
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;
    private final FileStorageService fileStorageService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "上传视频", description = "上传滑雪视频,支持MD5秒传。文件最大100MB,允许mp4/mov/m4v")
    @PostMapping("/upload")
    public ApiResult<VideoUploadResponse> upload(
            @Parameter(description = "视频文件", required = true)
            @RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtil.getUserIdOrThrow();
        VideoUploadResponse resp = videoService.upload(userId, file);
        return ApiResult.success(resp);
    }

    @Operation(summary = "我的视频列表", description = "分页查询当前用户的所有视频")
    @GetMapping
    public ApiResult<PageResult<VideoListItemVO>> list(@Valid VideoListQuery query) {
        Long userId = SecurityUtil.getUserIdOrThrow();
        PageResult<VideoListItemVO> page = videoService.list(userId, query);
        return ApiResult.success(page);
    }

    @Operation(summary = "视频详情", description = "查看某个视频的完整信息(包含分析数据)")
    @GetMapping("/{id}")
    public ApiResult<VideoDetailVO> detail(@PathVariable("id") Long id) {
        Long userId = SecurityUtil.getUserIdOrThrow();
        VideoDetailVO vo = videoService.getDetail(userId, id);
        return ApiResult.success(vo);
    }

    @Operation(summary = "删除视频", description = "逻辑删除视频(物理文件保留)")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable("id") Long id) {
        Long userId = SecurityUtil.getUserIdOrThrow();
        videoService.delete(userId, id);
        return ApiResult.success();
    }

    @Operation(summary = "视频流播放", description = "流式播放视频文件,支持 Range 请求(拖动进度条)。通过 ?token= 参数鉴权(<video>标签无法发送自定义Header)")
    @GetMapping("/{id}/stream")
    public void stream(
            @PathVariable("id") Long id,
            @RequestParam("token") String token,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        Long userId = jwtUtil.parseUserToken(token);   // 手动解析JWT(不走拦截器)
        Video video = videoService.getOwnedVideoOrThrow(userId, id);

        String absolutePath = fileStorageService.resolveAbsolutePath(video.getFilePath());
        Path path = Path.of(absolutePath);

        if (!Files.exists(path)) {
            throw new BusinessException(ResultCode.VIDEO_NOT_FOUND, "视频文件不存在");
        }

        long fileSize = Files.size(path);
        String contentType = Files.probeContentType(path);
        if (contentType == null) {
            contentType = "video/mp4";
        }

        response.setHeader("Accept-Ranges", "bytes");
        response.setContentType(contentType);

        String rangeHeader = request.getHeader("Range");

        if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
            response.setContentLengthLong(fileSize);
            Files.copy(path, response.getOutputStream());
            response.getOutputStream().flush();
            return;
        }

        // 解析 Range: "bytes=0-1023" 或 "bytes=1024-"
        String rangeValue = rangeHeader.substring(6);
        String[] parts = rangeValue.split("-");
        long start = Long.parseLong(parts[0]);
        long end = (parts.length > 1 && !parts[1].isEmpty())
                ? Long.parseLong(parts[1])
                : fileSize - 1;

        if (start >= fileSize || end >= fileSize || start > end) {
            response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            response.setHeader("Content-Range", "bytes */" + fileSize);
            return;
        }

        long rangeLength = end - start + 1;

        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);
        response.setContentLengthLong(rangeLength);

        try (var input = Files.newInputStream(path);
             var output = response.getOutputStream()) {
            input.skip(start);
            byte[] buffer = new byte[8192];
            long remaining = rangeLength;
            while (remaining > 0) {
                int len = (int) Math.min(buffer.length, remaining);
                int read = input.read(buffer, 0, len);
                if (read == -1) break;
                output.write(buffer, 0, read);
                remaining -= read;
            }
            output.flush();
        } catch (IOException e) {
            // 客户端断开连接是正常情况,不记录错误日志
            log.debug("视频流传输中断(客户端断开): videoId={}, userId={}", id, userId);
        }
    }
}