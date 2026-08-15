import request from './index'

export interface OnlineUser {
  userId: string
  username: string
  nickname: string
  ip: string
  loginTime: string
}

export function getOnlineUsers(): Promise<{ data: OnlineUser[] }> {
  return request.get('/online')
}

export function getOnlineCount(): Promise<{ data: number }> {
  return request.get('/online/count')
}

export function forceLogout(userId: number): Promise<{ data: null }> {
  return request.delete(`/online/${userId}`)
}
