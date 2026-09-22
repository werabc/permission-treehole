<template>
  <article class="post-card" @click="goDetail">
    <header class="post-head">
      <button
        v-if="!post.isAnonymous"
        class="author"
        type="button"
        @click.stop="goAuthor"
      >{{ post.authorName || '未知用户' }}</button>
      <span v-else class="author anonymous">匿名用户</span>

      <span v-if="post.categoryName" class="category">{{ post.categoryName }}</span>
      <span v-if="post.isTop === 1" class="top-flag">置顶</span>
    </header>

    <p class="post-content">{{ preview }}</p>

    <footer class="post-foot">
      <span class="meta">{{ formatDate(post.createTime) }}</span>
      <span class="stats">
        <span class="stat">浏览 {{ post.viewCount || 0 }}</span>
        <span class="stat">点赞 {{ post.likeCount || 0 }}</span>
        <span class="stat">评论 {{ post.commentCount || 0 }}</span>
      </span>

      <span class="actions">
        <button
          v-if="showCollect"
          class="act-btn"
          :class="{ active: collected }"
          type="button"
          :title="collected ? '取消收藏' : '收藏'"
          @click.stop="$emit('collect', post)"
        >{{ collected ? '★ 已收藏' : '☆ 收藏' }}</button>
        <button
          v-if="showDelete"
          class="act-btn danger"
          type="button"
          title="删除"
          @click.stop="$emit('delete', post)"
        >删除</button>
      </span>
    </footer>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { Post } from '../api/treehole'

const props = withDefaults(defineProps<{
  post: Post
  showDelete?: boolean
  showCollect?: boolean
  collected?: boolean
}>(), {
  showDelete: false,
  showCollect: false,
  collected: false
})

defineEmits<{
  (e: 'delete', post: Post): void
  (e: 'collect', post: Post): void
}>()

const router = useRouter()

const preview = computed(() => {
  const text = props.post.content || ''
  return text.length > 120 ? text.slice(0, 120) + '…' : text
})

function goDetail() {
  router.push(`/post/${props.post.id}`)
}

function goAuthor() {
  if (props.post.isAnonymous === 1) return
  if (!props.post.userId) return
  router.push(`/user/${props.post.userId}`)
}

function formatDate(value?: string) {
  if (!value) return ''
  const date = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return value
  const diff = Date.now() - date.getTime()
  const minute = 60 * 1000
  if (diff < minute) return '刚刚'
  if (diff < 60 * minute) return `${Math.floor(diff / minute)} 分钟前`
  if (diff < 24 * 60 * minute) return `${Math.floor(diff / (60 * minute))} 小时前`
  if (diff < 7 * 24 * 60 * minute) return `${Math.floor(diff / (24 * 60 * minute))} 天前`
  return date.toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.post-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 18px 20px;
  margin-bottom: 14px;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.2s;
}
.post-card:hover {
  border-color: #bfdbfe;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.10);
  transform: translateY(-1px);
}
.post-head { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.author {
  background: none; border: none; padding: 0; cursor: pointer;
  color: #3b82f6; font-size: 14px; font-weight: 600;
}
.author:hover { text-decoration: underline; }
.author.anonymous { color: #94a3b8; cursor: default; font-weight: 500; }
.category {
  font-size: 12px; color: #64748b; background: #f1f5f9;
  padding: 2px 8px; border-radius: 10px;
}
.top-flag { font-size: 12px; color: #b45309; background: #fef3c7; padding: 2px 8px; border-radius: 10px; }
.post-content { margin: 0 0 12px; color: #1e293b; font-size: 15px; line-height: 1.7; white-space: pre-wrap; word-break: break-word; }
.post-foot { display: flex; align-items: center; gap: 14px; font-size: 12px; color: #94a3b8; flex-wrap: wrap; }
.stats { display: flex; gap: 10px; }
.actions { margin-left: auto; display: flex; gap: 8px; }
.act-btn {
  background: #f8fafc; border: 1px solid #e2e8f0; color: #64748b;
  font-size: 12px; padding: 3px 10px; border-radius: 6px; cursor: pointer;
  transition: all 0.2s;
}
.act-btn:hover { border-color: #93c5fd; color: #3b82f6; background: #eff6ff; }
.act-btn.active { color: #b45309; border-color: #fcd34d; background: #fffbeb; }
.act-btn.danger:hover { color: #ef4444; border-color: #fca5a5; background: #fef2f2; }
@media (max-width: 640px) {
  .post-card { padding: 14px 15px; }
  .actions { margin-left: 0; width: 100%; }
}
</style>
