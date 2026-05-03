import http from './http'

/** 查询任务状态 */
export function getTaskStatus(id) {
  return http.get(`/api/tasks/${id}`)
}
