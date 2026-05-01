package com.skicoach.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.skicoach.backend.common.enums.VideoStatusEnum;
import com.skicoach.backend.common.exception.BusinessException;
import com.skicoach.backend.common.result.PageResult;
import com.skicoach.backend.common.result.ResultCode;
import com.skicoach.backend.common.util.FileUtil;
import com.skicoach.backend.dto.video.VideoDetailVO;
import com.skicoach.backend.dto.video.VideoListItemVO;
import com.skicoach.backend.dto.video.VideoListQuery;
import com.skicoach.backend.dto.video.VideoUploadResponse;
import com.skicoach.backend.entity.Video;
import com.skicoach.backend.mapper.VideoMapper;
import com.skicoach.backend.service.FileStorageService;
import com.skicoach.backend.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final VideoMapper videoMapper;
    private final FileStorageService fileStorageService;

    @Value("${ski.upload.allowed-extensions}")
    private String allowedExtensions;

    @Value("${ski.upload.max-file-size-mb}")
    private long maxFileSizeMB;

    @Override
    public VideoUploadResponse upload(Long userId, MultipartFile file) {
        // ============ 1. 基础校验 ============
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "上传的文件为空");
        }

        long maxBytes = maxFileSizeMB * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BusinessException(ResultCode.FILE_TOO_LARGE,
                    "文件大小超过上限 " + maxFileSizeMB + "MB");
        }

        // 校验扩展名
        String extension = FileUtil.getExtension(file.getOriginalFilename());
        if (!FileUtil.isExtensionAllowed(extension, allowedExtensions)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED,
                    "不支持的文件类型,允许: " + allowedExtensions);
        }

        // ============ 2. 计算 MD5 ============
        String md5;
        try {
            md5 = FileUtil.md5(file);
        } catch (IOException e) {
            log.error("计算MD5失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "文件读取失败");
        }

        // ============ 3. 秒传判断 ============
        Video existing = videoMapper.selectOne(
                new LambdaQueryWrapper<Video>()
                        .eq(Video::getUserId, userId)
                        .eq(Video::getFileMd5, md5)
                        .last("LIMIT 1")
        );
        if (existing != null) {
            log.info("秒传命中: userId={}, md5={}, existingVideoId={}", userId, md5, existing.getId());
            return buildUploadResponse(existing, true, "秒传成功(您已上传过相同文件)");
        }

        // ============ 4. 保存文件 ============
        String relativePath;
        try {
            relativePath = fileStorageService.saveVideo(file, userId);
        } catch (IOException e) {
            log.error("保存文件失败: userId={}", userId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "文件保存失败");
        }

        // ============ 5. 入库 ============
        Video video = new Video();
        video.setUserId(userId);
        video.setOriginalFilename(file.getOriginalFilename());
        video.setFilePath(relativePath);
        video.setFileMd5(md5);
        video.setFileSize(file.getSize());
        video.setAnalysisStatus(VideoStatusEnum.PENDING.getValue());
        video.setAnalysisVersion("v1.0");

        videoMapper.insert(video);
        log.info("视频入库: videoId={}, userId={}, filename={}",
                video.getId(), userId, file.getOriginalFilename());

        // P2.4 阶段会在这里触发异步分析任务

        return buildUploadResponse(video, false, "上传成功,等待分析");
    }

    @Override
    public PageResult<VideoListItemVO> list(Long userId, VideoListQuery query) {
        Page<Video> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<Video>()
                .eq(Video::getUserId, userId)
                .orderByDesc(Video::getCreatedTime);

        if (query.getAnalysisStatus() != null && !query.getAnalysisStatus().isBlank()) {
            wrapper.eq(Video::getAnalysisStatus, query.getAnalysisStatus());
        }

        Page<Video> result = videoMapper.selectPage(page, wrapper);

        // 转 VO
        List<VideoListItemVO> voList = result.getRecords().stream().map(v -> {
            VideoListItemVO vo = new VideoListItemVO();
            BeanUtils.copyProperties(v, vo);
            return vo;
        }).toList();

        Page<VideoListItemVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(voList);

        return PageResult.from(voPage);
    }

    @Override
    public VideoDetailVO getDetail(Long userId, Long videoId) {
        Video video = getOwnedVideoOrThrow(userId, videoId);
        VideoDetailVO vo = new VideoDetailVO();
        BeanUtils.copyProperties(video, vo);
        return vo;
    }

    @Override
    public void delete(Long userId, Long videoId) {
        Video video = getOwnedVideoOrThrow(userId, videoId);

        // 1. 逻辑删除数据库记录(MyBatis-Plus 自动设置 deletedTime = NOW())
        videoMapper.deleteById(videoId);

        // 2. 物理文件保留(后续可以加定时清理任务)
        // 如果需要立即物理删除,取消下面注释:
        // fileStorageService.delete(video.getFilePath());

        log.info("视频已删除(逻辑删除): videoId={}, userId={}", videoId, userId);
    }

    @Override
    public Video getOwnedVideoOrThrow(Long userId, Long videoId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            throw new BusinessException(ResultCode.VIDEO_NOT_FOUND);
        }
        if (!video.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.VIDEO_NOT_OWNED);
        }
        return video;
    }

    // -------- 私有工具 --------
    private VideoUploadResponse buildUploadResponse(Video video, boolean instantUpload, String message) {
        VideoUploadResponse resp = new VideoUploadResponse();
        resp.setVideoId(video.getId());
        resp.setOriginalFilename(video.getOriginalFilename());
        resp.setFileSize(video.getFileSize());
        resp.setAnalysisStatus(video.getAnalysisStatus());
        resp.setInstantUpload(instantUpload);
        resp.setMessage(message);
        return resp;
    }
}
