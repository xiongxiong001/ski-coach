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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

    @Value("${video.use-nginx:false}")
    private boolean useNginx;

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


    // 缓冲区大小: 64KB（视频流最优值）
    private static final int BUFFER_SIZE = 64 * 1024;

    @Operation(summary = "视频流播放", description = "流式播放视频文件,支持 Range 请求(拖动进度条)。通过 ?token= 参数鉴权")
    @GetMapping("/{id}/stream")
    public void stream(
            @PathVariable("id") Long id,
            @RequestParam("token") String token,
            HttpServletRequest request,
            HttpServletResponse response) {
        log.info("视频流播放: id={}, token={}", id, token);
        // 1. 鉴权
        Long userId = jwtUtil.parseUserToken(token);
        Video video = videoService.getOwnedVideoOrThrow(userId, id);
        String dbFilePath = video.getFilePath();
        // 【线上环境】用 Nginx 高性能传输
        if (useNginx) {
            log.info("use nginx to stream video {}", id);
            response.setHeader("X-Accel-Redirect", "/api/video_files/" + dbFilePath);
            response.setContentType("video/mp4");
            return;
        }
        log.info("run this way");
        // 2. 获取文件路径
        String absolutePath = fileStorageService.resolveAbsolutePath(dbFilePath);
        Path filePath = Path.of(absolutePath);

        if (!Files.exists(filePath)) {
            throw new BusinessException(ResultCode.VIDEO_NOT_FOUND, "视频文件不存在");
        }

        try {
            long fileSize = Files.size(filePath);
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "video/mp4";
            }

            // 3. 生成 ETag（文件大小 + 修改时间）
            String eTag = generateETag(fileSize, Files.getLastModifiedTime(filePath).toMillis());

            // 4. 基础响应头
            response.setHeader("Accept-Ranges", "bytes");
            response.setContentType(contentType);
            response.setHeader("ETag", eTag);
            response.setHeader("Cache-Control", "public, max-age=3600");
            response.setHeader("Content-Disposition", "inline; filename=\"" + video.getOriginalFilename() + "\"");

            // 5. 缓存命中直接返回 304
            String ifNoneMatch = request.getHeader("If-None-Match");
            if (ifNoneMatch != null && ifNoneMatch.equals(eTag)) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }

            String rangeHeader = request.getHeader("Range");

            // 6. 无 Range → 全量返回
            if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
                response.setContentLengthLong(fileSize);
                try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(filePath), BUFFER_SIZE);
                     OutputStream os = response.getOutputStream()) {

                    byte[] buffer = new byte[BUFFER_SIZE];
                    int len;
                    while ((len = bis.read(buffer)) != -1) {
                        os.write(buffer, 0, len);
                    }
                    os.flush();
                }
                return;
            }

            // 7. 解析 Range：bytes=start-end
            String range = rangeHeader.substring(6);
            String[] split = range.split("-");
            long start = Long.parseLong(split[0]);
            long end = (split.length > 1 && !split[1].isEmpty()) ? Long.parseLong(split[1]) : fileSize - 1;

            // 范围非法 → 416
            if (end >= fileSize || start > end) {
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                response.setHeader("Content-Range", "bytes */" + fileSize);
                return;
            }

            long contentLength = end - start + 1;

            // 8. 分段响应 206
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);
            response.setContentLengthLong(contentLength);

            // 9. 随机读取文件（支持大文件、低内存）
            try (RandomAccessFile raf = new RandomAccessFile(absolutePath, "r");
                 OutputStream os = response.getOutputStream()) {

                raf.seek(start);
                byte[] buffer = new byte[BUFFER_SIZE];
                long remain = contentLength;

                while (remain > 0) {
                    int readLen = (int) Math.min(BUFFER_SIZE, remain);
                    int read = raf.read(buffer, 0, readLen);
                    if (read == -1) break;

                    os.write(buffer, 0, read);
                    remain -= read;
                }
                os.flush();
            }

        } catch (IOException e) {
            // 客户端断开连接属于正常行为，只打 debug 日志
            log.debug("视频流传输中断：videoId={}, userId={}, 异常信息：{}", id, userId, e.getMessage());
        } catch (Exception e) {
            log.error("视频流播放异常：videoId={}, userId={}", id, userId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "视频播放失败");
        }
    }

    /**
     * 生成 ETag（MD5 保证唯一）
     */
    private String generateETag(long fileSize, long lastModified) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(String.valueOf(fileSize).getBytes());
            md.update(String.valueOf(lastModified).getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) {
                sb.append(String.format("%02x", b));
            }
            return "\"" + sb + "\"";
        } catch (NoSuchAlgorithmException e) {
            return "\"" + fileSize + "-" + lastModified + "\"";
        }
    }
}