package com.skicoach.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 短信发送记录
 */
@Data
@TableName("sms_send_record")
public class SmsSendRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 手机号 */
    private String phone;

    /** 验证码 */
    private String code;

    /** 短信类型: register_login=注册登录, forgot_password=忘记密码 */
    private String smsType;

    /** 服务商: aliyun */
    private String provider;

    /** 发送状态: 0=未发送(开发环境) 1=发送成功 2=发送失败 */
    private Integer sendStatus;

    /** 失败原因(可空) */
    private String errorMessage;

    /** 请求来源IP */
    private String ipAddress;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
