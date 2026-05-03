import http from './http'

export function listUsers(params) {
  return http.get('/admin/users', { params })
}

export function getUserDetail(id) {
  return http.get(`/admin/users/${id}`)
}

export function updateUserStatus(id, status) {
  return http.put(`/admin/users/${id}/status`, { status })
}
