<template>
  <div class="search-page">
    <form class="search-bar" @submit.prevent="doSearch(1)">
      <input
        v-model="keyword"
        type="search"
        placeholder="搜索帖子内容或作者昵称…"
        aria-label="搜索关键词"
      />
      <button type="submit" :disabled="!keyword.trim()">搜索</button>
    </form>

    <p v-if="searched" class="result-hint">
      关键词「{{ lastKeyword }}」共找到 <strong>{{ total }}</strong> 条帖子
    </p>

    <div v-if="loading" class="state">搜索中…</div>
    <div v-else-if="searched && posts.length === 0" class="state">没有找到相关内容，换个关键词试试</div>

    <div v-else>
      <PostCard
        v-for="post in posts"
        :key="post.id"
        :post="post"
      />
      <div v-if="hasMore" class="load-more">
        <button type="button" @click="doSearch(pageNum + 1)">加载更多</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PostCard from '../components/PostCard.vue'
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
  // 同步到地址栏，便于分享/刷新
  router.replace({ path: '/search', query: { keyword: kw } })
  try {
    const res = await searchPosts({ keyword: kw, pageNum: page, pageSize })
    const records = res.data.records || []
    posts.value = page === 1 ? records : posts.value.concat(records)
    total.value = res.data.total || 0
  } catch (e) {
    console.error('搜索失败', e)
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
.search-page { max-width: 720px; margin: 0 auto; }
.search-bar { display: flex; gap: 10px; margin-bottom: 18px; }
.search-bar input {
  flex: 1; padding: 11px 16px; border: 1px solid #e2e8f0; border-radius: 10px;
  font-size: 14px; outline: none; transition: border-color 0.2s;
}
.search-bar input:focus { border-color: #93c5fd; box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.10); }
.search-bar button {
  background: #3b82f6; color: #fff; border: none; border-radius: 10px;
  padding: 0 22px; font-size: 14px; cursor: pointer; transition: background 0.2s;
}
.search-bar button:hover { background: #2563eb; }
.search-bar button:disabled { background: #cbd5e1; cursor: not-allowed; }
.result-hint { color: #64748b; font-size: 13px; margin-bottom: 14px; }
.result-hint strong { color: #3b82f6; }
.state { text-align: center; color: #94a3b8; padding: 48px 0; font-size: 14px; }
.load-more { text-align: center; margin-top: 8px; }
.load-more button {
  background: #fff; border: 1px solid #e2e8f0; color: #64748b;
  padding: 8px 20px; border-radius: 8px; cursor: pointer; font-size: 13px;
}
</style>
