import http from './http'

export function listReports(params) {
  return http.get('/admin/reports', { params })
}

export function getReportDetail(id) {
  return http.get(`/admin/reports/${id}`)
}

export function listComparisons(params) {
  return http.get('/admin/comparisons', { params })
}

export function getComparisonDetail(id) {
  return http.get(`/admin/comparisons/${id}`)
}