/**
 * 全局按钮微交互 —— 用事件委托一次性安装，页面里不用给每个按钮单独绑定。
 *
 * ① 磁吸：按钮跟光标轻微位移，内层文案再慢一点，形成层次
 * ② 聚光跟随：把光标位置写进 --mx/--my，交给 CSS 的 radial-gradient 画光
 * ③ 涟漪：按下点荡开一圈（只对 .dh-btn，图标按钮太小且徽标会被 overflow 裁掉）
 * ④ 按下回弹：纯 CSS（--ps），不在这里处理
 *
 * 触摸设备没有 hover 概念 → 不启用磁吸；prefers-reduced-motion → 整体不启用。
 */
const MAGNETIC_SELECTOR = '.dh-btn, .dh-icon-btn'
const RIPPLE_SELECTOR = '.dh-btn'

export function installButtonFx() {
  if (typeof window === 'undefined' || typeof document === 'undefined') return
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return

  const noHover = window.matchMedia('(hover: none)').matches
  const bound = new WeakSet<Element>()

  function bindMagnetic(el: HTMLElement) {
    if (bound.has(el)) return
    bound.add(el)
    const strength = el.classList.contains('dh-icon-btn') ? 0.12 : 0.2
    const label = el.querySelector<HTMLElement>('.dh-btn__lb')

    el.addEventListener('pointermove', (e) => {
      const r = el.getBoundingClientRect()
      if (!r.width || !r.height) return
      const dx = e.clientX - (r.left + r.width / 2)
      const dy = e.clientY - (r.top + r.height / 2)
      el.style.setProperty('--mx', `${(((e.clientX - r.left) / r.width) * 100).toFixed(1)}%`)
      el.style.setProperty('--my', `${(((e.clientY - r.top) / r.height) * 100).toFixed(1)}%`)
      el.style.setProperty('--tx', `${(dx * strength).toFixed(1)}px`)
      el.style.setProperty('--ty', `${(dy * strength).toFixed(1)}px`)
      if (label) {
        label.style.setProperty('--lx', `${(dx * strength * 0.4).toFixed(1)}px`)
        label.style.setProperty('--ly', `${(dy * strength * 0.4).toFixed(1)}px`)
      }
    })

    el.addEventListener('pointerleave', () => {
      el.style.setProperty('--tx', '0px')
      el.style.setProperty('--ty', '0px')
      el.style.setProperty('--mx', '50%')
      el.style.setProperty('--my', '50%')
      if (label) {
        label.style.setProperty('--lx', '0px')
        label.style.setProperty('--ly', '0px')
      }
    })
  }

  // 懒绑定：指针真正划过某个按钮时才给它挂事件，避免给全站按钮一次性挂上
  if (!noHover) {
    document.addEventListener(
      'pointerover',
      (e) => {
        const el = (e.target as Element | null)?.closest?.(MAGNETIC_SELECTOR) as HTMLElement | null
        if (el) bindMagnetic(el)
      },
      { passive: true }
    )
  }

  document.addEventListener(
    'pointerdown',
    (e) => {
      const el = (e.target as Element | null)?.closest?.(RIPPLE_SELECTOR) as HTMLElement | null
      if (!el || (el as HTMLButtonElement).disabled) return
      const r = el.getBoundingClientRect()
      if (!r.width) return
      const rip = document.createElement('span')
      rip.className = 'dh-rip'
      rip.style.left = `${e.clientX - r.left}px`
      rip.style.top = `${e.clientY - r.top}px`
      el.appendChild(rip)
      window.setTimeout(() => rip.remove(), 700)
    },
    { passive: true }
  )
}
