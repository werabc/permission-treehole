<template>
  <div class="dh-bg" aria-hidden="true">
    <!-- 层序不能调：veil 必须在 nebula 之前，否则收尾的不透明底色会把彩色雾团整块盖掉 -->
    <div class="dh-bg__art" />
    <div class="dh-bg__veil" />
    <div class="dh-nebula dh-nebula--1" />
    <div class="dh-nebula dh-nebula--2" />
    <div class="dh-nebula dh-nebula--3" />
    <div class="dh-bg__band" />
    <div class="dh-bg__ribbon" />
    <div class="dh-bg__vig" />
    <div class="dh-bg__grain" />
    <canvas ref="canvasRef" class="dh-bg__stars" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'

/**
 * 全局背景装饰层
 * 深色：极光图(ken-burns) + 3 团星云 + 银河带 + 三层视差星空 + 随机流星
 * 浅色：极光图(soft-light) + 3 团晨雾 + 晨光色彩带（已去掉原来的网格）
 *
 * 全部星星画在一个 canvas 上；页面不可见时暂停 rAF；prefers-reduced-motion 下不渲染星空。
 */

type Star = { li: number; x: number; y: number; r: number; a: number; ph: number; sp: number; v: number }
type Shoot = { x: number; y: number; vx: number; vy: number; len: number; life: number }

const canvasRef = ref<HTMLCanvasElement | null>(null)

// 三层：远星铺底 / 中星呼吸 / 近星发光
const LAYERS = [
  { n: 0, m: 0, rMin: 0.25, rMax: 0.72, aMin: 0.12, aMax: 0.42, v: 0.014, tw: 0.5, depth: 0.02, glow: false },
  { n: 0, m: 0, rMin: 0.7, rMax: 1.35, aMin: 0.28, aMax: 0.68, v: 0.03, tw: 1, depth: 0.05, glow: false },
  { n: 0, m: 0, rMin: 1.4, rMax: 2.3, aMin: 0.55, aMax: 1, v: 0.05, tw: 1.7, depth: 0.09, glow: true },
]

let raf = 0
let ctx: CanvasRenderingContext2D | null = null
let w = 0
let h = 0
let dpr = 1
let stars: Star[] = []
let shoots: Shoot[] = []
let nextShoot = 3000
let last = 0

function resize() {
  const c = canvasRef.value
  if (!c) return
  dpr = Math.min(window.devicePixelRatio || 1, 2)
  w = c.width = window.innerWidth * dpr
  h = c.height = window.innerHeight * dpr
  c.style.width = `${window.innerWidth}px`
  c.style.height = `${window.innerHeight}px`
}

function initStars() {
  const mobile = window.innerWidth < 720
  const counts = mobile ? [100, 48, 16] : [210, 96, 30]
  stars = []
  LAYERS.forEach((L, li) => {
    for (let i = 0; i < counts[li]; i++) {
      stars.push({
        li,
        x: Math.random() * w,
        y: Math.random() * h,
        r: (L.rMin + Math.random() * (L.rMax - L.rMin)) * dpr,
        a: L.aMin + Math.random() * (L.aMax - L.aMin),
        ph: Math.random() * Math.PI * 2,
        sp: 0.0004 + Math.random() * 0.0012,
        v: L.v * dpr,
      })
    }
  })
}

function frame(t: number) {
  const dt = Math.min(t - last, 60)
  last = t
  if (!ctx) return
  ctx.clearRect(0, 0, w, h)
  const scroll = window.scrollY * dpr

  for (const s of stars) {
    const L = LAYERS[s.li]
    s.ph += s.sp * dt
    s.y -= s.v * (dt / 16)
    if (s.y < -6) {
      s.y = h + 6
      s.x = Math.random() * w
    }
    // 三层不同系数做滚动视差，星星会"错位"，比贴纸有纵深
    const y = s.y - scroll * L.depth
    const yy = ((y % (h + 20)) + h + 20) % (h + 20) - 10
    const alpha = Math.min(Math.max(s.a * (0.72 + Math.sin(s.ph) * 0.28 * L.tw), 0.02), 1)
    ctx.beginPath()
    ctx.arc(s.x, yy, s.r, 0, Math.PI * 2)
    if (L.glow) {
      ctx.shadowBlur = 7 * dpr
      ctx.shadowColor = 'rgba(190, 240, 226, 0.85)'
    }
    ctx.fillStyle = `rgba(214, 244, 236, ${alpha.toFixed(3)})`
    ctx.fill()
    ctx.shadowBlur = 0
  }

  // 流星：每 5–13 秒一颗，带渐隐拖尾
  nextShoot -= dt
  if (nextShoot <= 0 && shoots.length < 2) {
    nextShoot = 5200 + Math.random() * 8000
    const fromLeft = Math.random() > 0.45
    const sp = (0.55 + Math.random() * 0.5) * dpr
    shoots.push({
      x: fromLeft ? Math.random() * w * 0.5 : w * (0.5 + Math.random() * 0.5),
      y: Math.random() * h * 0.55,
      vx: (fromLeft ? 1 : -1) * sp,
      vy: sp * 0.62,
      len: (90 + Math.random() * 90) * dpr,
      life: 1,
    })
  }
  for (let i = shoots.length - 1; i >= 0; i--) {
    const s = shoots[i]
    s.x += s.vx * (dt / 16)
    s.y += s.vy * (dt / 16)
    s.life -= dt / 1300
    if (s.life <= 0) {
      shoots.splice(i, 1)
      continue
    }
    const m = Math.hypot(s.vx, s.vy)
    const tx = s.x - (s.vx / m) * s.len
    const ty = s.y - (s.vy / m) * s.len
    const g = ctx.createLinearGradient(s.x, s.y, tx, ty)
    g.addColorStop(0, `rgba(226, 252, 246, ${(0.8 * s.life).toFixed(3)})`)
    g.addColorStop(0.35, `rgba(160, 235, 215, ${(0.35 * s.life).toFixed(3)})`)
    g.addColorStop(1, 'rgba(160, 235, 215, 0)')
    ctx.strokeStyle = g
    ctx.lineWidth = 1.6 * dpr
    ctx.lineCap = 'round'
    ctx.beginPath()
    ctx.moveTo(s.x, s.y)
    ctx.lineTo(tx, ty)
    ctx.stroke()
    ctx.beginPath()
    ctx.arc(s.x, s.y, 1.5 * dpr, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(240, 255, 251, ${(0.9 * s.life).toFixed(3)})`
    ctx.fill()
  }

  raf = requestAnimationFrame(frame)
}

function start() {
  cancelAnimationFrame(raf)
  last = performance.now()
  raf = requestAnimationFrame(frame)
}

function onResize() {
  cancelAnimationFrame(raf)
  resize()
  initStars()
  start()
}

function onVisibility() {
  if (document.hidden) cancelAnimationFrame(raf)
  else start()
}

onMounted(() => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  const c = canvasRef.value
  if (!c) return
  ctx = c.getContext('2d')
  if (!ctx) return
  resize()
  initStars()
  start()
  window.addEventListener('resize', onResize)
  document.addEventListener('visibilitychange', onVisibility)
})

onUnmounted(() => {
  cancelAnimationFrame(raf)
  window.removeEventListener('resize', onResize)
  document.removeEventListener('visibilitychange', onVisibility)
})
</script>
