<template>
  <div class="user-page">
    <div v-if="loading" class="state">加载中…</div>
    <div v-else-if="!profile" class="state">用户不存在</div>

    <template v-else>
      <section class="profile-head">
        <div class="avatar">{{ avatarText }}</div>
        <div class="info">
          <h2>{{ profile.nickname }}</h2>
          <p class="bio">{{ profile.bio || '这个人很懒，什么都没留下' }}</p>
          <p class="stats">
            <span>发帖 {{ profile.postCount || 0 }}</span>
            <span>评论 {{ profile.commentCount || 0 }}</span>
            <span>加入于 {{ formatDate(profile.createTime) }}</span>
          </p>
        </div>
      </section>

      <h3 class="section-title">TA 的帖子</h3>

      <div v-if="postsLoading" class="state">加载中…</div>
      <div v-else-if="posts.length === 0" class="state">还没有发布过帖子</div>
      <PostCard v-for="post in posts" :key="post.id" :post="post" />
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
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
  loading.value = true
  postsLoading.value = true
  try {
    const [profileRes, postsRes] = await Promise.all([
      getPublicProfile(userId),
      getUserPosts(userId, { pageNum: 1, pageSize: 20 })
    ])
    profile.value = profileRes.data
    posts.value = postsRes.data.records || []
  } catch (e) {
    console.error('加载用户主页失败', e)
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
.user-page { max-width: 720px; margin: 0 auto; }
.profile-head {
  display: flex; gap: 18px; align-items: center;
  background: #fff; border: 1px solid #e2e8f0; border-radius: 12px;
  padding: 22px; margin-bottom: 22px;
}
.avatar {
  width: 64px; height: 64px; border-radius: 50%; flex: 0 0 auto;
  background: linear-gradient(135deg, #60a5fa, #3b82f6); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 26px; font-weight: 600;
}
.info { min-width: 0; }
.info h2 { margin: 0 0 6px; font-size: 19px; color: #1e293b; }
.bio { margin: 0 0 8px; color: #64748b; font-size: 14px; line-height: 1.6; word-break: break-word; }
.stats { margin: 0; color: #94a3b8; font-size: 13px; display: flex; gap: 16px; flex-wrap: wrap; }
.section-title { font-size: 16px; color: #1e293b; margin: 0 0 14px; }
.state { text-align: center; color: #94a3b8; padding: 40px 0; font-size: 14px; }
@media (max-width: 640px) {
  .profile-head { flex-direction: column; text-align: center; }
}
</style>
