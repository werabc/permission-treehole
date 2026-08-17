import request from './index'

export interface LoginForm {
  username: string
  password: string
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

export function isLoggedIn() {
  return !!localStorage.getItem('th_token')
}

export function getToken() {
  return localStorage.getItem('th_token')
}

export function logout() {
  localStorage.removeItem('th_token')
  localStorage.removeItem('th_nickname')
}
