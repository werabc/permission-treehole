<template>
  <div class="novel-detail-page" v-loading="loading">
    <div class="detail-container" v-if="novel">
      <!-- Header -->
      <div class="detail-header">
        <div class="cover-col">
          <div class="detail-cover">
            <el-image v-if="novel.coverUrl" :src="novel.coverUrl" fit="cover">
              <template #error>
                <div class="cover-placeholder"><el-icon :size="48"><Notebook /></el-icon></div>
              </template>
            </el-image>
            <div v-else class="cover-placeholder"><el-icon :size="48"><Notebook /></el-icon></div>
          </div>
        </div>
        <div class="info-col">
          <h1 class="novel-title">{{ novel.title }}</h1>
          <div class="novel-meta">
            <span><el-icon><User /></el-icon> {{ novel.authorName }}</span>
            <span v-if="novel.categoryName"><el-icon><Collection /></el-icon> {{ novel.categoryName }}</span>
            <span :class="novel.status === 2 ? 'finished' : 'ongoing'">
              {{ novel.status === 2 ? '已完结' : '连载中' }}
            </span>
          </div>
          <div class="novel-stats">
            <div class="stat-item">
              <span class="stat-val">{{ formatCount(novel.wordCount) }}</span>
              <span class="stat-label">字数</span>
            </div>
            <div class="stat-item">
              <span class="stat-val">{{ formatCount(novel.clickCount) }}</span>
              <span class="stat-label">点击</span>
            </div>
            <div class="stat-item">
              <span class="stat-val">{{ formatCount(novel.likeCount) }}</span>
              <span class="stat-label">收藏</span>
            </div>
          </div>
          <div class="header-actions">
            <el-button
              type="primary"
              size="large"
              :icon="Reading"
              @click="startRead"
              :disabled="!chapters.length"
            >开始阅读</el-button>
            <el-button
              size="large"
              :type="inBookshelf ? 'success' : 'default'"
              :icon="inBookshelf ? StarFilled : Star"
              :loading="shelfLoading"
              @click="toggleBookshelf"
            >{{ inBookshelf ? '已收藏' : '加入书架' }}</el-button>
          </div>
        </div>
      </div>

      <!-- Intro -->
      <div class="detail-section">
        <h3 class="section-title">作品简介</h3>
        <p class="intro-text">{{ novel.intro }}</p>
      </div>

      <!-- Chapter List -->
      <div class="detail-section">
        <h3 class="section-title">
          章节目录
          <span class="chapter-count">共 {{ chapters.length }} 章</span>
        </h3>
        <div class="chapter-list">
          <div
            v-for="ch in chapters"
            :key="ch.id"
            class="chapter-item"
            @click="$router.push(`/novel/read/${novel.id}/${ch.id}`)"
          >
            <span class="ch-title">{{ ch.chapterTitle }}</span>
            <span class="ch-meta">{{ ch.wordCount }} 字 · {{ ch.createTime?.substring(0, 10) }}</span>
          </div>
          <el-empty v-if="!chapters.length" description="暂无章节" :image-size="60" />
        </div>
      </div>

      <!-- Comments -->
      <div class="detail-section">
        <h3 class="section-title">评论区</h3>
        <div class="comment-input" v-if="userStore.userInfo">
          <el-input
            v-model="commentText"
            type="textarea"
            :rows="3"
            placeholder="写下你的评论..."
            maxlength="500"
            show-word-limit
          />
          <el-button
            type="primary"
            :loading="commentLoading"
            :disabled="!commentText.trim()"
            @click="submitComment"
            style="margin-top: 10px"
          >发表评论</el-button>
        </div>
        <div v-else class="comment-login-hint">
          <el-button type="primary" @click="$router.push('/login')">登录后发表评论</el-button>
        </div>

        <div class="comment-list" v-if="comments.length">
          <div
            v-for="c in comments"
            :key="c.id"
            class="comment-card"
          >
            <div class="comment-main">
              <span class="comment-user">{{ c.userName }}</span>
              <span class="comment-time">{{ c.createTime?.substring(0, 16) }}</span>
              <p class="comment-content">{{ c.content }}</p>
              <div class="comment-actions">
                <el-button link size="small" @click="replyTo = c.id; commentText = `@${c.userName} `">
                  回复
                </el-button>
              </div>
            </div>
            <!-- Replies -->
            <div class="comment-replies" v-if="c.children && c.children.length">
              <div v-for="r in c.children" :key="r.id" class="reply-item">
                <span class="reply-user">{{ r.userName }}</span>
                <span v-if="r.parentId" class="reply-to">回复 @{{ c.userName }}</span>
                <span>：{{ r.content }}</span>
                <span class="reply-time">{{ r.createTime?.substring(0, 16) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Notebook, User, Collection, Reading, Star, StarFilled } from '@element-plus/icons-vue'
import { getNovelDetail, getChaptersByNovel } from '@/api/novel'
import {
  addBookshelf, removeBookshelf, checkBookshelf,
  getComments, addComment,
} from '@/api/novel'
import { useUserStore } from '@/stores/user'
import type { Novel, NovelChapter, NovelComment } from '@/types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const novel = ref<Novel>()
const chapters = ref<NovelChapter[]>([])
const comments = ref<NovelComment[]>([])
const inBookshelf = ref(false)
const shelfLoading = ref(false)
const commentText = ref('')
const commentLoading = ref(false)
const replyTo = ref(0)

const novelId = computed(() => Number(route.params.id))

async function fetchDetail() {
  loading.value = true
  try {
    const [novelRes, chapterRes, commentRes] = await Promise.all([
      getNovelDetail(novelId.value),
      getChaptersByNovel(novelId.value),
      getComments(novelId.value),
    ])
    novel.value = novelRes.data
    chapters.value = chapterRes.data
    comments.value = commentRes.data
  } finally {
    loading.value = false
  }
}

async function checkShelf() {
  if (!userStore.userInfo) return
  const res = await checkBookshelf(novelId.value)
  inBookshelf.value = res.data
}

async function toggleBookshelf() {
  if (!userStore.userInfo) {
    router.push('/login')
    return
  }
  shelfLoading.value = true
  try {
    if (inBookshelf.value) {
      await removeBookshelf(novelId.value)
      inBookshelf.value = false
      ElMessage.success('已移出书架')
    } else {
      await addBookshelf(novelId.value)
      inBookshelf.value = true
      ElMessage.success('已加入书架')
    }
  } finally {
    shelfLoading.value = false
  }
}

function startRead() {
  if (chapters.value.length) {
    router.push(`/novel/read/${novelId.value}/${chapters.value[0].id}`)
  }
}

async function submitComment() {
  if (!commentText.value.trim()) return
  commentLoading.value = true
  try {
    await addComment({
      novelId: novelId.value,
      content: commentText.value,
      parentId: replyTo.value || undefined,
    })
    ElMessage.success('评论发表成功')
    commentText.value = ''
    replyTo.value = 0
    const res = await getComments(novelId.value)
    comments.value = res.data
  } finally {
    commentLoading.value = false
  }
}

function formatCount(n: number) {
  if (!n) return '0'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return String(n)
}

onMounted(() => {
  fetchDetail()
  checkShelf()
})
</script>

<style scoped>
.novel-detail-page {
  margin: -20px;
  min-height: calc(100vh - 60px);
  background: #f5f7fa;
}

.detail-container {
  max-width: 960px;
  margin: 0 auto;
  padding: 32px 24px;
}

/* Header */
.detail-header {
  display: flex;
  gap: 28px;
  background: #fff;
  border-radius: 12px;
  padding: 28px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  margin-bottom: 20px;
}

.cover-col {
  flex-shrink: 0;
}

.detail-cover {
  width: 160px;
  height: 210px;
  border-radius: 8px;
  overflow: hidden;
  background: linear-gradient(135deg, #e8ecf1, #d5dbe3);
}

.detail-cover .el-image {
  width: 100%;
  height: 100%;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #b0b8c4;
}

.info-col {
  flex: 1;
  min-width: 0;
}

.novel-title {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 12px;
}

.novel-meta {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #606266;
  margin-bottom: 16px;
}

.novel-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.novel-meta .finished { color: #909399; }
.novel-meta .ongoing { color: #409eff; }

.novel-stats {
  display: flex;
  gap: 32px;
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
}

.stat-val {
  display: block;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.header-actions {
  display: flex;
  gap: 12px;
}

/* Sections */
.detail-section {
  background: #fff;
  border-radius: 12px;
  padding: 24px 28px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  margin-bottom: 20px;
}

.section-title {
  font-size: 17px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.chapter-count {
  font-size: 13px;
  font-weight: 400;
  color: #909399;
  margin-left: 8px;
}

.intro-text {
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
}

/* Chapter list */
.chapter-list {
  max-height: 500px;
  overflow-y: auto;
}

.chapter-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f2f3f5;
  cursor: pointer;
  transition: color 0.2s;
}

.chapter-item:hover {
  color: #409eff;
}

.ch-title {
  font-size: 14px;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ch-meta {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
  margin-left: 16px;
}

/* Comments */
.comment-input { margin-bottom: 24px; }

.comment-login-hint {
  text-align: center;
  padding: 24px;
}

.comment-card {
  padding: 16px 0;
  border-bottom: 1px solid #f2f3f5;
}

.comment-user {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-right: 10px;
}

.comment-time {
  font-size: 12px;
  color: #c0c4cc;
}

.comment-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 8px 0;
}

.comment-actions {
  display: flex;
  gap: 12px;
}

.comment-replies {
  margin-top: 10px;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.reply-item {
  font-size: 13px;
  color: #606266;
  padding: 6px 0;
  line-height: 1.5;
}

.reply-user {
  font-weight: 600;
  color: #409eff;
}

.reply-to {
  color: #909399;
}

.reply-time {
  font-size: 12px;
  color: #c0c4cc;
  margin-left: 8px;
}
</style>
