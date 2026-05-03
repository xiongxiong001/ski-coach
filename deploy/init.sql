-- ================================================================
-- Ski Coach 数据库初始化脚本
-- 数据库: MySQL 8.0
-- 字符集: utf8mb4
-- 时区: Asia/Shanghai
-- ================================================================

CREATE DATABASE IF NOT EXISTS ski_coach
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE ski_coach;

-- ================================================================
-- 用户表
-- ================================================================
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    phone           VARCHAR(20)  NOT NULL                COMMENT '手机号',
    password_hash   VARCHAR(255) NOT NULL                COMMENT 'BCrypt加密后的密码',
    nickname        VARCHAR(50)  DEFAULT NULL            COMMENT '昵称',
    status          TINYINT      NOT NULL DEFAULT 1      COMMENT '状态: 1=正常 0=封禁',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                          COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';


-- ================================================================
-- 视频表(包含分析数据,用于复用避免重复AI调用)
-- ================================================================
DROP TABLE IF EXISTS videos;
CREATE TABLE videos (
    id                      BIGINT        NOT NULL AUTO_INCREMENT COMMENT '视频ID',
    user_id                 BIGINT        NOT NULL                COMMENT '用户ID',

    -- 文件信息
    original_filename       VARCHAR(255)  NOT NULL                COMMENT '原始文件名',
    file_path               VARCHAR(500)  NOT NULL                COMMENT '存储路径(相对路径)',
    file_md5                VARCHAR(32)   NOT NULL                COMMENT '文件MD5(用于秒传)',
    file_size               BIGINT        NOT NULL                COMMENT '文件大小(字节)',

    -- 视频元信息
    duration_seconds        DECIMAL(10,2) DEFAULT NULL             COMMENT '视频时长(秒)',
    width                   INT           DEFAULT NULL             COMMENT '视频宽',
    height                  INT           DEFAULT NULL             COMMENT '视频高',
    fps                     DECIMAL(6,2)  DEFAULT NULL             COMMENT '帧率',

    -- 第1层分析结果(姿态识别 + 指标计算 + 动作分割)
    analysis_status         VARCHAR(20)   NOT NULL DEFAULT 'pending'
                                COMMENT '分析状态: pending/analyzing/analyzed/failed',
    analysis_data_json      JSON          DEFAULT NULL             COMMENT '完整的分析数据JSON',
    analysis_version        VARCHAR(10)   DEFAULT 'v1.0'           COMMENT '分析算法版本',
    detection_rate          DECIMAL(5,4)  DEFAULT NULL             COMMENT '姿态检测率',
    turn_left_count         INT           DEFAULT NULL             COMMENT '左转次数',
    turn_right_count        INT           DEFAULT NULL             COMMENT '右转次数',

    analysis_started_time   DATETIME      DEFAULT NULL             COMMENT '分析开始时间',
    analysis_finished_time  DATETIME      DEFAULT NULL             COMMENT '分析完成时间',
    analysis_error_message  TEXT          DEFAULT NULL             COMMENT '分析失败原因',

    -- 逻辑删除
    deleted_time            DATETIME      DEFAULT NULL             COMMENT '逻辑删除时间(NULL=未删除)',

    -- 时间字段
    created_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                          COMMENT '创建时间',
    update_time             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    KEY idx_user_status   (user_id, analysis_status),
    KEY idx_user_md5      (user_id, file_md5),
    KEY idx_user_created  (user_id, created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频表';


-- ================================================================
-- 分析任务表(单次分析 + 对比分析,统一管理)
-- ================================================================
DROP TABLE IF EXISTS analysis_tasks;
CREATE TABLE analysis_tasks (
    id                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    user_id             BIGINT        NOT NULL                COMMENT '用户ID',
    task_type           VARCHAR(20)   NOT NULL                COMMENT '任务类型: single=单次分析 / comparison=对比分析',

    -- 单次任务字段(对比任务时为NULL)
    video_id            BIGINT        DEFAULT NULL            COMMENT '视频ID(单次任务用)',

    -- 对比任务字段(单次任务时两个都为NULL)
    prev_video_id       BIGINT        DEFAULT NULL            COMMENT '上次视频ID(对比任务用)',
    curr_video_id       BIGINT        DEFAULT NULL            COMMENT '本次视频ID(对比任务用)',

    -- 状态
    status              VARCHAR(20)   NOT NULL DEFAULT 'pending'
                            COMMENT '任务状态: pending/running/success/failed',
    error_message       TEXT          DEFAULT NULL            COMMENT '失败原因',
    retry_count         INT           NOT NULL DEFAULT 0      COMMENT '重试次数',

    -- 调用成本统计
    llm_cost_yuan       DECIMAL(10,4) DEFAULT NULL            COMMENT 'LLM调用花费(元)',
    llm_input_tokens    INT           DEFAULT NULL            COMMENT 'LLM输入token数',
    llm_output_tokens   INT           DEFAULT NULL            COMMENT 'LLM输出token数',

    -- 关联的报告ID(成功后填充)
    report_id           BIGINT        DEFAULT NULL            COMMENT '关联的报告ID(单次报告或对比报告ID)',

    -- 时间字段
    start_time          DATETIME      DEFAULT NULL            COMMENT '任务开始执行时间',
    finish_time         DATETIME      DEFAULT NULL            COMMENT '任务结束时间',
    created_time        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                          COMMENT '创建时间',
    update_time         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    KEY idx_user_type_status (user_id, task_type, status),
    KEY idx_status_created   (status, created_time),
    KEY idx_video            (video_id),
    KEY idx_compare_videos   (prev_video_id, curr_video_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分析任务表';


-- ================================================================
-- 单次教练话术报告表
-- ================================================================
DROP TABLE IF EXISTS reports;
CREATE TABLE reports (
    id                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '报告ID',
    task_id             BIGINT        NOT NULL                COMMENT '任务ID',
    video_id            BIGINT        NOT NULL                COMMENT '视频ID',
    user_id             BIGINT        NOT NULL                COMMENT '用户ID',

    report_markdown     MEDIUMTEXT    NOT NULL                COMMENT '中文教练报告(Markdown)',

    llm_cost_yuan       DECIMAL(10,4) DEFAULT NULL            COMMENT 'LLM调用花费(元)',
    llm_input_tokens    INT           DEFAULT NULL            COMMENT 'LLM输入token数',
    llm_output_tokens   INT           DEFAULT NULL            COMMENT 'LLM输出token数',

    created_time        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                          COMMENT '创建时间',
    update_time         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_task (task_id),
    KEY idx_user_created  (user_id, created_time),
    KEY idx_video         (video_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单次教练报告表';


-- ================================================================
-- 对比教练话术报告表
-- ================================================================
DROP TABLE IF EXISTS comparison_reports;
CREATE TABLE comparison_reports (
    id                          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '对比报告ID',
    task_id                     BIGINT        NOT NULL                COMMENT '任务ID',
    user_id                     BIGINT        NOT NULL                COMMENT '用户ID',
    prev_video_id               BIGINT        NOT NULL                COMMENT '上次视频ID',
    curr_video_id               BIGINT        NOT NULL                COMMENT '本次视频ID',

    comparison_data_json        JSON          NOT NULL                COMMENT '差异计算数据JSON',
    report_markdown             MEDIUMTEXT    NOT NULL                COMMENT '对比教练报告(Markdown)',

    -- 关键统计快照(冗余存储,加速列表查询)
    improved_count              INT           NOT NULL DEFAULT 0       COMMENT '进步指标数',
    declined_count              INT           NOT NULL DEFAULT 0       COMMENT '退步指标数',
    stability_improved_count    INT           NOT NULL DEFAULT 0       COMMENT '稳定性提升数',

    llm_cost_yuan               DECIMAL(10,4) DEFAULT NULL             COMMENT 'LLM调用花费(元)',
    llm_input_tokens            INT           DEFAULT NULL             COMMENT 'LLM输入token数',
    llm_output_tokens           INT           DEFAULT NULL             COMMENT 'LLM输出token数',

    created_time                DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                          COMMENT '创建时间',
    update_time                 DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_task        (task_id),
    UNIQUE KEY uk_video_pair  (prev_video_id, curr_video_id),
    KEY idx_user_created      (user_id, created_time),
    KEY idx_videos            (prev_video_id, curr_video_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对比教练报告表';


-- ================================================================
-- 管理员表
-- ================================================================
DROP TABLE IF EXISTS admins;
CREATE TABLE admins (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
    username        VARCHAR(50)   NOT NULL                COMMENT '用户名',
    password_hash   VARCHAR(255)  NOT NULL                COMMENT 'BCrypt加密后的密码',
    real_name       VARCHAR(50)   DEFAULT NULL            COMMENT '真实姓名',
    status          TINYINT       NOT NULL DEFAULT 1      COMMENT '状态: 1=正常 0=禁用',
    last_login_time DATETIME      DEFAULT NULL            COMMENT '最后登录时间',
    created_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                          COMMENT '创建时间',
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';


-- ================================================================
-- 初始化默认管理员账号
-- 用户名: admin
-- 密码:   admin123  (BCrypt加密后存储)
-- 提示: 生产环境务必修改默认密码!
-- ================================================================
INSERT INTO admins (username, password_hash, real_name, status)
VALUES ('admin', '$2b$10$RfvwJJ5TP2yZOQ3Zdy6AXOCqUeCLvEcMAkENYj44.TQj/terhckQm', '默认管理员', 1);


-- ================================================================
-- 完成提示
-- ================================================================
SELECT 'Ski Coach 数据库初始化完成!' AS message;
SELECT
    table_name AS '表名',
    table_comment AS '说明'
FROM information_schema.tables
WHERE table_schema = 'ski_coach'
ORDER BY table_name;
