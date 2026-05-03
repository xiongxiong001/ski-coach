import http from './http'

export function getProfile() {
  return http.get('/api/user/profile')
}

export function updateProfile(data) {
  return http.put('/api/user/profile', data)
}
