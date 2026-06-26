import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserMenus } from '@/api/menu'
import { addDynamicRoutes } from '@/router'
import type { SysMenu } from '@/types'

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
    const moduleMap: Record<string, () => Promise<any>> = {
      '/system/user': () => import('@/views/system/user/index.vue'),
      '/system/role': () => import('@/views/system/role/index.vue'),
      '/system/menu': () => import('@/views/system/menu/index.vue'),
      '/system/dept': () => import('@/views/system/dept/index.vue'),
      '/log/operation': () => import('@/views/log/operation/index.vue'),
      '/log/login': () => import('@/views/log/login/index.vue'),
      '/dashboard': () => import('@/views/dashboard/index.vue'),
      '/bookshelf': () => import('@/views/novel/Bookshelf.vue'),
      '/author': () => import('@/views/author/Dashboard.vue'),
    }

    for (const menu of menus) {
      if (menu.menuType === 'CATALOG' || menu.menuType === 'MENU') {
        const route: any = {
          path: menu.path,
          name: menu.path.replace(/\//g, '_'),
          meta: {
            title: menu.menuName,
            icon: menu.icon,
            permission: menu.permission,
          },
        }

        if (menu.menuType === 'MENU') {
          if (moduleMap[menu.path]) {
            route.component = moduleMap[menu.path]
          }
        }

        if (menu.children && menu.children.length > 0) {
          route.children = buildRoutes(menu.children)
          if (menu.menuType === 'CATALOG') {
            const firstChild = menu.children.find((c: SysMenu) => c.menuType === 'MENU')
            if (firstChild) {
              route.redirect = firstChild.path
            }
          }
        }

        routes.push(route)
      }
    }
    return routes
  }

  function resetRoutes() {
    menus.value = []
    addRoutes.value = []
  }

  return { menus, addRoutes, generateRoutes, resetRoutes }
})
