<template>
  <div class="detail" v-if="post">
    <div class="post-card">
      <div class="post-header">
        <span class="th-tag th-tag-blue">{{ post.categoryName || '树洞' }}</span>
        <span class="post-time">{{ formatTime(post.createTime) }}</span>
      </div>
      <p class="post-content">{{ post.content }}</p>
      <div class="post-actions">
        <button :class="['action-btn', { liked }]" @click="handleLike">
          👍 {{ post.likeCount }}
        </button>
        <span class="action">👁 {{ post.viewCount }}</span>
      </div>
    </div>

    <div class="comment-section">
      <h3>评论 ({{ commentTotal }})</h3>

      <div class="comment-input">
        <textarea v-model="commentContent" placeholder="写下你的评论..." rows="3"></textarea>
        <button class="th-btn th-btn-primary" @click="handleComment">发表评论</button>
      </div>

      <div class="comment-list">
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <p class="comment-content">{{ comment.content }}</p>
          <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
        </div>
      </div>
    </div>
  </div>
  <div v-else class="loading">加载中...</div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getPostDetail, likePost, unlikePost, getCommentPage, createComment } from '../api/treehole'
import type { Post, Comment } from '../api/treehole'

const route = useRoute()
const post = ref<Post | null>(null)
const comments = ref<Comment[]>([])
const commentTotal = ref(0)
const commentContent = ref('')
const liked = ref(false)

async function fetchPost() {
  const id = Number(route.params.id)
  const res = await getPostDetail(id)
  post.value = res.data
}

async function fetchComments() {
  const id = Number(route.params.id)
  const res = await getCommentPage({ pageNum: 1, pageSize: 50, postId: id })
  comments.value = res.data.records
  commentTotal.value = res.data.total
}

async function handleLike() {
  if (!post.value) return
  if (liked.value) {
    await unlikePost(post.value.id)
    post.value.likeCount--
    liked.value = false
  } else {
    await likePost(post.value.id)
    post.value.likeCount++
    liked.value = true
  }
}

async function handleComment() {
  if (!commentContent.value.trim() || !post.value) return
  await createComment({ postId: post.value.id, content: commentContent.value })
  commentContent.value = ''
  fetchComments()
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
  fetchPost()
  fetchComments()
})
</script>

<style scoped>
.detail {
  max-width: 700px;
  margin: 0 auto;
}

.post-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.post-time {
  font-size: 12px;
  color: #94a3b8;
}

.post-content {
  font-size: 16px;
  color: #1e293b;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
  margin-bottom: 16px;
}

.post-actions {
  display: flex;
  gap: 16px;
  align-items: center;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: 20px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 13px;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #e2e8f0;
}

.action-btn.liked {
  background: #dbeafe;
  color: #3b82f6;
}

.action {
  font-size: 13px;
  color: #64748b;
}

.comment-section {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.comment-section h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.comment-input textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  margin-bottom: 10px;
  resize: vertical;
}

.comment-input button {
  float: right;
}

.comment-list {
  margin-top: 60px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-item {
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
}

.comment-content {
  font-size: 14px;
  color: #334155;
  margin-bottom: 4px;
  white-space: pre-wrap;
}

.comment-time {
  font-size: 12px;
  color: #94a3b8;
}

.loading {
  text-align: center;
  padding: 60px;
  color: #94a3b8;
}
</style>
