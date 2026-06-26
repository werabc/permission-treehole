import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const staticRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', noAuth: true },
  },
  // Public novel pages — standalone, no layout/sidebar required
  {
    path: '/novel',
    name: 'NovelList',
    component: () => import('@/views/novel/List.vue'),
    meta: { title: '小说广场', noAuth: true },
  },
  {
    path: '/novel/:id',
    name: 'NovelDetail',
    component: () => import('@/views/novel/Detail.vue'),
    meta: { title: '小说详情', noAuth: true },
  },
  {
    path: '/novel/read/:novelId/:chapterId',
    name: 'NovelRead',
    component: () => import('@/views/novel/Read.vue'),
    meta: { title: '阅读', noAuth: true },
  },
  // Auth-required pages under layout
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
        path: '/bookshelf',
        name: 'Bookshelf',
        component: () => import('@/views/novel/Bookshelf.vue'),
        meta: { title: '我的书架', icon: 'Star' },
      },
      {
        path: '/author',
        name: 'AuthorDashboard',
        component: () => import('@/views/author/Dashboard.vue'),
        meta: { title: '作者中心', icon: 'Edit' },
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

export default router
