import { createRouter, createWebHashHistory } from 'vue-router'
import { isLoggedIn } from '../api/auth'

const routes = [
  { path: '/', name: 'Home', component: () => import('../views/Home.vue') },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { guest: true } },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue'), meta: { guest: true } },
  { path: '/publish', name: 'Publish', component: () => import('../views/Publish.vue'), meta: { auth: true } },
  { path: '/post/:id', name: 'PostDetail', component: () => import('../views/PostDetail.vue') },
  { path: '/category/:code', name: 'Category', component: () => import('../views/Category.vue') },
  { path: '/profile', name: 'Profile', component: () => import('../views/Profile.vue'), meta: { auth: true } },
  // Alibaba-Java: 增加 404 页面
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('../views/Home.vue') },
]

const router = createRouter({
  // Alibaba-Java: 使用 hash 模式避免刷新 404
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach((to) => {
  document.title = (to.meta.title ? `${to.meta.title} - ` : '') + '树洞'
  // Alibaba-Java: 路由守卫 — isLoggedIn() 已包含 Token 过期检查
  if (to.meta.auth && !isLoggedIn()) return '/login'
  if (to.meta.guest && isLoggedIn()) return '/'
})

export default router
