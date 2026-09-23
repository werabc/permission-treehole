<template>
  <aside class="dh-sidenav">
    <div class="dh-sidenav__in">
      <router-link to="/" class="dh-sidenav__lk" :class="{ 'is-on': isPlaza }">
        <AppIcon name="home" :size="17" />
        广场
      </router-link>
      <router-link to="/publish" class="dh-sidenav__lk" :class="{ 'is-on': route.path === '/publish' }">
        <AppIcon name="plus" :size="17" />
        写一条心事
      </router-link>

      <template v-if="isLoggedIn">
        <router-link to="/notifications" class="dh-sidenav__lk" :class="{ 'is-on': route.path === '/notifications' }">
          <AppIcon name="mail" :size="17" />
          消息
          <span v-if="unreadCount > 0" class="dh-sidenav__cnt">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        </router-link>
        <router-link to="/profile" class="dh-sidenav__lk" :class="{ 'is-on': route.path === '/profile' }">
          <AppIcon name="cell" :size="17" />
          我的主页
        </router-link>
      </template>

      <div class="dh-sidenav__hr" />
      <div class="dh-sidenav__h">分类</div>

      <button
        class="dh-sidenav__cat"
        :class="{ 'is-on': activeCategory === undefined }"
        type="button"
        @click="pickCategory(undefined)"
      >
        全部 <b>{{ total }}</b>
      </button>
      <button
        v-for="cat in categories"
        :key="cat.id"
        class="dh-sidenav__cat"
        :class="{ 'is-on': activeCategory === cat.id }"
        type="button"
        @click="pickCategory(cat.id)"
      >
        {{ cat.name }} <b>{{ cat.postCount || 0 }}</b>
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from './AppIcon.vue'
import { getCategoryList, getPostPage } from '../api/treehole'
import type { Category } from '../api/treehole'

defineProps<{ unreadCount: number; isLoggedIn: boolean }>()

const route = useRoute()
const router = useRouter()
const categories = ref<Category[]>([])
const total = ref(0)

/** 选中分类同步在 URL 的 ?category= 上，这样可分享、可后退 */
const activeCategory = computed(() => {
  const raw = route.query.category
  const n = Number(Array.isArray(raw) ? raw[0] : raw)
  return Number.isInteger(n) && n > 0 ? n : undefined
})

const isPlaza = computed(() => route.path === '/' && activeCategory.value === undefined)

function pickCategory(id?: number) {
  if (route.path !== '/') {
    router.push(id ? { path: '/', query: { category: String(id) } } : { path: '/' })
    return
  }
  if (activeCategory.value === id) return
  // 用 push 而不是 replace，保证浏览器后退能回到上一个分类
  router.push({ path: '/', query: id ? { category: String(id) } : {} })
}

async function load() {
  try {
    const res = await getCategoryList()
    categories.value = res.data || []
  } catch {
    /* 分类加载失败不影响主流程 */
  }
  try {
    // 「全部」的口径必须以服务端 total 为准：分类求和会漏掉没选分类的帖子
    const res = await getPostPage({ pageNum: 1, pageSize: 1 })
    total.value = res.data.total || 0
  } catch {
    /* ignore */
  }
}

onMounted(load)
</script>
