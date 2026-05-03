import http from './http'

export function listTasks(params) {
  return http.get('/admin/tasks', { params })
}

export function getTaskDetail(id) {
  return http.get(`/admin/tasks/${id}`)
}

export function retryTask(id) {
  return http.post(`/admin/tasks/${id}/retry`)
}
