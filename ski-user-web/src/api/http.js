import axios from 'axios'
import { ElMessage } from 'element-plus'
import { TOKEN_KEY } from '@/utils/constants'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 30000  // 默认30秒,上传/AI接口在调用时单独传更长超时
})

// ============ 请求拦截器 ============
http.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ============ 响应拦截器 ============
http.interceptors.response.use(
  (response) => {
    const data = response.data
    // 后端统一格式: { code, message, data, timestamp }
    if (data && typeof data.code !== 'undefined') {
      if (data.code === 0) {
        return data.data   // 成功:直接返回 data 字段
      }
      // 业务错误
      if (data.code === 4010 || data.code === 4011 || data.code === 4012) {
        // 鉴权失败,清除token并跳到登录页
        localStorage.removeItem(TOKEN_KEY)
        // 防止在登录页重复跳转
        if (!window.location.pathname.startsWith('/login')) {
          ElMessage.error(data.message || '登录已过期')
          setTimeout(() => {
            window.location.href = '/login'
          }, 800)
        }
        return Promise.reject(new Error(data.message || '未登录'))
      }
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return data
  },
  (error) => {
    // 网络错误 / HTTP 状态错误
    let message = '网络异常,请稍后重试'
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        message = '未登录或登录已过期'
        localStorage.removeItem(TOKEN_KEY)
        if (!window.location.pathname.startsWith('/login')) {
          window.location.href = '/login'
        }
      } else if (status === 404) {
        message = '请求的资源不存在'
      } else if (status === 413) {
        message = '上传文件过大'
      } else if (status >= 500) {
        message = '服务器异常,请稍后重试'
      } else {
        message = error.response.data?.message || `HTTP ${status}`
      }
    } else if (error.code === 'ECONNABORTED') {
      message = '请求超时'
    }
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default http
