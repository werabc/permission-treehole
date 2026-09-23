<template>
  <div class="app">
    <IconSprite />
    <BackdropFx />

    <!-- App Shell（布局 3）：宽屏左侧常驻导航，窄屏自动收起 -->
    <div class="dh-shell">
      <AppSidenav v-if="!hideChrome" :unread-count="unreadCount" :is-logged-in="isLoggedIn" />

      <div class="dh-shell__main">
        <header v-if="!hideChrome" class="dh-hdr">
          <div class="dh-wrap">
            <div class="dh-hdr__inner">
              <router-link to="/" class="dh-brand">
                <AppIcon name="tree" :size="28" />
                <span>树洞</span>
                <em>deep hollow</em>
              </router-link>

              <nav class="dh-nav">
                <router-link to="/">广场</router-link>
                <router-link v-if="isLoggedIn" to="/publish">发布</router-link>
              </nav>

              <div class="dh-hdr__acts">
                <router-link to="/search" class="dh-icon-btn" title="搜索" aria-label="搜索">
                  <AppIcon name="search" :size="17" />
                </router-link>

                <button
                  class="dh-icon-btn dh-hide-mobile"
                  type="button"
                  :title="theme === 'dark' ? '切换到浅色' : '切换到深色'"
                  aria-label="切换主题"
                  @click="toggleTheme"
                >
                  <AppIcon :name="theme === 'dark' ? 'sun' : 'moon'" :size="17" />
                </button>

                <template v-if="isLoggedIn">
                  <router-link to="/notifications" class="dh-icon-btn" title="消息中心" aria-label="消息中心">
                    <AppIcon name="bell" :size="17" />
                    <span v-if="unreadCount > 0" class="dh-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
                  </router-link>
                  <router-link to="/settings" class="dh-icon-btn dh-hide-mobile" title="账号设置" aria-label="账号设置">
                    <AppIcon name="cog" :size="17" />
                  </router-link>
                  <router-link to="/profile" class="dh-user-chip" title="我的主页">
                    <span class="dh-av">{{ displayName.charAt(0) }}</span>
                    <b>{{ displayName }}</b>
                  </router-link>
                  <button
                    class="dh-icon-btn dh-hide-mobile"
                    type="button"
                    title="退出登录"
                aria-label="退出登录"
                @click="handleLogout"
              >
                <AppIcon name="exit" :size="17" />
              </button>
            </template>

            <template v-else>
              <router-link to="/login" class="dh-btn dh-btn--ghost dh-btn--sm">登录</router-link>
              <router-link to="/register" class="dh-btn dh-btn--primary dh-btn--sm">注册</router-link>
            </template>

            <!-- 窄屏收起的入口 -->
            <button
              class="dh-icon-btn dh-only-mobile"
              type="button"
              title="更多"
              aria-label="更多"
              :aria-expanded="menuOpen"
              @click="menuOpen = !menuOpen"
            >
              <AppIcon :name="menuOpen ? 'close' : 'more'" :size="17" />
            </button>

            <div v-if="menuOpen" class="dh-menu" role="menu">
              <router-link to="/" role="menuitem" @click="menuOpen = false">
                <AppIcon name="home" :size="16" /> 广场
              </router-link>
              <router-link v-if="isLoggedIn" to="/publish" role="menuitem" @click="menuOpen = false">
                <AppIcon name="plus" :size="16" /> 写一条心事
              </router-link>
              <button type="button" role="menuitem" @click="onMenuTheme">
                <AppIcon :name="theme === 'dark' ? 'sun' : 'moon'" :size="16" />
                {{ theme === 'dark' ? '切换到浅色' : '切换到深色' }}
              </button>
              <router-link v-if="isLoggedIn" to="/settings" role="menuitem">
                <AppIcon name="cog" :size="16" /> 账号设置
              </router-link>
              <button v-if="isLoggedIn" class="dh-menu__danger" type="button" role="menuitem" @click="onMenuLogout">
                <AppIcon name="exit" :size="16" /> 退出登录
              </button>
            </div>
          </div>
        </div>
      </div>
    </header>

        <main class="dh-main">
          <router-view />
        </main>

        <footer v-if="!hideChrome" class="dh-ftr">
          <div class="dh-wrap dh-ftr__in">
            <span>© 2026 树洞 · Deep Hollow — 说给懂的人听，不必署名</span>
            <span class="dh-ftr__links">
              <router-link to="/notifications">消息</router-link>
              <router-link to="/settings">设置</router-link>
            </span>
          </div>
        </footer>
      </div>
    </div>

    <BackToTop v-if="!hideChrome" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { logout as doLogout } from './api/auth'
import { getUnreadCount } from './api/treehole'
import { useTheme } from './composables/useTheme'
import AppIcon from './components/AppIcon.vue'
import IconSprite from './components/IconSprite.vue'
import BackdropFx from './components/BackdropFx.vue'
import AppSidenav from './components/AppSidenav.vue'
import BackToTop from './components/BackToTop.vue'

const route = useRoute()
const router = useRouter()
const { theme, toggleTheme } = useTheme()

// 登录/注册页为整屏分屏布局，不显示顶栏与页脚
const hideChrome = computed(() => route.meta.guest === true)

const unreadCount = ref(0)
const nickname = ref(localStorage.getItem('th_nickname') || '')
const tokenRef = ref(localStorage.getItem('th_token') || '')
const menuOpen = ref(false)

const isLoggedIn = computed(() => {
  const token = tokenRef.value
  if (!token) return false
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return Date.now() < payload.exp * 1000
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
  } catch {
    /* 未登录或网络异常时静默 */
  }
}

function onStorageChange() {
  tokenRef.value = localStorage.getItem('th_token') || ''
  nickname.value = localStorage.getItem('th_nickname') || ''
  loadUnreadCount()
}

function onMenuTheme() {
  toggleTheme()
  menuOpen.value = false
}

function onMenuLogout() {
  menuOpen.value = false
  handleLogout()
}

function onDocClick(e: MouseEvent) {
  if (!menuOpen.value) return
  const target = e.target as HTMLElement | null
  if (target?.closest('.dh-hdr__acts')) return
  menuOpen.value = false
}

// 路由切换后收起移动端菜单
watch(() => route.fullPath, () => { menuOpen.value = false })

onMounted(() => {
  window.addEventListener('storage', onStorageChange)
  document.addEventListener('click', onDocClick)
  loadUnreadCount()
})

onUnmounted(() => {
  window.removeEventListener('storage', onStorageChange)
  document.removeEventListener('click', onDocClick)
})
</script>
