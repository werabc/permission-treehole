<template>
  <article class="dh-card dh-card--hover post" @click="goDetail">
    <header class="post__top">
      <span v-if="post.isAnonymous === 1" class="dh-av dh-av--anon">
        <AppIcon name="user" :size="15" />
      </span>
      <button v-else class="dh-av" type="button" :title="post.authorName || '用户'" @click.stop="goAuthor">
        {{ initial }}
      </button>

      <span class="who">
        <button v-if="post.isAnonymous !== 1" class="who__name" type="button" @click.stop="goAuthor">
          {{ post.authorName || '未知用户' }}
        </button>
        <b v-else class="who__name who__name--anon">匿名</b>
        <span>{{ formatDate(post.createTime) }}</span>
      </span>

      <span v-if="post.isTop === 1" class="dh-tag dh-tag--pin">置顶</span>
      <span v-if="post.categoryName" class="dh-tag">{{ post.categoryName }}</span>
    </header>

    <p class="post__body">{{ preview }}</p>

    <footer class="post__foot">
      <span class="dh-metric"><AppIcon name="eye" :size="13" /> {{ post.viewCount || 0 }}</span>
      <span class="dh-metric"><AppIcon name="like" :size="13" /> {{ post.likeCount || 0 }}</span>
      <span class="dh-metric"><AppIcon name="comment" :size="13" /> {{ post.commentCount || 0 }}</span>

      <span class="post__acts">
        <button
          v-if="showCollect"
          class="mini"
          :class="{ on: collected }"
          type="button"
          @click.stop="$emit('collect', post)"
        >
          <AppIcon :name="collected ? 'star-fill' : 'star'" :size="13" />
          {{ collected ? '已收藏' : '收藏' }}
        </button>
        <button v-if="showDelete" class="mini mini--danger" type="button" @click.stop="$emit('delete', post)">
          <AppIcon name="trash" :size="13" /> 删除
        </button>
      </span>
    </footer>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from './AppIcon.vue'
import type { Post } from '../api/treehole'

const props = withDefaults(
  defineProps<{
    post: Post
    showDelete?: boolean
    showCollect?: boolean
    collected?: boolean
  }>(),
  { showDelete: false, showCollect: false, collected: false }
)

defineEmits<{
  (e: 'delete', post: Post): void
  (e: 'collect', post: Post): void
}>()

const router = useRouter()

const initial = computed(() => (props.post.authorName || '?').trim().charAt(0).toUpperCase() || '?')

const preview = computed(() => {
  const text = props.post.content || ''
  return text.length > 120 ? `${text.slice(0, 120)}…` : text
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
.post { padding: 22px 24px; }
.post__top { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.post__top .dh-tag:first-of-type { margin-left: auto; }

.who { display: flex; flex-direction: column; line-height: 1.25; min-width: 0; }
.who__name { font-size: 13.5px; font-weight: 500; color: var(--text); text-align: left; }
button.who__name:hover { color: var(--accent); }
.who__name--anon { color: var(--text-mute); font-weight: 400; }
.who span { font-size: 11.5px; color: var(--text-mute); font-family: var(--font-mono); }

.post__body { font-size: 15.5px; line-height: 1.78; color: var(--text); white-space: pre-wrap; word-break: break-word; }

.post__foot { display: flex; align-items: center; gap: 16px; margin-top: 16px; flex-wrap: wrap; }
.post__acts { margin-left: auto; display: flex; gap: 6px; opacity: 0; transform: translateX(6px); transition: all 0.35s var(--ease); }
.post:hover .post__acts, .post:focus-within .post__acts { opacity: 1; transform: none; }

.mini {
  display: inline-flex; align-items: center; gap: 6px; font-size: 12px; padding: 6px 12px;
  border-radius: 999px; color: var(--text-dim); border: 1px solid var(--border); transition: all 0.28s var(--ease);
}
.mini:hover { color: var(--accent); border-color: var(--accent-line); background: var(--accent-soft); }
.mini.on { color: var(--accent); border-color: var(--accent-line); background: var(--accent-soft); }
.mini--danger:hover { color: var(--danger); border-color: color-mix(in srgb, var(--danger) 45%, transparent); background: color-mix(in srgb, var(--danger) 12%, transparent); }

@media (max-width: 720px) {
  .post { padding: 18px; }
  .post__acts { opacity: 1; transform: none; margin-left: 0; width: 100%; }
}
</style>
