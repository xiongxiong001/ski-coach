import http from './http'

/** 获取个人资料 */
export function getProfile() {
  return http.get('/api/user/profile')
}

/** 修改昵称 */
export function updateProfile(data) {
  return http.put('/api/user/profile', data)
}
