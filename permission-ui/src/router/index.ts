import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'

const staticRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', noAuth: true },
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/views/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'HomeFilled' },
      },
      {
        path: '/profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人信息', icon: 'User' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/layout/404.vue'),
    meta: { title: '404', noAuth: true },
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes: staticRoutes,
  scrollBehavior: () => ({ top: 0 }),
})

let routesAdded = false

export function addDynamicRoutes(routes: RouteRecordRaw[]) {
  if (routesAdded) return
  routesAdded = true
  for (const route of routes) {
    router.addRoute('Layout', route)
  }
}

export function resetDynamicRoutes() {
  routesAdded = false
}

// ========== 路由守卫 ==========
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  const permissionStore = usePermissionStore()
  const token = localStorage.getItem('accessToken')

  // 设置页面标题
  document.title = (to.meta.title ? `${to.meta.title} - ` : '') + '权限管理系统'

  // 公开路由直接放行
  if (to.meta.noAuth) {
    next()
    return
  }

  // 无 token 跳转登录
  if (!token) {
    next('/login')
    return
  }

  // 有 token 但无用户信息，尝试获取
  if (!userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
      if (!routesAdded) {
        await permissionStore.generateRoutes()
      }
      next({ ...to, replace: true })
      return
    } catch {
      // 获取失败，尝试刷新 token
      try {
        const success = await userStore.refreshAction()
        if (success) {
          await userStore.fetchUserInfo()
          if (!routesAdded) {
            await permissionStore.generateRoutes()
          }
          next({ ...to, replace: true })
          return
        }
      } catch {
        // 刷新也失败
      }
      localStorage.clear()
      next('/login')
      return
    }
  }

  next()
})

export default router
