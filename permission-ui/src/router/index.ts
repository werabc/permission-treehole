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

/**
 * 递归添加路由 - 将嵌套路由扁平化后全部添加到 Layout 下
 */
function addRoutesRecursively(route: RouteRecordRaw) {
  // 添加当前路由（如果有组件）
  if (route.component) {
    router.addRoute('Layout', {
      path: route.path,
      name: route.name,
      component: route.component,
      meta: route.meta,
      redirect: route.redirect,
    })
  }
  // 递归添加子路由
  if (route.children && route.children.length > 0) {
    for (const child of route.children) {
      addRoutesRecursively(child)
    }
  }
}

export function addDynamicRoutes(routes: RouteRecordRaw[]) {
  if (routesAdded) return
  routesAdded = true
  for (const route of routes) {
    addRoutesRecursively(route)
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
  const refreshTokenVal = localStorage.getItem('refreshToken')

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
    } catch (e: any) {
      // 获取失败，尝试刷新 token
      if (refreshTokenVal) {
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
      }
      // 清除所有登录信息并跳转登录
      userStore.logoutAction()
      next('/login')
      return
    }
  }

  // 验证 token 是否过期（通过检查 token 的 payload）
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    const exp = payload.exp * 1000 // 转换为毫秒
    if (Date.now() >= exp) {
      // Token 过期，尝试刷新
      if (refreshTokenVal) {
        try {
          const success = await userStore.refreshAction()
          if (success) {
            await userStore.fetchUserInfo()
            next({ ...to, replace: true })
            return
          }
        } catch {
          // 刷新失败
        }
      }
      // 清除所有登录信息并跳转登录
      userStore.logoutAction()
      next('/login')
      return
    }
  } catch {
    // Token 格式无效，清除并跳转
    userStore.logoutAction()
    next('/login')
    return
  }

  next()
})

export default router
