import request from './index'
import type { LoginForm, TokenVO, UserInfo, ApiResult } from '@/types'

export function getCaptcha(): Promise<ApiResult<{ captchaKey: string; captchaImage: string }>> {
  return request.get('/auth/captcha')
}

export function login(data: LoginForm): Promise<ApiResult<TokenVO>> {
  return request.post('/auth/login', data)
}

export function refreshToken(refreshToken: string): Promise<ApiResult<TokenVO>> {
  return request.post('/auth/refresh', { refreshToken })
}

export function getUserInfo(): Promise<ApiResult<UserInfo>> {
  return request.get('/auth/user-info')
}

export function logout(token: string): Promise<ApiResult<null>> {
  return request.post('/auth/logout', {}, {
    headers: { Authorization: `Bearer ${token}` }
  })
}
