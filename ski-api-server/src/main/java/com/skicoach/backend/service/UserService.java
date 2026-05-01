package com.skicoach.backend.service;

import com.skicoach.backend.dto.auth.LoginRequest;
import com.skicoach.backend.dto.auth.LoginResponse;
import com.skicoach.backend.dto.auth.RegisterRequest;
import com.skicoach.backend.dto.user.UpdateProfileRequest;
import com.skicoach.backend.dto.user.UserProfileVO;
import com.skicoach.backend.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {

    /** 注册新用户 */
    LoginResponse register(RegisterRequest request);

    /** 登录 */
    LoginResponse login(LoginRequest request);

    /** 登出(把Token放进Redis黑名单) */
    void logout(String token);

    /** 获取当前用户的个人资料 */
    UserProfileVO getProfile(Long userId);

    /** 修改昵称 */
    void updateProfile(Long userId, UpdateProfileRequest request);

    /** 校验用户存在且未被封禁 */
    User getActiveUserOrThrow(Long userId);
}
