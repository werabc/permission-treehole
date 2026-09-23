<template>
  <div class="dh-narrow category-page">
    <div class="page-hd" v-reveal>
      <h2 class="dh-page-title">{{ categoryName || '分类' }}</h2>
      <p class="dh-page-sub">这个分类下共有 {{ total }} 条心事。</p>
    </div>

    <div v-if="loading && posts.length === 0" class="feed">
      <div v-for="n in 3" :key="n" class="dh-skeleton" />
    </div>

    <div v-else-if="posts.length === 0" class="dh-state">该分类下还没有内容</div>

    <div v-else class="feed">
      <PostCard v-for="(post, i) in posts" :key="post.id" v-reveal="Math.min(i, 6) * 60" :post="post" />
    </div>

    <div v-if="hasMore" class="load-more">
      <button class="dh-btn dh-btn--ghost" type="button" :disabled="loading" @click="loadMore">
        {{ loading ? '加载中…' : '加载更多' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import PostCard from '../components/PostCard.vue'
import { getPostPage, getCategoryList } from '../api/treehole'
import type { Post, Category } from '../api/treehole'

const route = useRoute()
const posts = ref<Post[]>([])
const categories = ref<Category[]>([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = 20
const total = ref(0)
const categoryName = ref('')
const categoryId = ref<number | undefined>(undefined)
const hasMore = ref(false)

async function loadCategories() {
  try {
    const res = await getCategoryList()
    categories.value = res.data || []
    resolveCategoryFromRoute()
  } catch {
    /* ignore */
  }
}

function resolveCategoryFromRoute() {
  const code = route.params.code as string
  const cat = categories.value.find((c) => c.code === code)
  if (cat) {
    categoryId.value = cat.id
    categoryName.value = cat.name
  } else {
    categoryName.value = code
    categoryId.value = undefined
  }
}

async function fetchPosts() {
  loading.value = true
  pageNum.value = 1
  try {
    const res = await getPostPage({ pageNum: 1, pageSize, categoryId: categoryId.value })
    posts.value = res.data.records || []
    total.value = res.data.total || 0
    hasMore.value = posts.value.length < total.value
  } catch {
    /* ignore */
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  loading.value = true
  pageNum.value += 1
  try {
    const res = await getPostPage({ pageNum: pageNum.value, pageSize, categoryId: categoryId.value })
    posts.value = [...posts.value, ...(res.data.records || [])]
    hasMore.value = posts.value.length < total.value
  } finally {
    loading.value = false
  }
}

watch(() => route.params.code, () => {
  resolveCategoryFromRoute()
  fetchPosts()
})

onMounted(async () => {
  await loadCategories()
  await fetchPosts()
})
</script>

<style scoped>
.category-page { padding: 40px 0 70px; }
.page-hd { margin-bottom: 22px; }
.feed { display: grid; gap: 12px; }
.load-more { text-align: center; margin-top: 20px; }

@media (max-width: 720px) {
  .category-page { padding: 26px 0 60px; }
}
</style>
