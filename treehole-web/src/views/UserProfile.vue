<template>
  <div class="dh-narrow user-page">
    <div v-if="loading" class="loading-wrap">
      <div class="dh-skeleton" style="height: 150px" />
    </div>

    <div v-else-if="!profile" class="dh-state">用户不存在</div>

    <template v-else>
      <section class="dh-card head" v-reveal>
        <div class="head__av">{{ avatarText }}</div>
        <div class="head__info">
          <h2>{{ profile.nickname }}</h2>
          <p>{{ profile.bio || '这个人很懒，什么都没留下' }}</p>
          <div class="head__tags">
            <span class="dh-tag">发布 {{ profile.postCount || 0 }} 条心事</span>
            <span class="dh-tag">写下 {{ profile.commentCount || 0 }} 条回响</span>
            <span class="dh-tag">加入于 {{ formatDate(profile.createTime) }}</span>
          </div>
        </div>
      </section>

      <div class="dh-section-title">
        <h3>TA 的心事</h3>
        <span>{{ posts.length }} POSTS</span>
      </div>

      <div v-if="postsLoading" class="feed">
        <div class="dh-skeleton" />
      </div>
      <div v-else-if="posts.length === 0" class="dh-state">还没有发布过心事</div>
      <div v-else class="feed">
        <PostCard v-for="(post, i) in posts" :key="post.id" v-reveal="Math.min(i, 6) * 60" :post="post" />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import PostCard from '../components/PostCard.vue'
import { getPublicProfile, getUserPosts, type Post, type PublicProfile } from '../api/treehole'

const route = useRoute()

const profile = ref<PublicProfile | null>(null)
const posts = ref<Post[]>([])
const loading = ref(false)
const postsLoading = ref(false)

const avatarText = computed(() => (profile.value?.nickname || '?').slice(0, 1).toUpperCase())

async function load(userId: number) {
  if (!Number.isInteger(userId) || userId <= 0) {
    profile.value = null
    loading.value = false
    postsLoading.value = false
    return
  }
  loading.value = true
  postsLoading.value = true
  try {
    const [profileRes, postsRes] = await Promise.all([
      getPublicProfile(userId),
      getUserPosts(userId, { pageNum: 1, pageSize: 20 }),
    ])
    profile.value = profileRes.data
    posts.value = postsRes.data.records || []
  } catch {
    profile.value = null
  } finally {
    loading.value = false
    postsLoading.value = false
  }
}

function formatDate(value?: string) {
  if (!value) return ''
  const date = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString('zh-CN')
}

onMounted(() => {
  const id = Number(route.params.id)
  if (id) load(id)
})

watch(() => route.params.id, (id) => {
  const num = Number(id)
  if (num) load(num)
})
</script>

<style scoped>
.user-page { padding: 40px 0 70px; }
.loading-wrap { padding-top: 8px; }

.head { display: flex; gap: 22px; align-items: center; padding: 28px 30px; border-radius: var(--r-xl); }
.head__av {
  width: 76px; height: 76px; border-radius: 24px; flex-shrink: 0; display: grid; place-items: center;
  font-size: 30px; font-weight: 700; color: #04140f;
  background: linear-gradient(140deg, var(--accent), var(--violet));
  box-shadow: 0 16px 40px -16px color-mix(in srgb, var(--accent) 70%, transparent);
}
.head__info { min-width: 0; }
.head__info h2 { font-size: 22px; letter-spacing: -0.02em; font-weight: 700; }
.head__info > p { margin-top: 6px; color: var(--text-dim); font-size: 14px; line-height: 1.65; word-break: break-word; }
.head__tags { display: flex; gap: 8px; margin-top: 12px; flex-wrap: wrap; }

.feed { display: grid; gap: 12px; }

@media (max-width: 720px) {
  .user-page { padding: 26px 0 60px; }
  .head { flex-direction: column; align-items: flex-start; padding: 22px; }
}
</style>
