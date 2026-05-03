import http from './http'

export function getTaskStatus(id) {
  return http.get(`/api/tasks/${id}`)
}
