<template>
  <Transition name="totop">
    <button
      v-if="visible"
      class="dh-totop"
      type="button"
      title="回到顶部"
      aria-label="回到顶部"
      @click="toTop"
    >
      <AppIcon name="chevron-down" :size="17" class="dh-totop__ic" />
    </button>
  </Transition>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import AppIcon from './AppIcon.vue'

const visible = ref(false)

function onScroll() {
  visible.value = window.scrollY > 700
}

function toTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
})

onUnmounted(() => window.removeEventListener('scroll', onScroll))
</script>
