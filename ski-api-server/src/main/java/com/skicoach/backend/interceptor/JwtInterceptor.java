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
 * 用户端 JWT 拦截器
 *
 * 工作流程:
 * 1. 从请求头 Authorization: Bearer xxx 取出Token
 * 2. 检查Token是否在黑名单(已登出)
 * 3. 解析Token,验证签名和过期时间
 * 4. 把userId放入ThreadLocal(SecurityUtil),供Controller/Service使用
 * 5. 请求结束后清除ThreadLocal(在afterCompletion中)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    public static final String AUTH_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 取出 Authorization 头
        String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new AuthException(ResultCode.UNAUTHORIZED, "缺少 Authorization 头");
        }
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new AuthException(ResultCode.UNAUTHORIZED, "Token 为空");
        }

        // 2. 检查黑名单(已登出的Token)
        String blacklistKey = RedisKeyConstant.USER_TOKEN_BLACKLIST + token;
        Boolean isBlacklisted = stringRedisTemplate.hasKey(blacklistKey);
        if (Boolean.TRUE.equals(isBlacklisted)) {
            throw new AuthException(ResultCode.TOKEN_INVALID, "Token已失效,请重新登录");
        }

        // 3. 解析Token
        Long userId = jwtUtil.parseUserToken(token);

        // 4. 设置上下文
        SecurityUtil.setUserId(userId);
        log.debug("JWT校验通过: userId={}, uri={}", userId, request.getRequestURI());

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        // 5. 清除ThreadLocal
        SecurityUtil.clear();
    }
}
