<template>
  <div class="category-page">
    <h2 class="page-title">{{ categoryName }}</h2>
    <div class="post-list">
      <div v-for="post in posts" :key="post.id" class="post-card" @click="$router.push(`/post/${post.id}`)">
        <p class="post-content">{{ post.content }}</p>
        <div class="post-meta">
          <span>👍 {{ post.likeCount }}</span>
          <span>💬 {{ post.commentCount }}</span>
          <span class="time">{{ formatTime(post.createTime) }}</span>
        </div>
      </div>
    </div>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-if="!loading && posts.length === 0" class="empty">该分类暂无内容</div>

    <div v-if="hasMore" class="load-more">
      <button class="th-btn th-btn-ghost" @click="loadMore">加载更多</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
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
    categories.value = res.data
    resolveCategoryFromRoute()
  } catch (e) {
    console.error('加载分类失败:', e)
  }
}

function resolveCategoryFromRoute() {
  const code = route.params.code as string
  const cat = categories.value.find(c => c.code === code)
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
    const res = await getPostPage({
      pageNum: 1,
      pageSize: pageSize,
      categoryId: categoryId.value,
    })
    posts.value = res.data.records
    total.value = res.data.total
    hasMore.value = posts.value.length < total.value
  } catch (e) {
    console.error('加载帖子失败:', e)
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  loading.value = true
  pageNum.value++
  try {
    const res = await getPostPage({
      pageNum: pageNum.value,
      pageSize: pageSize,
      categoryId: categoryId.value,
    })
    posts.value = [...posts.value, ...res.data.records]
    hasMore.value = posts.value.length < total.value
  } finally {
    loading.value = false
  }
}

function formatTime(time: string) {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return date.toLocaleDateString()
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
.category-page {
  max-width: 700px;
  margin: 0 auto;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 20px;
  color: #1e293b;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.2s;
}

.post-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.post-content {
  font-size: 15px;
  color: #334155;
  line-height: 1.6;
  margin-bottom: 8px;
  white-space: pre-wrap;
  word-break: break-word;
}

.post-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #94a3b8;
}

.loading, .empty {
  text-align: center;
  padding: 40px;
  color: #94a3b8;
}

.load-more {
  text-align: center;
  margin-top: 20px;
}
</style>
