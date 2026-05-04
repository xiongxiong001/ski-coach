// ski-admin-web/src/api/reports.js
import http from './http'

export function listReports(params) {
  return http.get('/admin/reports', { params })
}

export function getReportDetail(id) {
  return http.get(`/admin/reports/${id}`)
}

export function listComparisonReports(params) {
  return http.get('/admin/reports/comparisons', { params })
}

export function getComparisonReportDetail(id) {
  return http.get(`/admin/reports/comparisons/${id}`)
}