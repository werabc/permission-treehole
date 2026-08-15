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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getPostPage } from '../api/treehole'
import type { Post } from '../api/treehole'

const route = useRoute()
const posts = ref<Post[]>([])
const loading = ref(false)
const categoryName = ref('')

const categoryMap: Record<string, string> = {
  emotion: '情感树洞',
  life: '生活随笔',
  rant: '匿名吐槽',
  help: '求助问答',
  fun: '趣味分享',
}

async function fetchPosts() {
  loading.value = true
  try {
    const code = route.params.code as string
    categoryName.value = categoryMap[code] || code
    // 这里简化处理，实际应该通过分类ID查询
    const res = await getPostPage({ pageNum: 1, pageSize: 50 })
    posts.value = res.data.records
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

onMounted(fetchPosts)
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
</style>
