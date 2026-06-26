import type { Directive } from 'vue'
import { useUserStore } from '@/stores/user'

export const permissionDirective: Directive = {
  mounted(el, binding) {
    const { value } = binding
    if (value) {
      const userStore = useUserStore()
      if (!userStore.hasPermission(value as string)) {
        el.parentNode?.removeChild(el)
      }
    }
  },
}
