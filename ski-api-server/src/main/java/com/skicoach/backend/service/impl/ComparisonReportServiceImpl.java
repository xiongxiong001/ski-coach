package com.skicoach.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.skicoach.backend.common.enums.VideoStatusEnum;
import com.skicoach.backend.common.exception.BusinessException;
import com.skicoach.backend.common.result.PageResult;
import com.skicoach.backend.common.result.ResultCode;
import com.skicoach.backend.dto.comparison.ComparisonListItemVO;
import com.skicoach.backend.dto.comparison.ComparisonReportVO;
import com.skicoach.backend.dto.comparison.CreateComparisonRequest;
import com.skicoach.backend.entity.ComparisonReport;
import com.skicoach.backend.entity.Video;
import com.skicoach.backend.mapper.ComparisonReportMapper;
import com.skicoach.backend.service.AnalysisTaskService;
import com.skicoach.backend.service.ComparisonReportService;
import com.skicoach.backend.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComparisonReportServiceImpl implements ComparisonReportService {

    private final ComparisonReportMapper mapper;
    private final VideoService videoService;
    private final AnalysisTaskService taskService;

    @Override
    public CreateComparisonResult create(Long userId, CreateComparisonRequest req) {
        Long prevId = req.getPrevVideoId();
        Long currId = req.getCurrVideoId();

        if (prevId.equals(currId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "两个视频不能相同");
        }

        // 步骤1: 幂等检查 - 该视频对是否已有报告
        ComparisonReport existing = findByVideoPair(prevId, currId);
        if (existing != null) {
            // 校验所属
            if (!existing.getUserId().equals(userId)) {
                throw new BusinessException(ResultCode.FORBIDDEN);
            }
            log.info("[对比] 命中已有报告: reportId={}, userId={}", existing.getId(), userId);
            return new CreateComparisonResult(null, existing.getId(), true);
        }

        // 步骤2: 校验两个视频都属于该用户、都已分析完成
        Video prev = videoService.getOwnedVideoOrThrow(userId, prevId);
        Video curr = videoService.getOwnedVideoOrThrow(userId, currId);

        if (!VideoStatusEnum.ANALYZED.getValue().equals(prev.getAnalysisStatus())) {
            throw new BusinessException(ResultCode.VIDEO_NOT_ANALYZED,
                    "上次视频还未完成分析,无法对比");
        }
        if (!VideoStatusEnum.ANALYZED.getValue().equals(curr.getAnalysisStatus())) {
            throw new BusinessException(ResultCode.VIDEO_NOT_ANALYZED,
                    "本次视频还未完成分析,无法对比");
        }

        // 步骤3: 创建任务并入队
        Long taskId = taskService.enqueueComparison(userId, prevId, currId);
        log.info("[对比] 创建任务: taskId={}, userId={}, prev={}, curr={}",
                taskId, userId, prevId, currId);

        return new CreateComparisonResult(taskId, null, false);
    }

    @Override
    public ComparisonReportVO getDetail(Long userId, Long reportId) {
        ComparisonReport report = mapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ResultCode.REPORT_NOT_FOUND);
        }
        if (!report.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        ComparisonReportVO vo = new ComparisonReportVO();
        BeanUtils.copyProperties(report, vo);
        return vo;
    }

    @Override
    public PageResult<ComparisonListItemVO> list(Long userId, Long pageNum, Long pageSize) {
        Page<ComparisonReport> page = new Page<>(pageNum, pageSize);
        Page<ComparisonReport> result = mapper.selectPage(page,
                new LambdaQueryWrapper<ComparisonReport>()
                        .eq(ComparisonReport::getUserId, userId)
                        .orderByDesc(ComparisonReport::getCreatedTime));

        List<ComparisonListItemVO> vos = result.getRecords().stream().map(r -> {
            ComparisonListItemVO vo = new ComparisonListItemVO();
            BeanUtils.copyProperties(r, vo);
            return vo;
        }).toList();

        Page<ComparisonListItemVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(vos);
        return PageResult.from(voPage);
    }

    @Override
    public ComparisonReport findByVideoPair(Long prevVideoId, Long currVideoId) {
        return mapper.selectOne(
                new LambdaQueryWrapper<ComparisonReport>()
                        .eq(ComparisonReport::getPrevVideoId, prevVideoId)
                        .eq(ComparisonReport::getCurrVideoId, currVideoId)
                        .last("LIMIT 1")
        );
    }

    @Override
    public Long createReportRecord(Long taskId, Long userId, Long prevVideoId, Long currVideoId,
                                    String comparisonDataJson, String reportMarkdown,
                                    Integer improvedCount, Integer declinedCount, Integer stabilityImprovedCount,
                                    Integer inputTokens, Integer outputTokens, BigDecimal costYuan) {
        ComparisonReport report = new ComparisonReport();
        report.setTaskId(taskId);
        report.setUserId(userId);
        report.setPrevVideoId(prevVideoId);
        report.setCurrVideoId(currVideoId);
        report.setComparisonDataJson(comparisonDataJson);
        report.setReportMarkdown(reportMarkdown);
        report.setImprovedCount(improvedCount != null ? improvedCount : 0);
        report.setDeclinedCount(declinedCount != null ? declinedCount : 0);
        report.setStabilityImprovedCount(stabilityImprovedCount != null ? stabilityImprovedCount : 0);
        report.setLlmInputTokens(inputTokens);
        report.setLlmOutputTokens(outputTokens);
        report.setLlmCostYuan(costYuan);
        mapper.insert(report);
        log.info("[对比报告创建] reportId={}, prev={}, curr={}", report.getId(), prevVideoId, currVideoId);
        return report.getId();
    }
}
