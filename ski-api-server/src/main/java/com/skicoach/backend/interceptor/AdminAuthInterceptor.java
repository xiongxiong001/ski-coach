package com.skicoach.backend.interceptor;

import com.skicoach.backend.common.constant.RedisKeyConstant;
import com.skicoach.backend.common.exception.AuthException;
import com.skicoach.backend.common.result.ResultCode;
import com.skicoach.backend.common.util.JwtUtil;
import com.skicoach.backend.common.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理端 JWT 拦截器
 *
 * 与用户端拦截器分离,使用独立的 JWT 密钥(admin-secret)。
 * 即使用户端 Token 被泄露,也无法访问管理后台。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    public static final String AUTH_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new AuthException(ResultCode.UNAUTHORIZED, "缺少 Authorization 头");
        }
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new AuthException(ResultCode.UNAUTHORIZED, "Token 为空");
        }

        // 黑名单检查(管理员独立的key)
        String blacklistKey = RedisKeyConstant.ADMIN_TOKEN_BLACKLIST + token;
        Boolean isBlacklisted = stringRedisTemplate.hasKey(blacklistKey);
        if (Boolean.TRUE.equals(isBlacklisted)) {
            throw new AuthException(ResultCode.TOKEN_INVALID, "Token已失效,请重新登录");
        }

        Long adminId = jwtUtil.parseAdminToken(token);
        SecurityUtil.setAdminId(adminId);
        log.debug("管理员JWT校验通过: adminId={}, uri={}", adminId, request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        SecurityUtil.clear();
    }
}
