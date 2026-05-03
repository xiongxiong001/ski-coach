import http from './http'

export function uploadVideo(file, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/api/videos/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 600000,
    onUploadProgress: (e) => {
      if (e.total && onProgress) {
        onProgress(Math.round((e.loaded * 100) / e.total))
      }
    }
  })
}

export function listVideos(params) {
  return http.get('/api/videos', { params })
}

export function getVideo(id) {
  return http.get(`/api/videos/${id}`)
}

export function deleteVideo(id) {
  return http.delete(`/api/videos/${id}`)
}
