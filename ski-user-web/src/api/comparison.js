import http from './http'

/** 创建对比报告(异步) */
export function createComparison(data) {
  return http.post('/api/comparisons', data)
}

/** 我的对比报告列表 */
export function listComparisons(params) {
  return http.get('/api/comparisons', { params })
}

/** 对比报告详情 */
export function getComparison(id) {
  return http.get(`/api/comparisons/${id}`)
}
