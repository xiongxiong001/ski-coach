package com.skicoach.backend.common.constant;

/**
 * Redis Key 命名常量
 *
 * 命名规范:
 *   ski_coach:{业务模块}:{key类型}:{业务ID}
 */
public final class RedisKeyConstant {

    private RedisKeyConstant() {}

    // 应用前缀
    public static final String PREFIX = "ski_coach:";

    // 用户Token黑名单(用户登出后,JWT放进黑名单到过期时间)
    public static final String USER_TOKEN_BLACKLIST = PREFIX + "user:token_blacklist:";

    // 管理员Token黑名单
    public static final String ADMIN_TOKEN_BLACKLIST = PREFIX + "admin:token_blacklist:";

    // 任务队列(P2.4 用)
    public static final String TASK_QUEUE = PREFIX + "task:queue";

    // 任务正在运行的标记(防止同一任务重复消费)
    public static final String TASK_RUNNING = PREFIX + "task:running:";

    // 用户上传频率限制(可选,后续做)
    public static final String UPLOAD_RATE_LIMIT = PREFIX + "user:upload_limit:";
}
