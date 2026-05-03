import http from './http'

/** 我的报告列表 */
export function listReports(params) {
  return http.get('/api/reports', { params })
}

/** 报告详情 */
export function getReport(id) {
  return http.get(`/api/reports/${id}`)
}
