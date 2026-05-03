/** 任务状态 */
export const TASK_STATUS = {
  pending: { text: '排队中', type: 'info'    },
  running: { text: '执行中', type: 'warning' },
  success: { text: '成功',   type: 'success' },
  failed:  { text: '失败',   type: 'danger'  }
}

/** 任务类型 */
export const TASK_TYPE = {
  single:     { text: '单次分析', type: 'primary' },
  comparison: { text: '对比分析', type: 'success' }
}

/** 用户状态 */
export const USER_STATUS = {
  1: { text: '正常', type: 'success' },
  0: { text: '封禁', type: 'danger' }
}

export const TOKEN_KEY = 'ski_coach_admin_token'
export const ADMIN_INFO_KEY = 'ski_coach_admin_info'
