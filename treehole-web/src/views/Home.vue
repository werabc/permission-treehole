<template>
  <div class="home">
    <!-- ============ Hero ============ -->
    <section class="hero">
      <div class="dh-wrap hero__grid">
        <div>
          <span class="dh-eyebrow"><i class="dh-dot" /> 匿名 · 安全 · 被听见</span>
          <h1>把心事<br />种进<i>树洞</i>里</h1>
          <p class="hero__sub">说给懂的人听，不必署名。</p>
          <div class="hero__cta">
            <button class="dh-btn dh-btn--primary dh-btn--lg" type="button" @click="goPublish">
              <AppIcon name="spark" :size="16" /> 匿名写一条
            </button>
            <button class="dh-btn dh-btn--ghost dh-btn--lg" type="button" @click="scrollToFeed">
              先逛逛广场
            </button>
          </div>
          <div class="hero__stats">
            <div class="stat">
              <b>{{ total.toLocaleString() }}</b>
              <span>条心事</span>
            </div>
            <div class="stat">
              <b>{{ categories.length }}</b>
              <span>个分类</span>
            </div>
          </div>
        </div>

        <!-- 今日回声：从最新帖子里随机挑一条真实内容 -->
        <div v-if="echoPost" class="dh-card echo" v-reveal>
          <div class="echo__head"><AppIcon name="bell" :size="14" /> 今日回声</div>
          <p>“{{ echoText }}”</p>
          <div class="echo__foot">
            <span>{{ echoPost.isAnonymous === 1 ? '来自 一个匿名的人' : `来自 ${echoPost.authorName || '陌生人'}` }}</span>
            <span class="wave"><i /><i /><i /><i /><i /></span>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ 公告 ============ -->
    <div v-if="announcements.length > 0" class="dh-wrap ticker">
      <div class="ticker__in">
        <span class="ticker__label">公告</span>
        <div class="ticker__viewport">
          <Transition name="tick" mode="out-in">
            <div :key="annIndex" class="ticker__item">
              <b>{{ announcements[annIndex].title }}</b>
              <span>{{ announcements[annIndex].content }}</span>
            </div>
          </Transition>
        </div>
        <span class="ticker__dots">
          <button
            v-for="(ann, i) in announcements"
            :key="ann.id"
            type="button"
            :class="['ticker__dot', { on: i === annIndex }]"
            :aria-label="`公告 ${i + 1}`"
            @click="annIndex = i"
          />
        </span>
      </div>
    </div>

    <!-- ============ 搜索 ============ -->
    <div class="dh-wrap search">
      <label class="search__in">
        <AppIcon name="search" :size="18" class="search__ico" />
        <input
          v-model="keyword"
          type="search"
          placeholder="搜索心事、关键词或某个人的昵称…"
          aria-label="搜索"
          @keyup.enter="goSearch"
        />
        <button class="dh-btn dh-btn--primary dh-btn--sm" type="button" @click="goSearch">搜索</button>
      </label>
    </div>

    <!-- ============ 分类 + 列表 ============ -->
    <div class="dh-wrap">
      <div class="filters">
        <button type="button" :class="['dh-pill', { 'is-on': selectedCategory === undefined }]" @click="pickCategory(undefined)">
          全部
        </button>
        <button
          v-for="cat in categories"
          :key="cat.id"
          type="button"
          :class="['dh-pill', { 'is-on': selectedCategory === cat.id }]"
          @click="pickCategory(cat.id)"
        >
          {{ cat.name }}
        </button>
        <span class="filters__sp">共 {{ total.toLocaleString() }} 条</span>
      </div>

      <div class="feed">
        <template v-if="loading && posts.length === 0">
          <div v-for="n in 3" :key="n" class="dh-skeleton" />
        </template>

        <template v-else>
          <PostCard
            v-for="(post, i) in posts"
            :key="post.id"
            v-reveal="Math.min(i, 6) * 70"
            :post="post"
          />
        </template>

        <div v-if="!loading && posts.length === 0" class="dh-state">
          <p>这里还很安静</p>
          <button class="dh-btn dh-btn--ghost" type="button" style="margin-top: 14px" @click="goPublish">
            来写第一条
          </button>
        </div>
      </div>

      <div v-if="hasMore" class="load-more">
        <button class="dh-btn dh-btn--ghost" type="button" :disabled="loading" @click="loadMore">
          {{ loading ? '加载中…' : '加载更多' }}
        </button>
      </div>
      <div v-else-if="posts.length > 0" class="sentinel">— 到底了 —</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PostCard from '../components/PostCard.vue'
import AppIcon from '../components/AppIcon.vue'
import { getPostPage, getCategoryList, getActiveAnnouncements } from '../api/treehole'
import { isLoggedIn } from '../api/auth'
import type { Post, Category, Announcement } from '../api/treehole'

const router = useRouter()
const posts = ref<Post[]>([])
const categories = ref<Category[]>([])
const announcements = ref<Announcement[]>([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const selectedCategory = ref<number | undefined>(undefined)
const keyword = ref('')
const hasMore = ref(false)
const annIndex = ref(0)

let annTimer: ReturnType<typeof setInterval> | null = null

/** 今日回声：取当前列表里 content 最长的一条，保证内容充实 */
const echoPost = computed(() => {
  if (posts.value.length === 0) return null
  return [...posts.value].sort((a, b) => (b.content?.length || 0) - (a.content?.length || 0))[0]
})

const echoText = computed(() => {
  const text = echoPost.value?.content || ''
  return text.length > 46 ? `${text.slice(0, 46)}…` : text
})

function goPublish() {
  router.push(isLoggedIn() ? '/publish' : '/login')
}

function scrollToFeed() {
  document.querySelector('.filters')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function goSearch() {
  const kw = keyword.value.trim()
  if (!kw) return
  router.push({ path: '/search', query: { keyword: kw } })
}

function pickCategory(id?: number) {
  if (selectedCategory.value === id) return
  selectedCategory.value = id
  fetchPosts()
}

async function fetchPosts() {
  loading.value = true
  pageNum.value = 1
  try {
    const res = await getPostPage({ pageNum: 1, pageSize, categoryId: selectedCategory.value })
    posts.value = res.data.records || []
    total.value = res.data.total || 0
    hasMore.value = posts.value.length < total.value
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  loading.value = true
  pageNum.value += 1
  try {
    const res = await getPostPage({ pageNum: pageNum.value, pageSize, categoryId: selectedCategory.value })
    posts.value = [...posts.value, ...(res.data.records || [])]
    total.value = res.data.total || 0
    hasMore.value = posts.value.length < total.value
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    const res = await getCategoryList()
    categories.value = res.data || []
  } catch {
    /* ignore */
  }
}

async function loadAnnouncements() {
  try {
    const res = await getActiveAnnouncements()
    announcements.value = res.data || []
  } catch {
    /* ignore */
  }
}

onMounted(() => {
  loadCategories()
  fetchPosts()
  loadAnnouncements()
  annTimer = setInterval(() => {
    if (announcements.value.length > 1) {
      annIndex.value = (annIndex.value + 1) % announcements.value.length
    }
  }, 4200)
})

onUnmounted(() => {
  if (annTimer) clearInterval(annTimer)
})
</script>

<style scoped>
/* ---------- Hero ---------- */
.hero { padding: 78px 0 24px; }
.hero__grid { display: grid; grid-template-columns: 1.15fr 0.85fr; gap: 48px; align-items: center; }
.hero h1 {
  margin: 22px 0 0; font-size: clamp(38px, 5.4vw, 62px); line-height: 1.06;
  letter-spacing: -0.035em; font-weight: 700;
}
.hero h1 i { font-style: normal; color: var(--accent); }
.hero__sub {
  margin-top: 20px; font-family: var(--font-display); font-style: italic;
  font-size: clamp(17px, 2vw, 21px); color: var(--text-dim); max-width: 30ch;
}
.hero__cta { margin-top: 34px; display: flex; gap: 12px; flex-wrap: wrap; }
.hero__stats { margin-top: 42px; display: flex; gap: 34px; flex-wrap: wrap; }
.stat b { display: block; font-family: var(--font-mono); font-size: 24px; font-weight: 500; letter-spacing: -0.02em; }
.stat span { font-size: 12px; color: var(--text-mute); letter-spacing: 0.05em; }

/* ---------- 今日回声 ---------- */
.echo { padding: 26px; border-radius: var(--r-xl); }
.echo__head {
  display: flex; align-items: center; gap: 10px; font-size: 12px; color: var(--text-mute);
  letter-spacing: 0.1em; font-family: var(--font-mono); text-transform: uppercase;
}
.echo p { margin: 18px 0 20px; font-family: var(--font-display); font-style: italic; font-size: 19px; line-height: 1.6; }
.echo__foot { display: flex; align-items: center; justify-content: space-between; font-size: 12px; color: var(--text-mute); }
.wave { display: flex; align-items: flex-end; gap: 3px; height: 20px; }
.wave i { width: 3px; border-radius: 3px; background: var(--accent-line); animation: wave 1.3s ease-in-out infinite; }
.wave i:nth-child(1) { height: 8px; }
.wave i:nth-child(2) { height: 16px; animation-delay: 0.12s; }
.wave i:nth-child(3) { height: 11px; animation-delay: 0.24s; }
.wave i:nth-child(4) { height: 19px; animation-delay: 0.36s; }
.wave i:nth-child(5) { height: 7px; animation-delay: 0.48s; }
@keyframes wave { 0%, 100% { transform: scaleY(0.5); } 50% { transform: scaleY(1); } }

/* ---------- 公告 ---------- */
.ticker { margin-top: 30px; }
.ticker__in {
  display: flex; align-items: center; gap: 14px; height: 46px; padding: 0 14px 0 16px;
  border-radius: 999px; border: 1px solid var(--border); background: var(--surface);
  backdrop-filter: blur(16px);
}
.ticker__label {
  font-family: var(--font-mono); font-size: 10px; letter-spacing: 0.16em;
  color: var(--accent); text-transform: uppercase; white-space: nowrap; flex-shrink: 0;
}
.ticker__viewport { flex: 1; min-width: 0; height: 46px; overflow: hidden; }
.ticker__item { display: flex; align-items: center; gap: 10px; height: 46px; font-size: 13px; color: var(--text-dim); }
.ticker__item b { color: var(--text); font-weight: 500; flex-shrink: 0; }
.ticker__item span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tick-enter-active, .tick-leave-active { transition: opacity 0.4s var(--ease), transform 0.4s var(--ease); }
.tick-enter-from { opacity: 0; transform: translateY(100%); }
.tick-leave-to { opacity: 0; transform: translateY(-100%); }
.ticker__dots { display: flex; gap: 5px; flex-shrink: 0; }
.ticker__dot { width: 5px; height: 5px; border-radius: 999px; background: var(--border-strong); transition: all 0.3s var(--ease); }
.ticker__dot.on { background: var(--accent); width: 14px; }

/* ---------- 搜索 ---------- */
.search { margin-top: 30px; }
.search__in {
  display: flex; align-items: center; gap: 12px; padding: 6px 6px 6px 20px;
  border-radius: 999px; border: 1px solid var(--border); background: var(--surface);
  backdrop-filter: blur(18px); transition: border-color 0.35s var(--ease), box-shadow 0.35s var(--ease);
}
.search__in:focus-within { border-color: var(--accent-line); box-shadow: 0 0 0 4px var(--accent-soft), var(--shadow); }
.search__ico { color: var(--text-mute); }
.search__in input { flex: 1; font-size: 15px; padding: 12px 0; min-width: 0; }
.search__in input::placeholder { color: var(--text-mute); }

/* ---------- 分类 / 列表 ---------- */
.filters { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin: 30px 0 20px; }
.filters__sp { margin-left: auto; font-family: var(--font-mono); font-size: 11px; color: var(--text-mute); letter-spacing: 0.08em; }
.feed { display: grid; gap: 14px; }
.load-more { text-align: center; margin-top: 26px; }
.sentinel { text-align: center; padding: 30px; color: var(--text-mute); font-family: var(--font-mono); font-size: 12px; letter-spacing: 0.1em; }

@media (max-width: 980px) {
  .hero__grid { grid-template-columns: 1fr; gap: 34px; }
}
@media (max-width: 720px) {
  .hero { padding: 44px 0 18px; }
  .search__in { padding-left: 14px; }
  .ticker__dots { display: none; }
}
</style>
