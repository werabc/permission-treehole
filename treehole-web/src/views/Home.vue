<template>
  <div class="dh-page">
    <!-- ============ Hero ============ -->
    <section class="dh-page__hero hero">
      <span class="dh-eyebrow"><i class="dh-dot" /> 匿名 · 安全 · 被听见</span>
      <h1>把心事<br />种进<i>树洞</i>里</h1>
      <p class="hero__sub">说给懂的人听，不必署名。</p>

      <label class="hero__search">
        <AppIcon name="search" :size="17" class="hero__search-ic" />
        <input
          ref="searchRef"
          v-model="keyword"
          type="search"
          placeholder="搜索心事、关键词或某个人的昵称…"
          aria-label="搜索"
          @keyup.enter="goSearch"
        />
        <span class="hero__kbd">/</span>
      </label>

      <div class="hero__cta">
        <button class="dh-btn dh-btn--primary dh-btn--lg" type="button" @click="goPublish">
          <span class="dh-btn__lb"><AppIcon name="spark" :size="16" /> 匿名写一条</span>
        </button>
        <button class="dh-btn dh-btn--ghost dh-btn--lg" type="button" @click="scrollToFeed">
          <span class="dh-btn__lb">
            看看今晚的心事
            <AppIcon name="chevron-right" :size="15" class="dh-arw" />
          </span>
        </button>
      </div>

      <div class="hero__stats">
        <div class="stat"><b>{{ total.toLocaleString() }}</b><span>条心事</span></div>
        <div class="stat"><b>{{ categories.length }}</b><span>个分类</span></div>
      </div>
    </section>

    <!-- ============ 右栏 ============ -->
    <aside class="dh-rail">
      <!-- 公告 -->
      <div v-if="announcements.length > 0" class="dh-card dh-mod">
        <div class="dh-mod__h">
          <AppIcon name="mega" :size="15" />
          树洞公告
          <span>{{ announcements.length }} 条</span>
        </div>
        <div
          v-for="(ann, i) in announcements"
          :key="ann.id"
          class="dh-ann"
          :class="{ 'is-on': annOpen === i }"
        >
          <button class="dh-ann__t" type="button" @click="toggleAnn(i)">
            <i class="dh-ann__mk" />
            <span class="dh-ann__tx">{{ ann.title }}</span>
          </button>
          <div class="dh-ann__bd">
            {{ ann.content }}
            <div class="dh-ann__meta">
              <span>{{ ann.type }}</span>
              <span>·</span>
              <span>{{ ann.publishTime || ann.createTime }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 今日回声（取当前列表里内容最长的一条真实帖子） -->
      <div v-if="echoPost" class="dh-card dh-mod echo" @click="goPost(echoPost.id)">
        <div class="dh-mod__h">
          <AppIcon name="moon" :size="15" />
          今日回声
        </div>
        <p class="echo__q">“{{ echoText }}”</p>
        <div class="echo__f">
          <span>来自 {{ echoPost.isAnonymous === 1 ? '一个匿名的人' : echoPost.authorName || '陌生人' }}</span>
          <span class="echo__go">去看看 <AppIcon name="chevron-right" :size="12" /></span>
        </div>
      </div>

      <!-- 数据 -->
      <div class="dh-card dh-mod">
        <div class="dh-mod__h">
          <AppIcon name="cell" :size="15" />
          树洞数据
        </div>
        <div class="dh-statrow">
          <div><b>{{ total.toLocaleString() }}</b><span>条心事</span></div>
          <div><b>{{ categories.length }}</b><span>个分类</span></div>
        </div>
      </div>

      <!-- 公约 -->
      <div class="dh-card dh-mod">
        <div class="dh-mod__h">
          <AppIcon name="shield" :size="15" />
          树洞公约
        </div>
        <ul class="dh-pact">
          <li><span class="no">01</span><span><b>匿名是保护，不是武器。</b>不攻击、不人肉、不泄露隐私。</span></li>
          <li><span class="no">02</span><span><b>不做广告与引流。</b>联系方式、二维码、外链都会被拦下。</span></li>
          <li><span class="no">03</span><span><b>你可以只写一句。</b>不必完整，不必正确，写下来就够。</span></li>
        </ul>
        <p class="dh-trust">本平台不记录你的真实身份</p>
      </div>
    </aside>

    <!-- ============ 精选（置顶帖单独成卡，不再混在流里） ============ -->
    <article v-if="spotPost" class="dh-card dh-card--hover dh-spot" @click="goPost(spotPost.id)">
      <div class="dh-spot__h">
        <span class="dh-tag dh-tag--pin">置顶 · 精选</span>
        <span class="dh-mono dh-spot__time">{{ formatDate(spotPost.createTime) }}</span>
        <span v-if="spotPost.categoryName" class="dh-tag dh-spot__cat">{{ spotPost.categoryName }}</span>
      </div>
      <p class="dh-spot__b">{{ spotPost.content }}</p>
      <div class="dh-spot__f">
        <span class="dh-metric"><AppIcon name="eye" :size="13" /> {{ spotPost.viewCount || 0 }}</span>
        <span class="dh-metric"><AppIcon name="like" :size="13" /> {{ spotPost.likeCount || 0 }}</span>
        <span class="dh-metric"><AppIcon name="comment" :size="13" /> {{ spotPost.commentCount || 0 }}</span>
        <span class="dh-spot__more">读全文 <AppIcon name="chevron-right" :size="12" /></span>
      </div>
    </article>

    <!-- ============ 内容 ============ -->
    <div class="dh-page__content">
      <div class="filters">
        <button
          type="button"
          :class="['dh-pill', { 'is-on': !activeCategory }]"
          @click="pickCategory(undefined)"
        >
          全部 <i>{{ total }}</i>
        </button>
        <button
          v-for="cat in categories"
          :key="cat.id"
          type="button"
          :class="['dh-pill', { 'is-on': activeCategory === cat.id }]"
          @click="pickCategory(cat.id)"
        >
          {{ cat.name }} <i>{{ cat.postCount || 0 }}</i>
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

        <div v-if="!loading && posts.length === 0 && !spotPost" class="dh-state">
          <p>这里还很安静</p>
          <button class="dh-btn dh-btn--ghost" type="button" style="margin-top: 14px" @click="goPublish">
            <span class="dh-btn__lb">来写第一条</span>
          </button>
        </div>
      </div>

      <!-- 触底自动加载：哨兵进入视口就翻页，按钮作兜底 -->
      <div v-if="hasMore" ref="sentinelRef" class="load-more">
        <button class="dh-btn dh-btn--ghost" type="button" :disabled="loading" @click="loadMore">
          <span class="dh-btn__lb">{{ loading ? '加载中…' : '加载更多' }}</span>
        </button>
      </div>
      <div v-else-if="posts.length > 0" class="sentinel-line">— 到底了 —</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PostCard from '../components/PostCard.vue'
import AppIcon from '../components/AppIcon.vue'
import { getPostPage, getCategoryList, getActiveAnnouncements } from '../api/treehole'
import { isLoggedIn } from '../api/auth'
import type { Post, Category, Announcement } from '../api/treehole'

const route = useRoute()
const router = useRouter()

const posts = ref<Post[]>([])
const categories = ref<Category[]>([])
const announcements = ref<Announcement[]>([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = 10
const keyword = ref('')
const hasMore = ref(false)
const annOpen = ref(0)
const spotPost = ref<Post | null>(null)

const searchRef = ref<HTMLInputElement | null>(null)
const sentinelRef = ref<HTMLElement | null>(null)

/** 分类筛选同步在 URL 上：可分享、可后退 */
const activeCategory = computed(() => {
  const raw = route.query.category
  const n = Number(Array.isArray(raw) ? raw[0] : raw)
  return Number.isInteger(n) && n > 0 ? n : undefined
})

/** 今日回声：取内容最长的一条，保证读起来有内容 */
const echoPost = computed(() => {
  const list = [spotPost.value, ...posts.value].filter(Boolean) as Post[]
  if (list.length === 0) return null
  return [...list].sort((a, b) => (b.content?.length || 0) - (a.content?.length || 0))[0]
})
const echoText = computed(() => {
  const text = echoPost.value?.content || ''
  return text.length > 52 ? `${text.slice(0, 52)}…` : text
})

function goPost(id: number) {
  router.push(`/post/${id}`)
}

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
  if (activeCategory.value === id) return
  router.push({ path: '/', query: id ? { category: String(id) } : {} })
}

function toggleAnn(i: number) {
  annOpen.value = annOpen.value === i ? -1 : i
}

function formatDate(value?: string) {
  if (!value) return ''
  const d = new Date(value.replace(' ', 'T'))
  return Number.isNaN(d.getTime()) ? value : d.toLocaleDateString('zh-CN')
}

/** 第一页数据里把置顶帖抽出来做「精选」，并从列表移除，避免同一篇出现两次 */
function splitSpot(list: Post[]) {
  if (pageNum.value !== 1) return { spot: null as Post | null, rest: list }
  const idx = list.findIndex((p) => p.isTop === 1)
  if (idx === -1) return { spot: null as Post | null, rest: list }
  return { spot: list[idx], rest: list.filter((_, i) => i !== idx) }
}

async function fetchPosts() {
  loading.value = true
  pageNum.value = 1
  try {
    const res = await getPostPage({ pageNum: 1, pageSize, categoryId: activeCategory.value })
    const list = res.data.records || []
    total.value = res.data.total || 0
    const { spot, rest } = splitSpot(list)
    spotPost.value = spot
    posts.value = rest
    hasMore.value = posts.value.length + (spot ? 1 : 0) < total.value
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (loading.value || !hasMore.value) return
  loading.value = true
  pageNum.value += 1
  try {
    const res = await getPostPage({ pageNum: pageNum.value, pageSize, categoryId: activeCategory.value })
    const list = res.data.records || []
    total.value = res.data.total || 0
    posts.value = [...posts.value, ...list]
    hasMore.value = posts.value.length + (spotPost.value ? 1 : 0) < total.value
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

/* 触底自动加载 */
let io: IntersectionObserver | null = null
function observeSentinel() {
  if (!sentinelRef.value) return
  if (!io) {
    io = new IntersectionObserver((entries) => {
      if (entries[0]?.isIntersecting) loadMore()
    }, { rootMargin: '240px 0px' })
  }
  io.disconnect()
  io.observe(sentinelRef.value)
}

/* 按 "/" 聚焦搜索 */
function onKeydown(e: KeyboardEvent) {
  if (e.key !== '/' || e.metaKey || e.ctrlKey || e.altKey) return
  const el = document.activeElement
  if (el && /input|textarea/i.test(el.tagName)) return
  e.preventDefault()
  searchRef.value?.focus()
}

watch(activeCategory, () => {
  fetchPosts().then(() => observeSentinel())
})
// 哨兵出现/消失时重新观察
watch(sentinelRef, () => observeSentinel())
watch(hasMore, () => observeSentinel())

onMounted(() => {
  loadCategories()
  loadAnnouncements()
  fetchPosts().then(observeSentinel)
  window.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  io?.disconnect()
  window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
/* ---------- Hero ---------- */
.hero { padding: 62px 0 6px; }
.hero h1 {
  margin: 20px 0 0; font-size: clamp(34px, 4.4vw, 56px); line-height: 1.06;
  letter-spacing: -0.038em; font-weight: 700;
}
.hero h1 i { font-style: normal; color: var(--accent); position: relative; }
.hero h1 i::after {
  content: ""; position: absolute; left: 0; right: -2px; bottom: 0.06em; height: 0.16em;
  border-radius: 4px; background: color-mix(in srgb, var(--accent) 24%, transparent);
}
.hero__sub {
  margin-top: 18px; font-family: var(--font-display); font-style: italic;
  font-size: clamp(16px, 1.8vw, 20px); color: var(--text-dim); max-width: 28ch;
}
.hero__search {
  display: flex; align-items: center; gap: 11px; margin-top: 26px; max-width: 470px;
  padding: 6px 6px 6px 18px; border-radius: 999px; border: 1px solid var(--border);
  background: var(--surface); backdrop-filter: blur(18px);
  transition: border-color 0.35s var(--ease), box-shadow 0.35s var(--ease);
}
.hero__search:focus-within { border-color: var(--accent-line); box-shadow: 0 0 0 4px var(--accent-soft), var(--shadow); }
.hero__search-ic { color: var(--text-mute); }
.hero__search input { flex: 1; font-size: 14.5px; padding: 10px 0; min-width: 0; }
.hero__search input::placeholder { color: var(--text-mute); }
.hero__kbd {
  font-family: var(--font-mono); font-size: 10.5px; color: var(--text-mute);
  border: 1px solid var(--border); border-radius: 6px; padding: 2px 7px; margin-right: 6px;
}
.hero__cta { margin-top: 22px; display: flex; gap: 12px; flex-wrap: wrap; }
.hero__stats { margin-top: 34px; display: flex; gap: 34px; flex-wrap: wrap; }
.stat b { display: block; font-family: var(--font-mono); font-size: 24px; font-weight: 500; letter-spacing: -0.02em; }
.stat span { font-size: 12px; color: var(--text-mute); letter-spacing: 0.05em; }

/* ---------- 右栏模块 ---------- */
.echo { cursor: pointer; }
.echo__q { font-family: var(--font-display); font-style: italic; font-size: 16.5px; line-height: 1.62; color: var(--text-dim); }
.echo__f { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-top: 14px; font-size: 11.5px; color: var(--text-mute); }
.echo__go { display: inline-flex; align-items: center; gap: 3px; color: var(--accent); transition: gap 0.3s var(--ease); }
.echo:hover .echo__go { gap: 7px; }

/* ---------- 精选 ---------- */
.dh-spot__time { font-size: 11.5px; color: var(--text-mute); }
.dh-spot__cat { margin-left: auto; }
.dh-spot__f { display: flex; align-items: center; gap: 16px; margin-top: 16px; font-size: 12px; color: var(--text-mute); flex-wrap: wrap; }
.dh-spot__more { margin-left: auto; display: inline-flex; align-items: center; gap: 4px; color: var(--accent); font-size: 12.5px; }

/* ---------- 筛选 / 列表 ---------- */
.filters { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.filters .dh-pill i { font-style: normal; font-family: var(--font-mono); font-size: 10.5px; color: var(--text-mute); }
.filters .dh-pill.is-on i { color: var(--accent); }
.filters__sp { margin-left: auto; font-family: var(--font-mono); font-size: 11px; color: var(--text-mute); letter-spacing: 0.08em; }
.feed { display: grid; gap: 14px; margin-top: 18px; }
.load-more { display: flex; justify-content: center; margin-top: 24px; }
.sentinel-line { text-align: center; padding: 26px; color: var(--text-mute); font-family: var(--font-mono); font-size: 11.5px; letter-spacing: 0.1em; }

@media (max-width: 720px) {
  .hero { padding: 36px 0 4px; }
  .hero__search { padding-left: 14px; }
  .hero__kbd { display: none; }
}
</style>
