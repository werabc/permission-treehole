<template>
  <div class="novel-list-page">
    <!-- Hero Banner -->
    <div class="hero-banner">
      <div class="hero-content">
        <h1 class="hero-title">小说广场</h1>
        <p class="hero-subtitle">海量精品小说，随时随地畅快阅读</p>
        <div class="hero-search">
          <el-input
            v-model="keyword"
            placeholder="搜索书名、作者..."
            size="large"
            clearable
            @keyup.enter="doSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" size="large" :icon="Search" @click="doSearch">搜索</el-button>
        </div>
      </div>
    </div>

    <!-- Category Tabs -->
    <div class="category-bar">
      <div class="category-tabs">
        <span
          class="cat-item"
          :class="{ active: !activeCategory }"
          @click="activeCategory = undefined; fetchData()"
        >全部分类</span>
        <span
          v-for="cat in categories"
          :key="cat.id"
          class="cat-item"
          :class="{ active: activeCategory === cat.id }"
          @click="activeCategory = cat.id; fetchData()"
        >{{ cat.categoryName }}</span>
      </div>
    </div>

    <!-- Novel Grid -->
    <div class="novel-grid-wrap" v-loading="loading">
      <div class="novel-grid" v-if="novels.length > 0">
        <div
          class="novel-card"
          v-for="item in novels"
          :key="item.id"
          @click="$router.push(`/novel/${item.id}`)"
        >
          <div class="card-cover">
            <el-image
              v-if="item.coverUrl"
              :src="item.coverUrl"
              fit="cover"
              class="cover-img"
            >
              <template #error>
                <div class="cover-placeholder">
                  <el-icon :size="36"><Notebook /></el-icon>
                </div>
              </template>
            </el-image>
            <div v-else class="cover-placeholder">
              <el-icon :size="36"><Notebook /></el-icon>
            </div>
            <span class="status-tag" :class="item.status === 2 ? 'finished' : 'ongoing'">
              {{ item.status === 2 ? '已完结' : '连载中' }}
            </span>
          </div>
          <div class="card-body">
            <h3 class="card-title">{{ item.title }}</h3>
            <div class="card-meta">
              <span class="meta-item">
                <el-icon :size="14"><User /></el-icon>
                {{ item.authorName }}
              </span>
              <span class="meta-item" v-if="item.categoryName">
                <el-icon :size="14"><Collection /></el-icon>
                {{ item.categoryName }}
              </span>
            </div>
            <div class="card-stats">
              <span>{{ formatWordCount(item.wordCount) }} 字</span>
              <span class="divider">|</span>
              <span><el-icon :size="14"><Star /></el-icon> {{ formatCount(item.likeCount) }}</span>
              <span class="divider">|</span>
              <span><el-icon :size="14"><View /></el-icon> {{ formatCount(item.clickCount) }}</span>
            </div>
            <p class="card-last" v-if="item.lastChapterTitle">
              最新：{{ item.lastChapterTitle }}
            </p>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无小说" :image-size="120" />

      <div class="pagination-wrap" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[12, 24, 36]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search, Notebook, User, Collection, Star, View } from '@element-plus/icons-vue'
import { getPublishedNovels, getCategoryList } from '@/api/novel'
import type { Novel, NovelCategory } from '@/types'

const loading = ref(false)
const novels = ref<Novel[]>([])
const categories = ref<NovelCategory[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(12)
const keyword = ref('')
const activeCategory = ref<number>()

async function fetchData() {
  loading.value = true
  try {
    const res = await getPublishedNovels({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      categoryId: activeCategory.value,
    })
    novels.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  const res = await getCategoryList()
  categories.value = res.data
}

function doSearch() {
  pageNum.value = 1
  fetchData()
}

function formatWordCount(n: number) {
  if (!n) return '0'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  if (n >= 1000) return (n / 1000).toFixed(1) + '千'
  return String(n)
}

function formatCount(n: number) {
  if (!n) return '0'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return String(n)
}

onMounted(() => {
  loadCategories()
  fetchData()
})
</script>

<style scoped>
.novel-list-page {
  margin: -20px;
  min-height: calc(100vh - 60px);
  background: #f5f7fa;
}

/* Hero */
.hero-banner {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 40%, #0f3460 100%);
  padding: 48px 40px 40px;
  text-align: center;
}

.hero-title {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 8px;
  letter-spacing: 2px;
}

.hero-subtitle {
  font-size: 15px;
  color: rgba(255,255,255,0.65);
  margin: 0 0 28px;
}

.hero-search {
  max-width: 560px;
  margin: 0 auto;
  display: flex;
  gap: 10px;
}

.hero-search :deep(.el-input__wrapper) {
  background: rgba(255,255,255,0.12);
  border: 1px solid rgba(255,255,255,0.2);
  box-shadow: none;
}

.hero-search :deep(.el-input__inner) {
  color: #fff;
}

.hero-search :deep(.el-input__inner::placeholder) {
  color: rgba(255,255,255,0.45);
}

/* Category */
.category-bar {
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  padding: 0 40px;
  overflow-x: auto;
}

.category-tabs {
  display: flex;
  gap: 0;
  white-space: nowrap;
}

.cat-item {
  display: inline-block;
  padding: 14px 20px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
  user-select: none;
}

.cat-item:hover {
  color: #0f3460;
}

.cat-item.active {
  color: #0f3460;
  border-bottom-color: #0f3460;
  font-weight: 600;
}

/* Grid */
.novel-grid-wrap {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 40px;
}

.novel-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

@media (max-width: 1100px) {
  .novel-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 768px) {
  .novel-grid { grid-template-columns: repeat(2, 1fr); }
}

/* Card */
.novel-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

.novel-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.1);
}

.card-cover {
  position: relative;
  height: 180px;
  background: linear-gradient(135deg, #e8ecf1, #d5dbe3);
  overflow: hidden;
}

.cover-img {
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

.status-tag {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  color: #fff;
}

.status-tag.ongoing { background: rgba(64, 158, 255, 0.85); }
.status-tag.finished { background: rgba(144, 147, 153, 0.85); }

.card-body {
  padding: 14px 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 3px;
}

.card-stats {
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
}

.card-stats .divider {
  margin: 0 2px;
  color: #dcdfe6;
}

.card-last {
  font-size: 12px;
  color: #606266;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding-bottom: 24px;
}
</style>
