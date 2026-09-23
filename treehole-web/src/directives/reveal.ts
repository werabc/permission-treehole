import type { Directive } from 'vue'

/**
 * v-reveal 指令：元素进入视口时上浮淡入，可选延迟做交错（stagger）
 *
 * 用法：
 *   <div v-reveal>…</div>
 *   <div v-reveal="index * 70">…</div>   // 第 index 条延迟 index*70ms
 */
let observer: IntersectionObserver | null = null

function getObserver() {
  if (!observer) {
    observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-in')
            observer!.unobserve(entry.target)
          }
        }
      },
      { rootMargin: '-40px 0px -60px' }
    )
  }
  return observer
}

function reducedMotion() {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

export const reveal: Directive<HTMLElement, number | undefined> = {
  mounted(el, binding) {
    el.classList.add('dh-reveal')
    const delay = Number(binding.value)
    if (Number.isFinite(delay) && delay > 0) {
      el.style.setProperty('--reveal-delay', `${delay}ms`)
    }
    if (reducedMotion()) {
      el.classList.add('is-in')
      return
    }
    getObserver().observe(el)
  },
  unmounted(el) {
    observer?.unobserve(el)
  },
}
