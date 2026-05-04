-- 短信发送记录表
CREATE TABLE IF NOT EXISTS `sms_send_record` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `phone`         VARCHAR(11)  NOT NULL                COMMENT '手机号',
    `code`          VARCHAR(6)   NOT NULL                COMMENT '验证码',
    `sms_type`      VARCHAR(32)  NOT NULL DEFAULT 'register_login' COMMENT '短信类型: register_login=注册登录, forgot_password=忘记密码',
    `provider`      VARCHAR(32)  NOT NULL DEFAULT 'aliyun' COMMENT '服务商',
    `send_status`   TINYINT      NOT NULL DEFAULT 0      COMMENT '发送状态: 0=未发送(开发环境) 1=发送成功 2=发送失败',
    `error_message` VARCHAR(256)          DEFAULT NULL   COMMENT '失败原因',
    `ip_address`    VARCHAR(45)           DEFAULT NULL   COMMENT '请求来源IP',
    `created_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_phone` (`phone`),
    INDEX `idx_sms_type` (`sms_type`),
    INDEX `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信发送记录';
