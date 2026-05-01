package com.skicoach.backend.controller.user;

import com.skicoach.backend.common.result.ApiResult;
import com.skicoach.backend.common.result.PageResult;
import com.skicoach.backend.common.util.SecurityUtil;
import com.skicoach.backend.dto.video.VideoDetailVO;
import com.skicoach.backend.dto.video.VideoListItemVO;
import com.skicoach.backend.dto.video.VideoListQuery;
import com.skicoach.backend.dto.video.VideoUploadResponse;
import com.skicoach.backend.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * 视频接口
 *
 * - POST   /api/videos/upload   上传视频
 * - GET    /api/videos          视频列表(分页)
 * - GET    /api/videos/{id}     视频详情
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
}
