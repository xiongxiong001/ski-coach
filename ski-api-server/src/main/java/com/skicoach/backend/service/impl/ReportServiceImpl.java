package com.skicoach.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.skicoach.backend.common.exception.BusinessException;
import com.skicoach.backend.common.result.PageResult;
import com.skicoach.backend.common.result.ResultCode;
import com.skicoach.backend.dto.report.ReportDetailVO;
import com.skicoach.backend.entity.Report;
import com.skicoach.backend.entity.Video;
import com.skicoach.backend.mapper.ReportMapper;
import com.skicoach.backend.mapper.VideoMapper;
import com.skicoach.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;
    private final VideoMapper videoMapper;

    @Override
    public Long createReport(Long taskId, Long videoId, Long userId, String reportMarkdown,
                              Integer inputTokens, Integer outputTokens, BigDecimal costYuan) {
        Report report = new Report();
        report.setTaskId(taskId);
        report.setVideoId(videoId);
        report.setUserId(userId);
        report.setReportMarkdown(reportMarkdown);
        report.setLlmInputTokens(inputTokens);
        report.setLlmOutputTokens(outputTokens);
        report.setLlmCostYuan(costYuan);
        reportMapper.insert(report);
        log.info("[报告创建] reportId={}, videoId={}, userId={}", report.getId(), videoId, userId);
        return report.getId();
    }

    @Override
    public ReportDetailVO getDetail(Long userId, Long reportId) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ResultCode.REPORT_NOT_FOUND);
        }
        if (!report.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "报告不属于当前用户");
        }
        ReportDetailVO vo = new ReportDetailVO();
        BeanUtils.copyProperties(report, vo);
        return vo;
    }

    @Override
    public PageResult<ReportDetailVO> list(Long userId, Long pageNum, Long pageSize) {
        Page<Report> page = new Page<>(pageNum, pageSize);
        Page<Report> result = reportMapper.selectPage(page,
                new LambdaQueryWrapper<Report>()
                        .eq(Report::getUserId, userId)
                        .orderByDesc(Report::getCreatedTime));

        // 批量加载视频文件名(避免 N+1)
        List<Long> videoIds = result.getRecords().stream()
                .map(Report::getVideoId).distinct().toList();
        Map<Long, String> filenameMap = videoIds.isEmpty() ? Collections.emptyMap()
                : videoMapper.selectBatchIds(videoIds).stream()
                        .collect(Collectors.toMap(Video::getId, Video::getOriginalFilename, (a, b) -> a));

        List<ReportDetailVO> vos = result.getRecords().stream().map(r -> {
            ReportDetailVO vo = new ReportDetailVO();
            BeanUtils.copyProperties(r, vo);
            vo.setVideoFilename(filenameMap.get(r.getVideoId()));
            return vo;
        }).toList();

        Page<ReportDetailVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(vos);

        return PageResult.from(voPage);
    }

    @Override
    public Report findByVideoId(Long videoId) {
        return reportMapper.selectOne(
                new LambdaQueryWrapper<Report>()
                        .eq(Report::getVideoId, videoId)
                        .last("LIMIT 1")
        );
    }
}
