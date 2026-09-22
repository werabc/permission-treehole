<template>
  <div class="app">
    <Welcome />
    <header class="header">
      <div class="header-inner">
        <router-link to="/" class="logo">
          <span class="logo-icon">🌳</span>
          <span class="logo-text">树洞</span>
        </router-link>
        <nav class="nav">
          <router-link to="/">首页</router-link>
          <router-link to="/search" class="icon-link" title="搜索" aria-label="搜索">🔍</router-link>
          <!-- Alibaba-Java: 使用 computed 属性确保响应式 -->
          <template v-if="isLoggedIn">
            <router-link to="/publish">发布</router-link>
            <router-link to="/notifications" class="icon-link" title="消息中心" aria-label="消息中心">
              🔔<span v-if="unreadCount > 0" class="nav-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
            </router-link>
            <router-link to="/settings" class="icon-link" title="账号设置" aria-label="账号设置">⚙️</router-link>
            <router-link to="/profile" class="profile-link">👤 {{ displayName }}</router-link>
            <a @click="handleLogout" class="logout-btn">退出</a>
          </template>
          <template v-else>
            <router-link to="/login">登录</router-link>
            <router-link to="/register">注册</router-link>
          </template>
        </nav>
      </div>
    </header>

    <main class="main">
      <router-view />
    </main>

    <footer class="footer">
      <p>© 2026 树洞 - 匿名分享你的故事</p>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { logout as doLogout } from './api/auth'
import { getUnreadCount } from './api/treehole'
import Welcome from './views/Welcome.vue'

const router = useRouter()
const unreadCount = ref(0)
const nickname = ref(localStorage.getItem('th_nickname') || '')
const tokenRef = ref(localStorage.getItem('th_token') || '')

// 使用 ref 追踪登录状态，确保响应式更新
const isLoggedIn = computed(() => {
  const token = tokenRef.value
  if (!token) return false
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    const exp = payload.exp * 1000
    return Date.now() < exp
  } catch {
    return false
  }
})

const displayName = computed(() => nickname.value || '用户')

function handleLogout() {
  doLogout()
  tokenRef.value = ''
  nickname.value = ''
  unreadCount.value = 0
  router.push('/login')
}

async function loadUnreadCount() {
  if (!isLoggedIn.value) return
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data
  } catch (e) { /* ignore */ }
}

// 响应登录状态变化
function onStorageChange() {
  tokenRef.value = localStorage.getItem('th_token') || ''
  nickname.value = localStorage.getItem('th_nickname') || ''
  loadUnreadCount()
}

window.addEventListener('storage', onStorageChange)

onMounted(() => {
  loadUnreadCount()
})

// Alibaba-Java: 清理事件监听器，防止内存泄漏
onUnmounted(() => {
  window.removeEventListener('storage', onStorageChange)
})
</script>

<style scoped>
.app { min-height: 100vh; display: flex; flex-direction: column; }
.header { background: #fff; box-shadow: 0 1px 4px rgba(0,0,0,0.08); position: sticky; top: 0; z-index: 100; }
.header-inner { max-width: 800px; margin: 0 auto; padding: 0 20px; height: 60px; display: flex; align-items: center; justify-content: space-between; }
.logo { display: flex; align-items: center; gap: 8px; font-size: 20px; font-weight: 700; color: #1e293b; text-decoration: none; }
.logo:hover { text-decoration: none; }
.logo-icon { font-size: 28px; }
.nav { display: flex; gap: 20px; align-items: center; }
.nav a { color: #64748b; font-size: 15px; font-weight: 500; text-decoration: none; padding: 6px 12px; border-radius: 6px; transition: all 0.2s; }
.nav a:hover, .nav a.router-link-active { color: #3b82f6; background: #eff6ff; text-decoration: none; }
.profile-link { position: relative; }
.icon-link { position: relative; padding: 6px 8px !important; font-size: 16px; }
.nav-badge { position: absolute; top: -4px; right: -6px; background: #ef4444; color: #fff; font-size: 10px; padding: 1px 5px; border-radius: 10px; min-width: 16px; text-align: center; }
.logout-btn { color: #ef4444; cursor: pointer; margin-left: 8px; }
.main { flex: 1; padding: 24px 20px; }
.footer { text-align: center; padding: 24px; color: #94a3b8; font-size: 13px; }
</style>
