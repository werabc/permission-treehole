<template>
  <div class="dh-narrow search-page">
    <form class="dh-card bar" v-reveal @submit.prevent="doSearch(1)">
      <AppIcon name="search" :size="18" class="ico" />
      <input
        v-model="keyword"
        type="search"
        placeholder="搜索心事内容或作者昵称…"
        aria-label="搜索关键词"
      />
      <button class="dh-btn dh-btn--primary dh-btn--sm" type="submit" :disabled="!keyword.trim()">搜索</button>
    </form>

    <p v-if="searched" class="hint">
      关键词「<b>{{ lastKeyword }}</b>」共找到 <b>{{ total }}</b> 条心事
    </p>

    <div v-if="loading && posts.length === 0" class="feed">
      <div v-for="n in 3" :key="n" class="dh-skeleton" />
    </div>

    <div v-else-if="searched && posts.length === 0" class="dh-state">
      <p>没有找到相关的心事</p>
      <p style="font-size: 13px; margin-top: 6px">换个关键词试试</p>
    </div>

    <div v-else class="feed">
      <PostCard v-for="(post, i) in posts" :key="post.id" v-reveal="Math.min(i, 6) * 60" :post="post" />
      <div v-if="hasMore" class="load-more">
        <button class="dh-btn dh-btn--ghost" type="button" :disabled="loading" @click="doSearch(pageNum + 1)">
          {{ loading ? '加载中…' : '加载更多' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PostCard from '../components/PostCard.vue'
import AppIcon from '../components/AppIcon.vue'
import { searchPosts, type Post } from '../api/treehole'

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const lastKeyword = ref('')
const posts = ref<Post[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const loading = ref(false)
const searched = ref(false)

const hasMore = computed(() => posts.value.length < total.value)

async function doSearch(page = 1) {
  const kw = keyword.value.trim()
  if (!kw) return
  loading.value = true
  searched.value = true
  lastKeyword.value = kw
  pageNum.value = page
  router.replace({ path: '/search', query: { keyword: kw } })
  try {
    const res = await searchPosts({ keyword: kw, pageNum: page, pageSize })
    const records = res.data.records || []
    posts.value = page === 1 ? records : posts.value.concat(records)
    total.value = res.data.total || 0
  } catch {
    posts.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  const kw = (route.query.keyword as string) || ''
  if (kw) {
    keyword.value = kw
    doSearch(1)
  }
})

watch(() => route.query.keyword, (kw) => {
  if (typeof kw === 'string' && kw && kw !== lastKeyword.value) {
    keyword.value = kw
    doSearch(1)
  }
})
</script>

<style scoped>
.search-page { padding: 40px 0 70px; }
.bar { display: flex; align-items: center; gap: 12px; padding: 8px 8px 8px 20px; border-radius: 999px; }
.ico { color: var(--text-mute); }
.bar input { flex: 1; font-size: 15px; padding: 11px 0; min-width: 0; }
.bar input::placeholder { color: var(--text-mute); }

.hint { margin: 20px 4px 16px; font-size: 13px; color: var(--text-dim); }
.hint b { color: var(--accent); font-family: var(--font-mono); }
.feed { display: grid; gap: 12px; }
.load-more { text-align: center; margin-top: 20px; }

@media (max-width: 720px) {
  .search-page { padding: 26px 0 60px; }
}
</style>
