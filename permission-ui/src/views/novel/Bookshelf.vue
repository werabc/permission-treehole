<template>
  <div class="shelf-page">
    <div class="shelf-header">
      <h2 class="page-title">我的书架</h2>
    </div>

    <el-tabs v-model="activeTab" class="shelf-tabs">
      <el-tab-pane label="我的书架" name="bookshelf">
        <div class="book-grid" v-loading="loading">
          <div
            class="book-card"
            v-for="item in bookshelf"
            :key="item.id"
            @click="$router.push(`/novel/${item.novelId}`)"
          >
            <div class="book-cover">
              <el-image v-if="item.coverUrl" :src="item.coverUrl" fit="cover">
                <template #error>
                  <div class="cover-placeholder"><el-icon :size="28"><Notebook /></el-icon></div>
                </template>
              </el-image>
              <div v-else class="cover-placeholder"><el-icon :size="28"><Notebook /></el-icon></div>
            </div>
            <div class="book-info">
              <h3 class="book-title">{{ item.novelTitle }}</h3>
              <p class="book-author">{{ item.authorName }}</p>
              <p class="book-last" v-if="item.lastChapterTitle">最新：{{ item.lastChapterTitle }}</p>
              <el-button
                size="small"
                type="danger"
                link
                @click.stop="handleRemove(item)"
              >移出书架</el-button>
            </div>
          </div>
          <el-empty v-if="!loading && !bookshelf.length" description="书架空空如也，去小说广场看看吧">
            <el-button type="primary" @click="$router.push('/novel')">去逛逛</el-button>
          </el-empty>
        </div>
      </el-tab-pane>

      <el-tab-pane label="阅读历史" name="history">
        <div class="history-list" v-loading="histLoading">
          <div
            class="history-item"
            v-for="item in history"
            :key="item.id"
            @click="$router.push(`/novel/read/${item.novelId}/${item.chapterId}`)"
          >
            <div class="hist-cover">
              <el-image v-if="item.coverUrl" :src="item.coverUrl" fit="cover">
                <template #error>
                  <div class="cover-placeholder-sm"><el-icon :size="20"><Notebook /></el-icon></div>
                </template>
              </el-image>
              <div v-else class="cover-placeholder-sm"><el-icon :size="20"><Notebook /></el-icon></div>
            </div>
            <div class="hist-info">
              <h3 class="hist-title">{{ item.novelTitle }}</h3>
              <p class="hist-chapter">上次看到：{{ item.chapterTitle }}</p>
              <p class="hist-time">{{ item.updateTime?.substring(0, 16) }}</p>
            </div>
            <el-button class="hist-btn" size="small" type="primary" link>继续阅读 →</el-button>
          </div>
          <el-empty v-if="!histLoading && !history.length" description="暂无阅读记录" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Notebook } from '@element-plus/icons-vue'
import { getBookshelf, removeBookshelf, getReadingHistory } from '@/api/novel'
import type { UserBookshelf, ReadingHistory } from '@/types'

const activeTab = ref('bookshelf')
const loading = ref(false)
const histLoading = ref(false)
const bookshelf = ref<UserBookshelf[]>([])
const history = ref<ReadingHistory[]>([])

async function fetchBookshelf() {
  loading.value = true
  try {
    const res = await getBookshelf()
    bookshelf.value = res.data
  } finally {
    loading.value = false
  }
}

async function fetchHistory() {
  histLoading.value = true
  try {
    const res = await getReadingHistory()
    history.value = res.data
  } finally {
    histLoading.value = false
  }
}

async function handleRemove(item: UserBookshelf) {
  await removeBookshelf(item.novelId)
  ElMessage.success('已移出书架')
  fetchBookshelf()
}

onMounted(() => {
  fetchBookshelf()
  fetchHistory()
})
</script>

<style scoped>
.shelf-page {
  margin: -20px;
  min-height: calc(100vh - 60px);
  background: #f5f7fa;
  padding: 24px 32px;
}

.shelf-header {
  margin-bottom: 16px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin: 0;
}

.shelf-tabs {
  background: #fff;
  border-radius: 10px;
  padding: 4px 24px 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

/* Bookshelf grid */
.book-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.book-card {
  display: flex;
  gap: 14px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.book-card:hover {
  background: #f0f2f5;
}

.book-cover {
  width: 72px;
  height: 96px;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(135deg, #e8ecf1, #d5dbe3);
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #b0b8c4;
}

.book-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.book-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-author {
  font-size: 12px;
  color: #909399;
  margin: 0 0 4px;
}

.book-last {
  font-size: 11px;
  color: #c0c4cc;
  margin: 0 0 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* History */
.history-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 0;
  border-bottom: 1px solid #f2f3f5;
  cursor: pointer;
  transition: background 0.15s;
}

.history-item:hover {
  background: #fafafa;
}

.hist-cover {
  width: 48px;
  height: 64px;
  border-radius: 4px;
  overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(135deg, #e8ecf1, #d5dbe3);
}

.cover-placeholder-sm {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #b0b8c4;
}

.hist-info {
  flex: 1;
  min-width: 0;
}

.hist-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px;
}

.hist-chapter {
  font-size: 12px;
  color: #606266;
  margin: 0 0 2px;
}

.hist-time {
  font-size: 11px;
  color: #c0c4cc;
  margin: 0;
}

.hist-btn {
  flex-shrink: 0;
}
</style>
