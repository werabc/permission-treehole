<template>
  <!-- 有头像用头像；没有则回落到首字母。匿名统一用遮罩图标，不暴露任何可识别信息 -->
  <span
    class="dhav"
    :class="{ 'dhav--anon': anonymous, 'dhav--img': showImg }"
    :style="{ width: size + 'px', height: size + 'px', fontSize: Math.round(size * 0.42) + 'px' }"
  >
    <img v-if="showImg" :src="src!" :alt="name || '头像'" loading="lazy" decoding="async" @error="failed = true" />
    <AppIcon v-else-if="anonymous" name="mask" :size="Math.round(size * 0.5)" />
    <template v-else>{{ initial }}</template>
  </span>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import AppIcon from './AppIcon.vue'

const props = withDefaults(
  defineProps<{
    src?: string | null
    name?: string | null
    size?: number
    /** 匿名：既不显示头像也不显示首字母，避免去匿名化 */
    anonymous?: boolean
  }>(),
  { src: '', name: '', size: 36, anonymous: false },
)

const initial = computed(() => (props.name || '?').trim().charAt(0).toUpperCase() || '?')

/** 图片加载失败标记：破损的 URL 不该留一个破图，直接退回首字母 */
const failed = ref(false)
watch(() => props.src, () => { failed.value = false })

/**
 * 只有当 src 看起来是"真的图片地址"时才渲染 img。
 * 历史数据里出现过 'default.png' 这种占位字符串（不是可访问 URL）→ 必然破图，
 * 这里直接拦掉，让用户看到首字母而不是一个碎图图标。
 */
const isUsableUrl = computed(() => {
  const s = (props.src || '').trim()
  if (!s) return false
  return /^(https?:\/\/|\/)/.test(s)
})

const showImg = computed(() => isUsableUrl.value && !props.anonymous && !failed.value)
</script>

<style scoped>
.dhav {
  display: inline-grid;
  place-items: center;
  flex-shrink: 0;
  border-radius: 999px;
  overflow: hidden;
  font-weight: 600;
  color: var(--accent);
  background: var(--accent-soft);
  border: 1px solid var(--accent-line);
  user-select: none;
}
.dhav--img {
  background: var(--surface-2);
  border-color: var(--border);
}
.dhav--img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.dhav--anon {
  color: var(--text-mute);
  background: color-mix(in srgb, var(--text) 7%, transparent);
  border-color: var(--border);
}
</style>
