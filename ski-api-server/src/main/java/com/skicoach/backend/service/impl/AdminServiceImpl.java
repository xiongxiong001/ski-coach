package com.skicoach.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.skicoach.backend.common.constant.RedisKeyConstant;
import com.skicoach.backend.common.enums.TaskStatusEnum;
import com.skicoach.backend.common.enums.UserStatusEnum;
import com.skicoach.backend.common.exception.BusinessException;
import com.skicoach.backend.common.result.PageResult;
import com.skicoach.backend.common.result.ResultCode;
import com.skicoach.backend.common.util.JwtUtil;
import com.skicoach.backend.common.util.PasswordUtil;
import com.skicoach.backend.dto.admin.*;
import com.skicoach.backend.entity.Admin;
import com.skicoach.backend.entity.AnalysisTask;
import com.skicoach.backend.entity.User;
import com.skicoach.backend.mapper.*;
import com.skicoach.backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminMapper adminMapper;
    private final UserMapper userMapper;
    private final VideoMapper videoMapper;
    private final ReportMapper reportMapper;
    private final AnalysisTaskMapper taskMapper;
    private final AdminStatsMapper statsMapper;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;

    @Value("${ski.jwt.expire-hours:168}")
    private long expireHours;

    @Value("${ski.storage.local-base-path}")
    private String storageBasePath;

    // ============== 鉴权 ==============

    @Override
    public AdminLoginResponse login(AdminLoginRequest request) {
        Admin admin = adminMapper.selectOne(
                new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, request.getUsername())
        );
        if (admin == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (!PasswordUtil.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_WRONG);
        }
        if (!UserStatusEnum.NORMAL.getValue().equals(admin.getStatus())) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 更新最后登录时间
        Admin update = new Admin();
        update.setId(admin.getId());
        update.setLastLoginTime(LocalDateTime.now());
        adminMapper.updateById(update);

        log.info("管理员登录: adminId={}, username={}", admin.getId(), admin.getUsername());

        AdminLoginResponse resp = new AdminLoginResponse();
        resp.setToken(jwtUtil.generateAdminToken(admin.getId()));
        resp.setExpireHours(expireHours);
        resp.setAdminId(admin.getId());
        resp.setUsername(admin.getUsername());
        resp.setRealName(admin.getRealName());
        return resp;
    }

    @Override
    public void logout(String token) {
        if (token == null || token.isEmpty()) return;
        long remainingMillis = jwtUtil.getRemainingMillis(token, JwtUtil.TokenType.ADMIN);
        if (remainingMillis <= 0) return;
        stringRedisTemplate.opsForValue().set(
                RedisKeyConstant.ADMIN_TOKEN_BLACKLIST + token,
                "1",
                remainingMillis,
                TimeUnit.MILLISECONDS
        );
        log.info("管理员登出,Token加入黑名单");
    }

    // ============== 用户管理 ==============

    @Override
    public PageResult<AdminUserVO> listUsers(UserListQuery query) {
        Page<User> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getCreatedTime);

        if (query.getPhone() != null && !query.getPhone().isBlank()) {
            wrapper.like(User::getPhone, query.getPhone());
        }
        if (query.getStatus() != null) {
            wrapper.eq(User::getStatus, query.getStatus());
        }

        Page<User> result = userMapper.selectPage(page, wrapper);

        // 转 VO 并查询每个用户的视频数和报告数
        List<AdminUserVO> vos = result.getRecords().stream().map(u -> {
            AdminUserVO vo = new AdminUserVO();
            BeanUtils.copyProperties(u, vo);
            // 视频数(不含逻辑删除,MP 自动过滤)
            Long videoCount = videoMapper.selectCount(
                    new LambdaQueryWrapper<com.skicoach.backend.entity.Video>()
                            .eq(com.skicoach.backend.entity.Video::getUserId, u.getId())
            );
            vo.setVideoCount(videoCount.intValue());
            // 报告数
            Long reportCount = reportMapper.selectCount(
                    new LambdaQueryWrapper<com.skicoach.backend.entity.Report>()
                            .eq(com.skicoach.backend.entity.Report::getUserId, u.getId())
            );
            vo.setReportCount(reportCount.intValue());
            return vo;
        }).toList();

        Page<AdminUserVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(vos);
        return PageResult.from(voPage);
    }

    @Override
    public AdminUserVO getUserDetail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        AdminUserVO vo = new AdminUserVO();
        BeanUtils.copyProperties(user, vo);
        Long videoCount = videoMapper.selectCount(
                new LambdaQueryWrapper<com.skicoach.backend.entity.Video>()
                        .eq(com.skicoach.backend.entity.Video::getUserId, userId)
        );
        vo.setVideoCount(videoCount.intValue());
        Long reportCount = reportMapper.selectCount(
                new LambdaQueryWrapper<com.skicoach.backend.entity.Report>()
                        .eq(com.skicoach.backend.entity.Report::getUserId, userId)
        );
        vo.setReportCount(reportCount.intValue());
        return vo;
    }

    @Override
    public void updateUserStatus(Long userId, Integer status) {
        if (status != 0 && status != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "status 只能是 0 或 1");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        User update = new User();
        update.setId(userId);
        update.setStatus(status);
        userMapper.updateById(update);
        log.info("[管理员操作] 修改用户状态: userId={}, newStatus={}", userId, status);
    }

    // ============== 任务管理 ==============

    @Override
    public PageResult<TaskListItemVO> listTasks(TaskListQuery query) {
        Page<AnalysisTask> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<AnalysisTask> wrapper = new LambdaQueryWrapper<AnalysisTask>()
                .orderByDesc(AnalysisTask::getCreatedTime);

        if (query.getTaskType() != null && !query.getTaskType().isBlank()) {
            wrapper.eq(AnalysisTask::getTaskType, query.getTaskType());
        }
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            wrapper.eq(AnalysisTask::getStatus, query.getStatus());
        }
        if (query.getUserId() != null) {
            wrapper.eq(AnalysisTask::getUserId, query.getUserId());
        }

        Page<AnalysisTask> result = taskMapper.selectPage(page, wrapper);

        List<TaskListItemVO> vos = result.getRecords().stream().map(t -> {
            TaskListItemVO vo = new TaskListItemVO();
            BeanUtils.copyProperties(t, vo);
            return vo;
        }).toList();

        Page<TaskListItemVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(vos);
        return PageResult.from(voPage);
    }

    @Override
    public TaskListItemVO getTaskDetail(Long taskId) {
        AnalysisTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "任务不存在");
        }
        TaskListItemVO vo = new TaskListItemVO();
        BeanUtils.copyProperties(task, vo);
        return vo;
    }

    @Override
    public void retryTask(Long taskId) {
        AnalysisTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "任务不存在");
        }
        if (!TaskStatusEnum.FAILED.getValue().equals(task.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "只能重试失败的任务");
        }

        // 重置状态
        AnalysisTask update = new AnalysisTask();
        update.setId(taskId);
        update.setStatus(TaskStatusEnum.PENDING.getValue());
        update.setErrorMessage("");
        update.setRetryCount((task.getRetryCount() == null ? 0 : task.getRetryCount()) + 1);
        update.setStartTime(null);
        update.setFinishTime(null);
        taskMapper.updateById(update);

        // 重新入队
        RBlockingQueue<Long> queue = redissonClient.getBlockingQueue(RedisKeyConstant.TASK_QUEUE);
        queue.offer(taskId);

        log.info("[管理员操作] 重试任务: taskId={}, retryCount={}", taskId, update.getRetryCount());
    }

    // ============== 数据统计 ==============

    @Override
    public OverviewStatsVO getOverview() {
        OverviewStatsVO vo = new OverviewStatsVO();
        LocalDate today = LocalDate.now();

        vo.setTotalUsers(safeLong(statsMapper.countTotalUsers()));
        vo.setNewUsersToday(safeLong(statsMapper.countNewUsersInRange(today, today)));
        vo.setTotalVideos(safeLong(statsMapper.countActiveVideos()));
        vo.setVideosToday(safeLong(statsMapper.countVideosInRange(today, today)));
        vo.setTotalReports(safeLong(statsMapper.countTotalReports()));
        vo.setReportsToday(safeLong(statsMapper.countReportsInRange(today, today)));
        vo.setRunningTasks(safeLong(statsMapper.countRunningTasks()));
        vo.setFailedTasks(safeLong(statsMapper.countFailedTasks()));

        BigDecimal costTotal = statsMapper.sumLlmCostTotal();
        BigDecimal costToday = statsMapper.sumLlmCostInRange(today, today);
        vo.setLlmCostTotal(costTotal != null ? costTotal : BigDecimal.ZERO);
        vo.setLlmCostToday(costToday != null ? costToday : BigDecimal.ZERO);

        return vo;
    }

    @Override
    public List<DailyStatsVO> getDailyStats(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            endDate = LocalDate.now();
            startDate = endDate.minusDays(6);  // 默认查最近7天
        }
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "开始日期不能晚于结束日期");
        }

        List<DailyStatsVO> dbResult = statsMapper.selectDailyStats(startDate, endDate);
        // SQL返回的可能不是连续日期(没数据的日期不会出现),Java端补全
        Map<LocalDate, DailyStatsVO> map = new HashMap<>();
        for (DailyStatsVO row : dbResult) {
            map.put(row.getStatDate(), row);
        }

        List<DailyStatsVO> result = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            DailyStatsVO row = map.get(d);
            if (row == null) {
                row = new DailyStatsVO();
                row.setStatDate(d);
                row.setNewUsers(0L);
                row.setVideoCount(0L);
                row.setTaskSuccess(0L);
                row.setTaskFailed(0L);
                row.setLlmCost(BigDecimal.ZERO);
            }
            result.add(row);
        }
        return result;
    }

    @Override
    public List<LlmCostStatsVO> getLlmCostStats() {
        return statsMapper.selectLlmCostByTaskType();
    }

    @Override
    public StorageStatsVO getStorageStats() {
        StorageStatsVO vo = new StorageStatsVO();
        vo.setBasePath(storageBasePath);
        vo.setActiveVideos(safeLong(statsMapper.countActiveVideos()));
        vo.setDeletedVideos(safeLong(statsMapper.countDeletedVideos()));
        Long totalSize = safeLong(statsMapper.sumVideoFileSize());
        vo.setTotalSizeBytes(totalSize);
        vo.setTotalSizeGB(formatGB(totalSize));

        File baseDir = new File(storageBasePath);
        vo.setStorageDirExists(baseDir.exists() && baseDir.isDirectory());
        if (vo.getStorageDirExists()) {
            long free = baseDir.getFreeSpace();
            vo.setFreeSpaceBytes(free);
            vo.setFreeSpaceGB(formatGB(free));
        } else {
            vo.setFreeSpaceBytes(0L);
            vo.setFreeSpaceGB("0.00 GB");
        }

        return vo;
    }

    // -------- 私有工具 --------

    private Long safeLong(Long v) {
        return v == null ? 0L : v;
    }

    private String formatGB(Long bytes) {
        if (bytes == null) return "0.00 GB";
        BigDecimal gb = BigDecimal.valueOf(bytes).divide(
                BigDecimal.valueOf(1024L * 1024L * 1024L), 2, RoundingMode.HALF_UP);
        return gb + " GB";
    }
}
