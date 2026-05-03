import http from './http'

export function getOverview() {
  return http.get('/admin/stats/overview')
}

export function getDailyStats(params) {
  return http.get('/admin/stats/daily', { params })
}

export function getLlmCostStats() {
  return http.get('/admin/stats/llm-cost')
}

export function getStorageStats() {
  return http.get('/admin/stats/storage')
}
