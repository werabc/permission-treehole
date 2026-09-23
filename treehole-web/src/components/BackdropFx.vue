<template>
  <div class="dh-bg" aria-hidden="true">
    <div class="dh-bg__art" />
    <div class="dh-bg__veil" />
    <div class="dh-bg__grid" />
    <div class="dh-bg__grain" />
    <canvas ref="canvasRef" class="dh-bg__stars" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'

/**
 * 全局背景装饰层：极光图(ken-burns) + 网格 + 噪点 + 轻量星点 canvas
 * 星点在 prefers-reduced-motion 下自动关闭；页面不可见时暂停渲染省电
 */
const canvasRef = ref<HTMLCanvasElement | null>(null)

let raf = 0
let ctx: CanvasRenderingContext2D | null = null
let w = 0
let h = 0
let dpr = 1
let points: Array<{ x: number; y: number; r: number; a: number; s: number; v: number }> = []

function resize() {
  const c = canvasRef.value
  if (!c) return
  dpr = Math.min(window.devicePixelRatio || 1, 2)
  w = c.width = window.innerWidth * dpr
  h = c.height = window.innerHeight * dpr
  c.style.width = `${window.innerWidth}px`
  c.style.height = `${window.innerHeight}px`
}

function init() {
  const count = window.innerWidth < 720 ? 60 : 160
  points = Array.from({ length: count }, () => ({
    x: Math.random() * w,
    y: Math.random() * h,
    r: (Math.random() * 1.1 + 0.3) * dpr,
    a: Math.random() * Math.PI * 2,
    s: Math.random() * 0.0016 + 0.0004,
    v: (Math.random() * 0.06 + 0.02) * dpr,
  }))
}

function draw() {
  if (!ctx) return
  ctx.clearRect(0, 0, w, h)
  for (const p of points) {
    p.a += p.s * 12
    p.y -= p.v
    if (p.y < -4) {
      p.y = h + 4
      p.x = Math.random() * w
    }
    const alpha = Math.max(0.28 + Math.sin(p.a) * 0.26, 0.04)
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(190, 240, 226, ${alpha.toFixed(3)})`
    ctx.fill()
  }
  raf = requestAnimationFrame(draw)
}

function onResize() {
  cancelAnimationFrame(raf)
  resize()
  init()
  draw()
}

function onVisibility() {
  if (document.hidden) {
    cancelAnimationFrame(raf)
  } else {
    cancelAnimationFrame(raf)
    draw()
  }
}

onMounted(() => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  const c = canvasRef.value
  if (!c) return
  ctx = c.getContext('2d')
  resize()
  init()
  draw()
  window.addEventListener('resize', onResize)
  document.addEventListener('visibilitychange', onVisibility)
})

onUnmounted(() => {
  cancelAnimationFrame(raf)
  window.removeEventListener('resize', onResize)
  document.removeEventListener('visibilitychange', onVisibility)
})
</script>
