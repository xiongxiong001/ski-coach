-- 用户反馈表
CREATE TABLE IF NOT EXISTS `feedbacks` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT       NOT NULL                COMMENT '用户ID',
    `type`          VARCHAR(32)  NOT NULL                COMMENT '反馈类型: bug=BUG反馈, feature=功能建议, performance=测速反馈, other=其他',
    `content`       TEXT         NOT NULL                COMMENT '反馈内容',
    `contact`       VARCHAR(64)           DEFAULT NULL   COMMENT '联系方式(手机号/邮箱)',
    `images`        VARCHAR(1024)         DEFAULT NULL   COMMENT '逗号分隔的图片相对路径',
    `app_version`   VARCHAR(32)           DEFAULT NULL   COMMENT 'APP版本号',
    `status`        TINYINT      NOT NULL DEFAULT 0      COMMENT '状态: 0=未处理, 1=已查看, 2=已回复',
    `reply`         TEXT                  DEFAULT NULL   COMMENT '官方回复内容',
    `created_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_type` (`type`),
    INDEX `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈';
