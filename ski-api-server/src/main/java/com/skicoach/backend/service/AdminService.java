package com.skicoach.backend.service;

import com.skicoach.backend.common.result.PageResult;
import com.skicoach.backend.dto.admin.*;

import java.time.LocalDate;
import java.util.List;

public interface AdminService {

    // ============== 鉴权 ==============
    AdminLoginResponse login(AdminLoginRequest request);

    void logout(String token);

    // ============== 用户管理 ==============
    PageResult<AdminUserVO> listUsers(UserListQuery query);

    AdminUserVO getUserDetail(Long userId);

    void updateUserStatus(Long userId, Integer status);

    // ============== 任务管理 ==============
    PageResult<TaskListItemVO> listTasks(TaskListQuery query);

    TaskListItemVO getTaskDetail(Long taskId);

    /** 重试失败任务(把任务重新入队) */
    void retryTask(Long taskId);

    // ============== 数据统计 ==============
    OverviewStatsVO getOverview();

    List<DailyStatsVO> getDailyStats(LocalDate startDate, LocalDate endDate);

    List<LlmCostStatsVO> getLlmCostStats();

    StorageStatsVO getStorageStats();
}
