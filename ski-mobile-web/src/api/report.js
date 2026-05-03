import http from './http'

export function listReports(params) {
  return http.get('/api/reports', { params })
}

export function getReport(id) {
  return http.get(`/api/reports/${id}`)
}
