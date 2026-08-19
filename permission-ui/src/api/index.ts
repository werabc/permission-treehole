import axios from 'axios'
import type { ApiResult } from '@/types'
import { ElMessage } from 'element-plus'
import { refreshToken as refreshTokenApi } from '@/api/auth'

const service = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

let isRefreshing = false
let refreshSubscribers: Array<(token: string) => void> = []
// 记录已尝试刷新过的请求 URL+方法，防止 403 无权限时无限刷新 token
const refreshedUrls = new Set<string>()

function onTokenRefreshed(token: string) {
  refreshSubscribers.forEach(cb => cb(token))
  refreshSubscribers = []
}

function subscribeTokenRefresh(cb: (token: string) => void) {
  refreshSubscribers.push(cb)
}

// Alibaba-Java: 401 处理 — 尝试刷新 Token，刷新成功后重试原请求
service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      if (res.code === 401) {
        // 业务层 401，尝试刷新 Token
        const refreshTokenVal = localStorage.getItem('refreshToken')
        if (refreshTokenVal) {
          return handleTokenRefresh(response.config)
        }
        localStorage.clear()
        window.location.hash = '/login'
      } else {
        ElMessage.error(res.message || '请求失败')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    const status = error.response?.status
    if (status === 401 || status === 403) {
      // 403 无权限：普通用户访问了 admin 接口，直接提示并跳转
      if (status === 403 && error.config?.url) {
        const urlKey = `${error.config.method}_${error.config.url}`
        if (refreshedUrls.has(urlKey)) {
          // 已尝试过刷新仍 403，说明是权限不足而非 token 过期
          refreshedUrls.delete(urlKey)
          ElMessage.warning('权限不足，即将跳转登录')
          localStorage.clear()
          setTimeout(() => { window.location.hash = '/login' }, 1500)
          return Promise.reject(error)
        }
        refreshedUrls.add(urlKey)
      }
      // 401 未认证 或 403 首次：尝试刷新 Token
      const refreshTokenVal = localStorage.getItem('refreshToken')
      if (refreshTokenVal) {
        return handleTokenRefresh(error.config)
      }
      localStorage.clear()
      window.location.hash = '/login'
    } else {
      ElMessage.error(error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

// 处理 Token 刷新逻辑
function handleTokenRefresh(originalConfig: any): Promise<any> {
  if (!isRefreshing) {
    isRefreshing = true
    const refreshTokenVal = localStorage.getItem('refreshToken')!
    return refreshTokenApi(refreshTokenVal)
      .then((refreshRes) => {
        const newToken = refreshRes.data.accessToken
        const newRefreshToken = refreshRes.data.refreshToken
        localStorage.setItem('accessToken', newToken)
        localStorage.setItem('refreshToken', newRefreshToken)
        // 刷新成功，清除 403 重试记录，允许后续请求重新尝试
        refreshedUrls.clear()
        // 通知所有等待的请求，并清空订阅列表
        onTokenRefreshed(newToken)
        // 重试原请求
        originalConfig.headers.Authorization = `Bearer ${newToken}`
        return service(originalConfig)
      })
      .catch(() => {
        localStorage.clear()
        window.location.hash = '/login'
        return Promise.reject(new Error('Token 刷新失败'))
      })
      .finally(() => {
        isRefreshing = false
      })
  }
  // 正在刷新中，将请求加入队列
  return new Promise((resolve) => {
    subscribeTokenRefresh((token: string) => {
      originalConfig.headers.Authorization = `Bearer ${token}`
      resolve(service(originalConfig))
    })
  })
}

export default service
export type { ApiResult }
