import http from './http'

/** 注册 */
export function register(data) {
  return http.post('/api/auth/register', data)
}

/** 登录 */
export function login(data) {
  return http.post('/api/auth/login', data)
}

/** 登出 */
export function logout() {
  return http.post('/api/auth/logout')
}
