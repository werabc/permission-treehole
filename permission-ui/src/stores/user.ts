import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, getUserInfo, logout as logoutApi, refreshToken as refreshApi } from '@/api/auth'
import type { UserInfo, LoginForm } from '@/types'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('accessToken') || '')
  const refreshToken = ref<string>(localStorage.getItem('refreshToken') || '')
  const userInfo = ref<UserInfo | null>(null)
  const permissions = ref<string[]>([])
  const roles = ref<string[]>([])

  function setToken(accessToken: string, refreshTokenVal: string) {
    token.value = accessToken
    refreshToken.value = refreshTokenVal
    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('refreshToken', refreshTokenVal)
  }

  function clearToken() {
    token.value = ''
    refreshToken.value = ''
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
  }

  async function loginAction(loginForm: LoginForm) {
    const res = await loginApi(loginForm)
    setToken(res.data.accessToken, res.data.refreshToken)
    await fetchUserInfo()
    return res
  }

  async function fetchUserInfo() {
    const res = await getUserInfo()
    userInfo.value = res.data
    permissions.value = res.data.permissions || []
    roles.value = res.data.roles || []
  }

  async function refreshAction() {
    if (!refreshToken.value) {
      clearToken()
      router.push('/login')
      return false
    }
    try {
      const res = await refreshApi(refreshToken.value)
      setToken(res.data.accessToken, res.data.refreshToken)
      return true
    } catch {
      clearToken()
      router.push('/login')
      return false
    }
  }

  async function logoutAction() {
    if (token.value) {
      try {
        await logoutApi(token.value)
      } catch { /* ignore */ }
    }
    clearToken()
    userInfo.value = null
    permissions.value = []
    roles.value = []
    router.push('/login')
  }

  function hasPermission(permission: string): boolean {
    if (roles.value.includes('admin')) return true
    return permissions.value.includes(permission)
  }

  return {
    token, refreshToken, userInfo, permissions, roles,
    loginAction, fetchUserInfo, refreshAction, logoutAction, setToken, clearToken, hasPermission,
  }
})
