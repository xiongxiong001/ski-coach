package com.skicoach.backend.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.skicoach.backend.common.constant.RedisKeyConstant;
import com.skicoach.backend.common.exception.BusinessException;
import com.skicoach.backend.common.result.ResultCode;
import com.skicoach.backend.service.SmsService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 阿里云号码认证服务 - 短信验证码
 */
@Slf4j
@Service
public class AliyunSmsServiceImpl implements SmsService {

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${ski.sms.access-key-id:}")
    private String accessKeyId;

    @Value("${ski.sms.access-key-secret:}")
    private String accessKeySecret;

    @Value("${ski.sms.sign-name:}")
    private String signName;

    @Value("${ski.sms.template-code:}")
    private String templateCode;

    private IAcsClient client;

    private static final int CODE_LENGTH = 6;
    private static final int CODE_TTL_MINUTES = 5;
    private static final int SEND_INTERVAL_SECONDS = 60;
    private static final int DAILY_LIMIT = 10;
    private static final int MAX_RETRY_COUNT = 5;

    public AliyunSmsServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @PostConstruct
    public void init() {
        if (accessKeyId.isEmpty() || accessKeySecret.isEmpty()) {
            log.warn("阿里云号码认证 AccessKey 未配置,短信服务将不可用");
            return;
        }
        DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKeyId, accessKeySecret);
        this.client = new DefaultAcsClient(profile);
    }

    @Override
    public void sendCode(String phone) {
        if (client == null) {
            throw new BusinessException(ResultCode.SMS_SEND_FAILED, "短信服务未配置");
        }

        // 1. 检查60秒发送间隔
        String limitKey = RedisKeyConstant.SMS_SEND_LIMIT + phone;
        Boolean hasLimit = stringRedisTemplate.hasKey(limitKey);
        if (Boolean.TRUE.equals(hasLimit)) {
            throw new BusinessException(ResultCode.SMS_SEND_TOO_FREQUENT);
        }

        // 2. 检查每日上限
        String dailyKey = RedisKeyConstant.SMS_DAILY_LIMIT + phone;
        String dailyCount = stringRedisTemplate.opsForValue().get(dailyKey);
        if (dailyCount != null && Integer.parseInt(dailyCount) >= DAILY_LIMIT) {
            throw new BusinessException(ResultCode.SMS_DAILY_LIMIT_EXCEEDED);
        }

        // 3. 生成6位随机验证码
        String code = String.format("%0" + CODE_LENGTH + "d",
                ThreadLocalRandom.current().nextInt(0, 1_000_000));

        // 4. 调用阿里云发送验证码
        doSendVerifyCode(phone, code);

        // 5. 存储验证码到Redis,TTL=5分钟
        String codeKey = RedisKeyConstant.SMS_CODE + phone;
        stringRedisTemplate.opsForValue().set(codeKey, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);

        // 6. 发送间隔限制,60秒
        stringRedisTemplate.opsForValue().set(limitKey, "1", SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);

        // 7. 每日计数+1,TTL=24小时
        stringRedisTemplate.opsForValue().increment(dailyKey);
        stringRedisTemplate.expire(dailyKey, 24, TimeUnit.HOURS);

        log.info("短信验证码发送成功: phone={}", maskPhone(phone));
    }

    @Override
    public void verifyCode(String phone, String code) {
        String codeKey = RedisKeyConstant.SMS_CODE + phone;
        String retryKey = codeKey + ":retry";

        // 1. 检查验证码是否存在
        String storedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (storedCode == null) {
            throw new BusinessException(ResultCode.SMS_CODE_EXPIRED);
        }

        // 2. 校验
        if (!storedCode.equals(code)) {
            Long retryCount = stringRedisTemplate.opsForValue().increment(retryKey);
            stringRedisTemplate.expire(retryKey, CODE_TTL_MINUTES, TimeUnit.MINUTES);
            if (retryCount != null && retryCount >= MAX_RETRY_COUNT) {
                stringRedisTemplate.delete(codeKey);
                stringRedisTemplate.delete(retryKey);
                throw new BusinessException(ResultCode.SMS_CODE_ERROR, "验证码错误次数过多,请重新获取");
            }
            throw new BusinessException(ResultCode.SMS_CODE_ERROR);
        }

        // 3. 验证成功,删除验证码
        stringRedisTemplate.delete(codeKey);
        stringRedisTemplate.delete(retryKey);
    }

    private void doSendVerifyCode(String phone, String code) {
        CommonRequest request = new CommonRequest();
        request.setSysMethod(MethodType.POST);
        request.setSysDomain("dypnsapi.aliyuncs.com");
        request.setSysVersion("2017-05-25");
        request.setSysAction("SendSmsVerifyCode");
        request.putQueryParameter("PhoneNumber", phone);
        request.putQueryParameter("SignName", signName);
        request.putQueryParameter("TemplateCode", templateCode);
        request.putQueryParameter("TemplateParam",
                String.format("{\"code\":\"%s\",\"min\":\"5\"}", code));

        try {
            CommonResponse response = client.getCommonResponse(request);
            JSONObject json = JSONUtil.parseObj(response.getData());
            if (!"OK".equals(json.getStr("Code"))) {
                throw new BusinessException(ResultCode.SMS_SEND_FAILED,
                        json.getStr("Message"));
            }
        } catch (ClientException e) {
            log.error("短信发送失败: phone={}, error={}", maskPhone(phone), e.getMessage());
            throw new BusinessException(ResultCode.SMS_SEND_FAILED, "短信发送失败");
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
