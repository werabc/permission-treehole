import request from './index'

export interface LoginForm {
  username: string
  password: string
}

export interface TokenVO {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

export function register(data: LoginForm) {
  return request.post('/th/auth/register', data) as Promise<{ data: { id: string } }>
}

export function login(data: LoginForm) {
  return request.post('/th/auth/login', data) as Promise<{ data: { token: string; nickname: string } }>
}

export function getUserInfo() {
  return request.get('/th/auth/user-info') as Promise<{ data: any }>
}

// Alibaba-Java: 安全规约 — Token 过期检查
export function isLoggedIn() {
  const token = localStorage.getItem('th_token')
  if (!token) return false
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    const exp = payload.exp * 1000
    return Date.now() < exp
  } catch {
    return false
  }
}

export function getToken() {
  return localStorage.getItem('th_token')
}

// Alibaba-Java: 安全规约 — 登出时调用后端接口并清除本地状态
export function logout() {
  const token = localStorage.getItem('th_token')
  if (token) {
    // 调用后端登出接口（异步，不阻塞）
    request.post('/th/auth/logout', {}, {
      headers: { Authorization: `Bearer ${token}` }
    }).catch(() => { /* ignore */ })
  }
  localStorage.removeItem('th_token')
  localStorage.removeItem('th_nickname')
}

// 解析 Token 获取用户ID
export function getUserIdFromToken(): number | null {
  const token = localStorage.getItem('th_token')
  if (!token) return null
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return Number(payload.sub)
  } catch {
    return null
  }
}
