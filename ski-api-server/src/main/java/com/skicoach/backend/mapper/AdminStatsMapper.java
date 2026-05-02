package com.skicoach.backend.mapper;

import com.skicoach.backend.dto.admin.DailyStatsVO;
import com.skicoach.backend.dto.admin.LlmCostStatsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 数据统计相关的 Mapper(自定义 SQL)
 */
@Mapper
public interface AdminStatsMapper {

    /** 总用户数 */
    Long countTotalUsers();

    /** 今日新增用户 */
    Long countNewUsersInRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 总视频数(不含逻辑删除) */
    Long countActiveVideos();

    /** 已逻辑删除的视频数 */
    Long countDeletedVideos();

    /** 总视频大小(字节) */
    Long sumVideoFileSize();

    /** 某日期范围内上传的视频数 */
    Long countVideosInRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 总报告数(单次+对比) */
    Long countTotalReports();

    /** 某日期范围内的报告数 */
    Long countReportsInRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 当前 running 状态任务数 */
    Long countRunningTasks();

    /** failed 状态任务数 */
    Long countFailedTasks();

    /** 累计 LLM 成本 */
    java.math.BigDecimal sumLlmCostTotal();

    /** 某日期范围内的 LLM 成本 */
    java.math.BigDecimal sumLlmCostInRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 每日统计 */
    List<DailyStatsVO> selectDailyStats(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** LLM 成本按任务类型聚合 */
    List<LlmCostStatsVO> selectLlmCostByTaskType();
}
