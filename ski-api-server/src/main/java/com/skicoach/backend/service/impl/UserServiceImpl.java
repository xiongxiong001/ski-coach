package com.skicoach.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.skicoach.backend.common.constant.RedisKeyConstant;
import com.skicoach.backend.common.enums.UserStatusEnum;
import com.skicoach.backend.common.exception.BusinessException;
import com.skicoach.backend.common.result.ResultCode;
import com.skicoach.backend.common.util.JwtUtil;
import com.skicoach.backend.common.util.PasswordUtil;
import com.skicoach.backend.dto.auth.LoginRequest;
import com.skicoach.backend.dto.auth.LoginResponse;
import com.skicoach.backend.dto.auth.RegisterRequest;
import com.skicoach.backend.dto.auth.SmsLoginRequest;
import com.skicoach.backend.dto.user.UpdateProfileRequest;
import com.skicoach.backend.dto.user.UserProfileVO;
import com.skicoach.backend.entity.User;
import com.skicoach.backend.mapper.UserMapper;
import com.skicoach.backend.service.SmsService;
import com.skicoach.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final SmsService smsService;

    @Value("${ski.jwt.expire-hours:168}")
    private long expireHours;

    @Override
    public LoginResponse register(RegisterRequest request) {
        // 1. 校验手机号未被注册
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone())
        );
        if (existing != null) {
            throw new BusinessException(ResultCode.USER_PHONE_EXISTS);
        }

        // 2. 创建用户
        User user = new User();
        user.setPhone(request.getPhone());
        user.setPasswordHash(PasswordUtil.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null && !request.getNickname().isBlank()
                ? request.getNickname()
                : "雪友" + request.getPhone().substring(7));   // 默认昵称: "雪友"+手机后4位
        user.setStatus(UserStatusEnum.NORMAL.getValue());

        userMapper.insert(user);
        log.info("用户注册成功: userId={}, phone={}", user.getId(), maskPhone(user.getPhone()));

        // 3. 生成Token
        return buildLoginResponse(user);
    }

    @Override
    public void sendSmsCode(String phone) {
        smsService.sendCode(phone, "register_login");
    }

    @Override
    public LoginResponse smsLogin(SmsLoginRequest request) {
        // 1. 校验验证码
        smsService.verifyCode(request.getPhone(), request.getCode());

        // 2. 查找用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone())
        );

        if (user == null) {
            // 3. 新用户自动注册(无密码)
            user = new User();
            user.setPhone(request.getPhone());
            user.setNickname("雪友" + request.getPhone().substring(7));
            user.setPasswordHash("");   // 短信登录注册的用户无密码
            user.setStatus(UserStatusEnum.NORMAL.getValue());
            userMapper.insert(user);
            log.info("短信验证码自动注册: userId={}, phone={}", user.getId(), maskPhone(user.getPhone()));
        } else {
            // 4. 已有用户校验状态
            if (!UserStatusEnum.NORMAL.getValue().equals(user.getStatus())) {
                throw new BusinessException(ResultCode.USER_DISABLED);
            }
            log.info("短信验证码登录: userId={}, phone={}", user.getId(), maskPhone(user.getPhone()));
        }

        return buildLoginResponse(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. 查找用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone())
        );
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 2. 校验密码
        if (!PasswordUtil.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_WRONG);
        }

        // 3. 校验状态
        if (!UserStatusEnum.NORMAL.getValue().equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        log.info("用户登录成功: userId={}, phone={}", user.getId(), maskPhone(user.getPhone()));
        return buildLoginResponse(user);
    }

    @Override
    public void logout(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        // 把Token放进Redis黑名单,TTL=Token剩余有效期
        long remainingMillis = jwtUtil.getRemainingMillis(token, JwtUtil.TokenType.USER);
        if (remainingMillis <= 0) {
            return;  // 已过期,无需加黑名单
        }
        String key = RedisKeyConstant.USER_TOKEN_BLACKLIST + token;
        stringRedisTemplate.opsForValue().set(key, "1", remainingMillis, TimeUnit.MILLISECONDS);
        log.info("用户登出,Token加入黑名单: TTL={}ms", remainingMillis);
    }

    @Override
    public UserProfileVO getProfile(Long userId) {
        User user = getActiveUserOrThrow(userId);
        UserProfileVO vo = new UserProfileVO();
        BeanUtils.copyProperties(user, vo);
        vo.setPhone(maskPhone(user.getPhone()));   // 脱敏
        return vo;
    }

    @Override
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getActiveUserOrThrow(userId);
        user.setNickname(request.getNickname());
        userMapper.updateById(user);
        log.info("用户修改资料: userId={}, nickname={}", userId, request.getNickname());
    }

    @Override
    public User getActiveUserOrThrow(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (!UserStatusEnum.NORMAL.getValue().equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        return user;
    }

    // ----------- 私有工具方法 -----------

    private LoginResponse buildLoginResponse(User user) {
        String token = jwtUtil.generateUserToken(user.getId());
        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setExpireHours(expireHours);
        resp.setUserId(user.getId());
        resp.setNickname(user.getNickname());
        resp.setPhone(maskPhone(user.getPhone()));
        return resp;
    }

    /** 手机号脱敏: 138****8000 */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
