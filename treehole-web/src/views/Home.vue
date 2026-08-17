<template>
  <div class="home">
    <div class="category-bar">
      <button
        :class="['cat-btn', { active: !selectedCategory }]"
        @click="selectedCategory = undefined; fetchPosts()"
      >
        全部
      </button>
      <button
        v-for="cat in categories"
        :key="cat.id"
        :class="['cat-btn', { active: selectedCategory === cat.id }]"
        @click="selectedCategory = cat.id; fetchPosts()"
      >
        {{ cat.name }}
      </button>
    </div>

    <div class="post-list">
      <div v-for="post in posts" :key="post.id" class="post-card" @click="goDetail(post.id)">
        <div class="post-header">
          <span class="post-category th-tag th-tag-blue">{{ post.categoryName || '树洞' }}</span>
          <span class="post-author-name">{{ post.authorName || '匿名' }}</span>
          <span class="post-time">{{ formatTime(post.createTime) }}</span>
        </div>
        <p class="post-content">{{ post.content }}</p>
        <div class="post-actions">
          <span class="action">👁 {{ post.viewCount }}</span>
          <span class="action">👍 {{ post.likeCount }}</span>
          <span class="action">💬 {{ post.commentCount }}</span>
        </div>
      </div>

      <div v-if="loading" class="loading">加载中...</div>
      <div v-if="!loading && posts.length === 0" class="empty">暂无内容，来发布第一条吧！</div>
    </div>

    <div v-if="hasMore" class="load-more">
      <button class="th-btn th-btn-ghost" @click="loadMore">加载更多</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getPostPage, getCategoryList } from '../api/treehole'
import type { Post, Category } from '../api/treehole'

const router = useRouter()
const posts = ref<Post[]>([])
const categories = ref<Category[]>([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const selectedCategory = ref<number | undefined>(undefined)

const hasMore = ref(false)

async function fetchPosts() {
  loading.value = true
  pageNum.value = 1
  try {
    const res = await getPostPage({ pageNum: 1, pageSize: pageSize, categoryId: selectedCategory.value })
    posts.value = res.data.records
    total.value = res.data.total
    hasMore.value = posts.value.length < total.value
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  loading.value = true
  pageNum.value++
  try {
    const res = await getPostPage({ pageNum: pageNum.value, pageSize: pageSize, categoryId: selectedCategory.value })
    posts.value = [...posts.value, ...res.data.records]
    hasMore.value = posts.value.length < total.value
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  const res = await getCategoryList()
  categories.value = res.data
}

function goDetail(id: number) {
  router.push(`/post/${id}`)
}

function formatTime(time: string) {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  return date.toLocaleDateString()
}

onMounted(() => {
  loadCategories()
  fetchPosts()
})
</script>

<style scoped>
.home {
  max-width: 800px;
  margin: 0 auto;
}

.category-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.cat-btn {
  padding: 6px 16px;
  border-radius: 20px;
  background: #fff;
  color: #64748b;
  font-size: 13px;
  border: 1px solid #e2e8f0;
  transition: all 0.2s;
}

.cat-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.cat-btn.active {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  border-color: transparent;
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
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.post-author-name {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.post-time {
  font-size: 12px;
  color: #94a3b8;
}

.post-content {
  font-size: 15px;
  color: #334155;
  line-height: 1.7;
  margin-bottom: 12px;
  white-space: pre-wrap;
  word-break: break-word;
}

.post-actions {
  display: flex;
  gap: 16px;
}

.action {
  font-size: 13px;
  color: #64748b;
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
