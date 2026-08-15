import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserMenus } from '@/api/menu'
import { addDynamicRoutes } from '@/router'
import type { SysMenu } from '@/types'

const componentMap: Record<string, () => Promise<any>> = {
  '/system/user':              () => import('@/views/system/user/index.vue'),
  '/system/role':              () => import('@/views/system/role/index.vue'),
  '/system/menu':              () => import('@/views/system/menu/index.vue'),
  '/system/dept':              () => import('@/views/system/dept/index.vue'),
  '/system/online':            () => import('@/views/system/online/index.vue'),
  '/log/operation':            () => import('@/views/log/operation/index.vue'),
  '/log/login':                () => import('@/views/log/login/index.vue'),
  '/admin/treehole/post':      () => import('@/views/treehole-admin/post/index.vue'),
  '/admin/treehole/statistics': () => import('@/views/treehole-admin/statistics/index.vue'),
  '/dashboard':                () => import('@/views/dashboard/index.vue'),
  '/profile':                  () => import('@/views/profile/index.vue'),
}

function resolveComponent(menu: SysMenu): (() => Promise<any>) | undefined {
  if (menu.menuType === 'BUTTON') return undefined
  if (menu.path && componentMap[menu.path]) return componentMap[menu.path]
  if (menu.menuType === 'CATALOG') return () => import('@/views/components/RouteView.vue')
  console.warn(`[Route] No component found for path: ${menu.path}`)
  return undefined
}

export const usePermissionStore = defineStore('permission', () => {
  const menus = ref<SysMenu[]>([])
  const addRoutes = ref<any[]>([])

  async function generateRoutes() {
    const res = await getUserMenus()
    menus.value = res.data || []
    addRoutes.value = buildRoutes(menus.value)
    addDynamicRoutes(addRoutes.value)
    return addRoutes.value
  }

  function buildRoutes(menus: SysMenu[]): any[] {
    const routes: any[] = []
    for (const menu of menus) {
      if (menu.menuType === 'BUTTON') continue
      const route: any = {
        path: menu.path,
        name: menu.path ? menu.path.replace(/\//g, '_').replace(/^_/, '') : `menu_${menu.id}`,
        meta: { title: menu.menuName, icon: menu.icon, permission: menu.permission, menuId: menu.id },
      }
      const component = resolveComponent(menu)
      if (component) route.component = component
      if (menu.children && menu.children.length > 0) {
        route.children = buildRoutes(menu.children)
        if (menu.menuType === 'CATALOG') {
          const firstChild = menu.children.find((c: SysMenu) => c.menuType === 'MENU')
          if (firstChild) route.redirect = firstChild.path
        }
      }
      routes.push(route)
    }
    return routes
  }

  function resetRoutes() {
    menus.value = []
    addRoutes.value = []
  }

  return { menus, addRoutes, generateRoutes, resetRoutes }
})
