import http from './http'

export function submitFeedback(formData) {
  return http.post('/api/feedbacks', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

export function getFeedbackStats() {
  return http.get('/api/feedbacks/stats')
}

export function listFeedbacks(params) {
  return http.get('/api/feedbacks', { params })
}

export function getFeedback(id) {
  return http.get(`/api/feedbacks/${id}`)
}
