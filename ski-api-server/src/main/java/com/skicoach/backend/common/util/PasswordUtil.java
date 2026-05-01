package com.skicoach.backend.common.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码加密工具(BCrypt)
 *
 * 单例 BCryptPasswordEncoder,业务代码直接静态调用。
 */
public final class PasswordUtil {

    private PasswordUtil() {}

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    /** 加密(用于注册/改密码) */
    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    /** 校验(用于登录) */
    public static boolean matches(String rawPassword, String hashedPassword) {
        return ENCODER.matches(rawPassword, hashedPassword);
    }
}
