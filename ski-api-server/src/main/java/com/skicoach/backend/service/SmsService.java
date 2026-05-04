package com.skicoach.backend.service;

/**
 * 短信服务接口
 */
public interface SmsService {

    /** 发送验证码到指定手机号, smsType: register_login / forgot_password */
    void sendCode(String phone, String smsType);

    /** 校验验证码,成功则删除Redis中的验证码,失败抛BusinessException */
    void verifyCode(String phone, String code);
}
