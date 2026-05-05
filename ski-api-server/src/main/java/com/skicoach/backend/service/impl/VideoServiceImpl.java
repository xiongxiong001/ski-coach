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
import com.skicoach.backend.service.AnalysisTaskService;
import com.skicoach.backend.service.FileStorageService;
import com.skicoach.backend.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final VideoMapper videoMapper;
    private final FileStorageService fileStorageService;
    private final AnalysisTaskService analysisTaskService;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${ski.upload.allowed-extensions}")
    private String allowedExtensions;

    @Value("${ski.upload.max-file-size-mb}")
    private long maxFileSizeMB;

    @Value("${ski.upload.daily-limit:10}")
    private int dailyUploadLimit;

    @Value("${ski.upload.total-limit:50}")
    private int totalUploadLimit;

    @Value("${ski.upload.rate-limit-per-minute:5}")
    private int rateLimitPerMinute;

    // Redis Key 常量
    private static final String RATE_LIMIT_KEY_PREFIX = "video:upload:rate:";

    @Override
    public VideoUploadResponse upload(Long userId, MultipartFile file) {
        // ============ 0. 上传限流检查 ============
        checkUploadRateLimit(userId);
        checkDailyUploadLimit(userId);
        checkTotalUploadLimit(userId);

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

        // 触发异步分析任务(P2.4 新增)
        analysisTaskService.enqueueSingleAnalysis(userId, video.getId());

        return buildUploadResponse(video, false, "上传成功,正在分析中...");
    }

    @Override
    public PageResult<VideoListItemVO> list(Long userId, VideoListQuery query) {
        Page<Video> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<Video>()
                .eq(Video::getUserId, userId)
                .orderByDesc(Video::getCreatedTime);

        if (query.getAnalysisStatus() != null && !query.getAnalysisStatus().isBlank()) {
            // 当筛选"分析中"时,包含 pending 和 analyzing 两种状态
            if ("analyzing".equals(query.getAnalysisStatus())) {
                wrapper.in(Video::getAnalysisStatus, VideoStatusEnum.PENDING.getValue(), VideoStatusEnum.ANALYZING.getValue());
            } else {
                wrapper.eq(Video::getAnalysisStatus, query.getAnalysisStatus());
            }
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

    // -------- 限流检查方法 --------

    /**
     * 检查上传速率限制(防止短时间大量上传)
     */
    private void checkUploadRateLimit(Long userId) {
        String key = RATE_LIMIT_KEY_PREFIX + userId;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        
        // 第一次设置时添加过期时间
        if (count == 1) {
            stringRedisTemplate.expire(key, 60, TimeUnit.SECONDS);
        }
        
        if (count > rateLimitPerMinute) {
            throw new BusinessException(ResultCode.RATE_LIMIT_EXCEEDED,
                    "上传过于频繁，请稍后再试(每分钟最多" + rateLimitPerMinute + "次)");
        }
    }

    /**
     * 检查每日上传数量限制
     */
    private void checkDailyUploadLimit(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        
        long todayCount = videoMapper.selectCount(
                new LambdaQueryWrapper<Video>()
                        .eq(Video::getUserId, userId)
                        .ge(Video::getCreatedTime, startOfDay)
                        .lt(Video::getCreatedTime, endOfDay)
        );
        
        if (todayCount >= dailyUploadLimit) {
            throw new BusinessException(ResultCode.UPLOAD_LIMIT_EXCEEDED,
                    "今日上传次数已达上限(最多" + dailyUploadLimit + "次)");
        }
    }

    /**
     * 检查总视频数量限制
     */
    private void checkTotalUploadLimit(Long userId) {
        long totalCount = videoMapper.selectCount(
                new LambdaQueryWrapper<Video>().eq(Video::getUserId, userId)
        );
        
        if (totalCount >= totalUploadLimit) {
            throw new BusinessException(ResultCode.QUOTA_EXCEEDED,
                    "视频数量已达上限(最多" + totalUploadLimit + "个)");
        }
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