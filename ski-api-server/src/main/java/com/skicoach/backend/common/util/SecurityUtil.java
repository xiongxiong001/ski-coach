package com.skicoach.backend.common.util;

import com.skicoach.backend.common.exception.AuthException;
import com.skicoach.backend.common.result.ResultCode;

/**
 * 当前登录用户上下文工具
 *
 * 通过 ThreadLocal 在请求处理过程中传递当前用户信息。
 * 由 JwtInterceptor 设置,Controller/Service 通过此工具获取。
 */
public final class SecurityUtil {

    private SecurityUtil() {}

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> CURRENT_ADMIN_ID = new ThreadLocal<>();

    // ----------- 用户 -----------
    public static void setUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static Long getUserId() {
        return CURRENT_USER_ID.get();
    }

    /** 获取当前用户ID,如果未登录抛异常 */
    public static Long getUserIdOrThrow() {
        Long userId = CURRENT_USER_ID.get();
        if (userId == null) {
            throw new AuthException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }

    // ----------- 管理员 -----------
    public static void setAdminId(Long adminId) {
        CURRENT_ADMIN_ID.set(adminId);
    }

    public static Long getAdminId() {
        return CURRENT_ADMIN_ID.get();
    }

    public static Long getAdminIdOrThrow() {
        Long adminId = CURRENT_ADMIN_ID.get();
        if (adminId == null) {
            throw new AuthException(ResultCode.UNAUTHORIZED);
        }
        return adminId;
    }

    /** 清除上下文(每次请求结束时由拦截器调用) */
    public static void clear() {
        CURRENT_USER_ID.remove();
        CURRENT_ADMIN_ID.remove();
    }
}
