<template>
  <div class="dh-state">正在跳转到分类…</div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCategoryList } from '../api/treehole'

/**
 * 历史上分类有独立页面，和首页的分类筛选是两套重复入口。
 * 现在统一到首页的 ?category=<id>（可分享、可后退），这里只做跳转。
 */
const route = useRoute()
const router = useRouter()

onMounted(async () => {
  const code = String(route.params.code || '')
  try {
    const res = await getCategoryList()
    const cat = (res.data || []).find((c) => c.code === code)
    if (cat) {
      router.replace({ path: '/', query: { category: String(cat.id) } })
      return
    }
  } catch {
    /* 分类加载失败就退回广场 */
  }
  router.replace('/')
})
</script>
