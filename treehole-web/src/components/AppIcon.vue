<template>
  <svg
    :width="size"
    :height="size"
    :stroke-width="strokeWidth"
    aria-hidden="true"
    focusable="false"
    style="flex-shrink: 0"
  >
    <use :href="`#i-${name}`" />
  </svg>
</template>

<script setup lang="ts">
/**
 * 统一图标组件 —— 引用 IconSprite 中的 symbol，替代原来的 emoji 图标
 * 用法：<AppIcon name="bell" :size="17" />
 */
withDefaults(defineProps<{ name: string; size?: number; strokeWidth?: number }>(), {
  size: 16,
  strokeWidth: 1.7,
})
</script>
