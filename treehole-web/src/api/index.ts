import axios from 'axios'
import { getToken, logout } from './auth'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

service.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Alibaba-Java: 401 处理 — Token 过期时跳转登录
service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      // 401 未授权，清除本地状态并跳转登录
      if (res.code === 401) {
        logout()
        window.location.hash = '/login'
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    // 网络层 401 处理
    if (error.response?.status === 401) {
      logout()
      window.location.hash = '/login'
    }
    return Promise.reject(error)
  }
)

export default service
