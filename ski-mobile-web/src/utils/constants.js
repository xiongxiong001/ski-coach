export const VIDEO_STATUS = {
  pending:   { text: '等待中',   type: 'default', color: '#6B7280' },
  analyzing: { text: '分析中',   type: 'warning', color: '#F59E0B' },
  analyzed:  { text: '已完成',   type: 'success', color: '#10B981' },
  failed:    { text: '失败',     type: 'danger',  color: '#EF4444' }
}

export const TASK_STATUS = {
  pending: { text: '排队中', type: 'default' },
  running: { text: '执行中', type: 'warning' },
  success: { text: '成功',  type: 'success' },
  failed:  { text: '失败',  type: 'danger'  }
}

export const MAX_FILE_SIZE_MB = 100
export const ALLOWED_VIDEO_EXTENSIONS = ['mp4', 'mov', 'm4v']
export const TASK_POLL_INTERVAL = 3000

export const TOKEN_KEY = 'ski_coach_user_token'
export const USER_INFO_KEY = 'ski_coach_user_info'
