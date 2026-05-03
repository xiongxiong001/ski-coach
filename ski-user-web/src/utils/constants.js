/** 视频分析状态显示 */
export const VIDEO_STATUS = {
  pending:   { text: '等待分析', type: 'info'    },
  analyzing: { text: '分析中',  type: 'warning' },
  analyzed:  { text: '已完成',  type: 'success' },
  failed:    { text: '分析失败', type: 'danger'  }
}

/** 任务状态显示 */
export const TASK_STATUS = {
  pending: { text: '排队中', type: 'info'    },
  running: { text: '执行中', type: 'warning' },
  success: { text: '成功',  type: 'success' },
  failed:  { text: '失败',  type: 'danger'  }
}

/** 文件大小限制 */
export const MAX_FILE_SIZE_MB = 100
export const ALLOWED_VIDEO_EXTENSIONS = ['mp4', 'mov', 'm4v']

/** 任务轮询间隔(毫秒) */
export const TASK_POLL_INTERVAL = 3000

/** Token 在 localStorage 的 key */
export const TOKEN_KEY = 'ski_coach_user_token'
export const USER_INFO_KEY = 'ski_coach_user_info'
