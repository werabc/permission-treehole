<template>
  <div class="reader-page" :class="{ 'dark-mode': darkMode }">
    <!-- Top bar -->
    <div class="reader-topbar">
      <div class="topbar-left">
        <el-button link @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <span class="topbar-title">{{ novel?.title }}</span>
      </div>
      <div class="topbar-right">
        <el-button link @click="darkMode = !darkMode">
          <el-icon :size="18"><Moon v-if="!darkMode" /><Sunny v-else /></el-icon>
        </el-button>
        <el-select v-model="fontSize" size="small" style="width: 90px">
          <el-option label="小号" :value="16" />
          <el-option label="中号" :value="20" />
          <el-option label="大号" :value="24" />
          <el-option label="特大" :value="28" />
        </el-select>
        <el-button link @click="$router.push(`/novel/${novelId}`)">
          <el-icon :size="18"><List /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- Content -->
    <div class="reader-content" v-loading="loading">
      <template v-if="chapter">
        <h1 class="chapter-title">{{ chapter.chapterTitle }}</h1>
        <div class="chapter-content" :style="{ fontSize: fontSize + 'px' }">
          <p v-for="(p, i) in paragraphs" :key="i">{{ p }}</p>
        </div>
      </template>
      <el-empty v-else description="章节加载失败" :image-size="80" />
    </div>

    <!-- Bottom nav -->
    <div class="reader-nav" v-if="chapter">
      <el-button
        :disabled="!nav.prev"
        @click="goChapter(nav.prev!.id)"
        size="large"
      >
        <el-icon><ArrowLeft /></el-icon>
        {{ nav.prev ? nav.prev.chapterTitle : '已是第一章' }}
      </el-button>

      <span class="nav-progress">{{ chapter.chapterNum }} / {{ totalChapters }}</span>

      <el-button
        :disabled="!nav.next"
        @click="goChapter(nav.next!.id)"
        size="large"
      >
        {{ nav.next ? nav.next.chapterTitle : '已是最后一章' }}
        <el-icon><ArrowRight /></el-icon>
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, Moon, Sunny, List } from '@element-plus/icons-vue'
import { getNovelDetail, getChaptersByNovel, getChapterDetail, getChapterNav, saveReadingHistory } from '@/api/novel'
import type { Novel, NovelChapter, ChapterNav } from '@/types'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const novel = ref<Novel>()
const chapter = ref<NovelChapter>()
const nav = ref<ChapterNav>({ prev: null, next: null })
const chapters = ref<NovelChapter[]>([])
const fontSize = ref(20)
const darkMode = ref(false)

const novelId = computed(() => Number(route.params.novelId))
const chapterId = computed(() => Number(route.params.chapterId))

const paragraphs = computed(() => {
  if (!chapter.value?.content) return []
  return chapter.value.content.split('\n').filter(p => p.trim())
})

const totalChapters = computed(() => chapters.value.length)

async function fetchChapter() {
  loading.value = true
  try {
    const [detailRes, navRes] = await Promise.all([
      getChapterDetail(chapterId.value),
      getChapterNav(chapterId.value),
    ])
    chapter.value = detailRes.data
    nav.value = navRes.data
  } finally {
    loading.value = false
  }
}

async function loadNovelAndChapters() {
  const [novelRes, chaptersRes] = await Promise.all([
    getNovelDetail(novelId.value),
    getChaptersByNovel(novelId.value),
  ])
  novel.value = novelRes.data
  chapters.value = chaptersRes.data
}

function goChapter(id: number) {
  if (!id) return
  router.replace(`/novel/read/${novelId.value}/${id}`)
}

function saveProgress() {
  if (!chapter.value) return
  const token = localStorage.getItem('accessToken')
  if (!token) return
  saveReadingHistory({
    novelId: novelId.value,
    chapterId: chapter.value.id,
    chapterTitle: chapter.value.chapterTitle,
  }).catch(() => {})
}

// Keyboard navigation
function onKeydown(e: KeyboardEvent) {
  if (e.key === 'ArrowLeft' || e.key === 'a') {
    if (nav.value.prev) goChapter(nav.value.prev.id)
  } else if (e.key === 'ArrowRight' || e.key === 'd') {
    if (nav.value.next) goChapter(nav.value.next.id)
  }
}

watch(chapterId, () => {
  fetchChapter()
  window.scrollTo({ top: 0, behavior: 'smooth' })
})

onMounted(async () => {
  await Promise.all([loadNovelAndChapters(), fetchChapter()])
  saveProgress()
  document.addEventListener('keydown', onKeydown)

  // Restore preferences
  const savedSize = localStorage.getItem('reader-font-size')
  if (savedSize) fontSize.value = Number(savedSize)
  const savedDark = localStorage.getItem('reader-dark-mode')
  if (savedDark) darkMode.value = savedDark === 'true'
})

// Save preferences on change
watch(fontSize, v => localStorage.setItem('reader-font-size', String(v)))
watch(darkMode, v => localStorage.setItem('reader-dark-mode', String(v)))
</script>

<style scoped>
.reader-page {
  margin: -20px;
  min-height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
  background: #fafafa;
  transition: background 0.3s;
}

.reader-page.dark-mode {
  background: #1a1a2e;
  color: #d0d0d0;
}

/* Top bar */
.reader-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  height: 50px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  position: sticky;
  top: 0;
  z-index: 10;
}

.dark-mode .reader-topbar {
  background: #16213e;
  border-bottom-color: #2a3a5e;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.topbar-title {
  font-size: 14px;
  font-weight: 500;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* Content */
.reader-content {
  flex: 1;
  max-width: 720px;
  width: 100%;
  margin: 0 auto;
  padding: 40px 24px;
}

.chapter-title {
  font-size: 26px;
  font-weight: 700;
  text-align: center;
  margin: 0 0 36px;
  color: #303133;
}

.dark-mode .chapter-title {
  color: #e8e8e8;
}

.chapter-content {
  line-height: 2;
  color: #303133;
}

.dark-mode .chapter-content {
  color: #c8c8c8;
}

.chapter-content p {
  text-indent: 2em;
  margin: 0 0 8px;
}

/* Bottom nav */
.reader-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #fff;
  border-top: 1px solid #ebeef5;
  position: sticky;
  bottom: 0;
}

.dark-mode .reader-nav {
  background: #16213e;
  border-top-color: #2a3a5e;
}

.reader-nav .el-button {
  max-width: 40%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nav-progress {
  font-size: 13px;
  color: #909399;
  flex-shrink: 0;
}

.dark-mode .nav-progress {
  color: #6a7a9e;
}
</style>
