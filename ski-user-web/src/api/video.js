import http from './http'

/**
 * 上传视频
 * @param {File} file
 * @param {(progress: number) => void} onProgress 上传进度回调(0-100)
 */
export function uploadVideo(file, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/api/videos/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 600000,   // 10分钟,大文件上传需要更长超时
    onUploadProgress: (e) => {
      if (e.total && onProgress) {
        onProgress(Math.round((e.loaded * 100) / e.total))
      }
    }
  })
}

/** 视频列表 */
export function listVideos(params) {
  return http.get('/api/videos', { params })
}

/** 视频详情 */
export function getVideo(id) {
  return http.get(`/api/videos/${id}`)
}

/** 删除视频 */
export function deleteVideo(id) {
  return http.delete(`/api/videos/${id}`)
}
