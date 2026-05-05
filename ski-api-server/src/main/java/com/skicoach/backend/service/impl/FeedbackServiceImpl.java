package com.skicoach.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.skicoach.backend.common.exception.BusinessException;
import com.skicoach.backend.common.result.PageResult;
import com.skicoach.backend.common.result.ResultCode;
import com.skicoach.backend.dto.feedback.FeedbackCreateRequest;
import com.skicoach.backend.dto.feedback.FeedbackStatsVO;
import com.skicoach.backend.dto.feedback.FeedbackVO;
import com.skicoach.backend.entity.Feedback;
import com.skicoach.backend.mapper.FeedbackMapper;
import com.skicoach.backend.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackMapper feedbackMapper;

    @Value("${ski.storage.local-base-path}")
    private String basePath;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy_MM");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final int MAX_IMAGES = 5;
    private static final int MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB

    @Override
    public void submit(Long userId, FeedbackCreateRequest request, List<MultipartFile> images) throws IOException {
        List<String> imagePaths = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            if (images.size() > MAX_IMAGES) {
                throw new BusinessException(ResultCode.FEEDBACK_IMAGE_TOO_MANY);
            }
            for (MultipartFile img : images) {
                if (img.isEmpty()) continue;
                if (img.getSize() > MAX_IMAGE_SIZE) {
                    throw new BusinessException(ResultCode.FEEDBACK_IMAGE_TOO_MANY, "单张图片最大5MB");
                }
                String ext = getExtension(img.getOriginalFilename());
                if (ext == null || !ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
                    throw new BusinessException(ResultCode.FEEDBACK_IMAGE_TOO_MANY,
                            "仅支持 jpg/png/gif/webp 格式");
                }
            }
        }

        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setType(request.getType());
        feedback.setContent(request.getContent());
        feedback.setContact(request.getContact());
        feedback.setAppVersion(request.getAppVersion());
        feedback.setStatus(0);
        feedbackMapper.insert(feedback);

        if (images != null && !images.isEmpty()) {
            String month = LocalDate.now().format(MONTH_FMT);
            for (int i = 0; i < images.size(); i++) {
                MultipartFile img = images.get(i);
                if (img.isEmpty()) continue;
                String ext = getExtension(img.getOriginalFilename());
                String filename = String.format("%d_%d_%s.%s",
                        feedback.getId(), i, UUID.randomUUID().toString().substring(0, 4), ext);
                String relativePath = String.join("/", "feedbacks", String.valueOf(userId), month, filename);
                Path absolutePath = Paths.get(basePath, relativePath);
                Files.createDirectories(absolutePath.getParent());
                Files.copy(img.getInputStream(), absolutePath);
                imagePaths.add(relativePath);
            }
        }

        if (!imagePaths.isEmpty()) {
            feedback.setImages(String.join(",", imagePaths));
            feedbackMapper.updateById(feedback);
        }

        log.info("反馈已提交: feedbackId={}, userId={}, type={}, images={}",
                feedback.getId(), userId, request.getType(), imagePaths.size());
    }

    @Override
    public PageResult<FeedbackVO> list(Long userId, Long pageNum, Long pageSize, String type) {
        LambdaQueryWrapper<Feedback> qw = new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getUserId, userId)
                .orderByDesc(Feedback::getCreatedTime);
        if (type != null && !type.isEmpty()) {
            qw.eq(Feedback::getType, type);
        }

        Page<Feedback> page = new Page<>(pageNum, pageSize);
        Page<Feedback> result = feedbackMapper.selectPage(page, qw);

        List<FeedbackVO> vos = result.getRecords().stream().map(f -> {
            FeedbackVO vo = new FeedbackVO();
            BeanUtils.copyProperties(f, vo);
            vo.setImageCount(f.getImages() == null || f.getImages().isEmpty() ? 0
                    : f.getImages().split(",").length);
            return vo;
        }).toList();

        Page<FeedbackVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(vos);
        return PageResult.from(voPage);
    }

    @Override
    public FeedbackVO getDetail(Long userId, Long feedbackId) {
        Feedback feedback = feedbackMapper.selectById(feedbackId);
        if (feedback == null) {
            throw new BusinessException(ResultCode.FEEDBACK_NOT_FOUND);
        }
        if (!feedback.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "反馈不属于当前用户");
        }
        FeedbackVO vo = new FeedbackVO();
        BeanUtils.copyProperties(feedback, vo);
        vo.setImageCount(feedback.getImages() == null || feedback.getImages().isEmpty() ? 0
                : feedback.getImages().split(",").length);
        return vo;
    }

    @Override
    public FeedbackStatsVO getStats(Long userId) {
        List<Feedback> all = feedbackMapper.selectList(
                new LambdaQueryWrapper<Feedback>()
                        .eq(Feedback::getUserId, userId)
                        .select(Feedback::getStatus));

        long pending = all.stream().filter(f -> f.getStatus() == 0).count();
        long viewed = all.stream().filter(f -> f.getStatus() == 1).count();
        long replied = all.stream().filter(f -> f.getStatus() == 2).count();

        FeedbackStatsVO vo = new FeedbackStatsVO();
        vo.setTotalCount((long) all.size());
        vo.setPendingCount(pending);
        vo.setViewedCount(viewed);
        vo.setRepliedCount(replied);
        return vo;
    }

    @Override
    public Path resolveImagePath(Long userId, Long feedbackId, int index) {
        Feedback feedback = feedbackMapper.selectById(feedbackId);
        if (feedback == null) {
            throw new BusinessException(ResultCode.FEEDBACK_NOT_FOUND);
        }
        if (!feedback.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "反馈不属于当前用户");
        }
        if (feedback.getImages() == null || feedback.getImages().isEmpty()) {
            throw new BusinessException(ResultCode.FEEDBACK_NOT_FOUND, "反馈无图片");
        }
        String[] paths = feedback.getImages().split(",");
        if (index < 0 || index >= paths.length) {
            throw new BusinessException(ResultCode.FEEDBACK_NOT_FOUND, "图片索引超出范围");
        }
        return Paths.get(basePath, paths[index]);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return null;
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}