import http from './http'

export function createComparison(data) {
  return http.post('/api/comparisons', data)
}

export function listComparisons(params) {
  return http.get('/api/comparisons', { params })
}

export function getComparison(id) {
  return http.get(`/api/comparisons/${id}`)
}
