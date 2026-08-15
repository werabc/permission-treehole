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

  /**
   * 根据菜单 component 字段动态解析组件路径
   * 支持格式：
   *   - "views/system/user/index" → 动态导入 @/views/system/user/index.vue
   *   - "system/user" → 自动补全为 @/views/system/user/index.vue
   *   - 空 → 使用通用 EmptyComponent
   */
  function resolveComponent(menu: SysMenu): (() => Promise<any>) | undefined {
    if (!menu.component && menu.menuType !== 'BUTTON') {
      // 尝试从路径推断组件
      if (menu.path && menu.path !== '#') {
        const path = menu.path.startsWith('/') ? menu.path.slice(1) : menu.path
        return () => import(`@/views/${path}/index.vue`).catch(() => import('@/views/components/RouteView.vue'))
      }
      return undefined
    }

    if (!menu.component) return undefined

    const componentPath = menu.component

    // 已经是完整路径（以 views/ 开头）
    if (componentPath.startsWith('views/')) {
      const path = componentPath.replace(/^views\//, '')
      return () => import(`@/views/${path}.vue`).catch(() => import('@/views/components/RouteView.vue'))
    }

    // 以 / 开头（如 /system/user）
    if (componentPath.startsWith('/')) {
      const path = componentPath.slice(1)
      return () => import(`@/views/${path}/index.vue`).catch(() => import('@/views/components/RouteView.vue'))
    }

    // 相对路径（如 system/user）
    return () => import(`@/views/${componentPath}/index.vue`).catch(() => import('@/views/components/RouteView.vue'))
  }

  function buildRoutes(menus: SysMenu[]): any[] {
    const routes: any[] = []

    for (const menu of menus) {
      if (menu.menuType === 'BUTTON') continue

      const route: any = {
        path: menu.path,
        name: menu.path ? menu.path.replace(/\//g, '_').replace(/^_/, '') : `menu_${menu.id}`,
        meta: {
          title: menu.menuName,
          icon: menu.icon,
          permission: menu.permission,
          menuId: menu.id,
        },
      }

      // 解析组件
      const component = resolveComponent(menu)
      if (component) {
        route.component = component
      }

      // 递归处理子菜单
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
    return routes
  }

  function resetRoutes() {
    menus.value = []
    addRoutes.value = []
  }

  return { menus, addRoutes, generateRoutes, resetRoutes }
})
