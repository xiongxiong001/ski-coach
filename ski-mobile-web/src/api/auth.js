import http from './http'

export function register(data) {
  return http.post('/api/auth/register', data)
}

export function login(data) {
  return http.post('/api/auth/login', data)
}

export function sendSmsCode(phone) {
  return http.post('/api/auth/send-sms-code', { phone })
}

export function smsLogin(data) {
  return http.post('/api/auth/sms-login', data)
}

export function logout() {
  return http.post('/api/auth/logout')
}
