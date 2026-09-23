import { ref, watch } from 'vue'

const STORAGE_KEY = 'th_theme'
export type Theme = 'dark' | 'light'

function detect(): Theme {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved === 'dark' || saved === 'light') return saved
  } catch {
    /* 隐身模式等场景忽略 */
  }
  // 树洞的气质以深空为基调，默认深色
  return 'dark'
}

const theme = ref<Theme>(detect())

function apply(value: Theme) {
  const root = document.documentElement
  root.dataset.theme = value
  // Element Plus 的深色变量挂在 html.dark 上，需要同步
  root.classList.toggle('dark', value === 'dark')
}

apply(theme.value)

watch(theme, (value) => {
  apply(value)
  try {
    localStorage.setItem(STORAGE_KEY, value)
  } catch {
    /* ignore */
  }
})

export function useTheme() {
  return {
    theme,
    toggleTheme: () => {
      theme.value = theme.value === 'dark' ? 'light' : 'dark'
    },
  }
}
