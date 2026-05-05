package com.skicoach.backend.service;

import com.skicoach.backend.common.result.PageResult;
import com.skicoach.backend.dto.feedback.FeedbackCreateRequest;
import com.skicoach.backend.dto.feedback.FeedbackStatsVO;
import com.skicoach.backend.dto.feedback.FeedbackVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * 用户反馈服务
 */
public interface FeedbackService {

    /** 提交反馈 */
    void submit(Long userId, FeedbackCreateRequest request, List<MultipartFile> images) throws IOException;

    /** 我的反馈列表(分页), type可选筛选 */
    PageResult<FeedbackVO> list(Long userId, Long pageNum, Long pageSize, String type);

    /** 反馈详情 */
    FeedbackVO getDetail(Long userId, Long feedbackId);

    /** 反馈统计(按状态) */
    FeedbackStatsVO getStats(Long userId);

    /** 解析反馈图片路径(用于流式查看) */
    Path resolveImagePath(Long userId, Long feedbackId, int index);
}
