<template>
  <div class="dh-narrow notify">
    <div class="page-hd" v-reveal>
      <h2 class="dh-page-title">消息中心</h2>
      <p class="dh-page-sub">有人回应了你的心事。</p>

      <div class="dh-tabs head-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          type="button"
          :class="{ 'is-on': activeTab === tab.value }"
          @click="switchTab(tab.value)"
        >
          {{ tab.label }}
          <span v-if="tab.value === 'all' && unread > 0" class="cnt">{{ unread }}</span>
        </button>
      </div>
    </div>

    <div v-if="loading && items.length === 0" class="dh-skeleton" style="height: 78px" />

    <div v-else-if="items.length === 0" class="dh-state">暂时没有消息</div>

    <ul v-else class="notes">
      <li
        v-for="(item, i) in items"
        :key="item.id"
        :class="['dh-card', 'note', { unread: item.isRead === 0 }]"
        v-reveal="Math.min(i, 8) * 50"
        @click="openNotification(item)"
      >
        <span class="note__ico"><AppIcon :name="typeIcon(item.type)" :size="18" /></span>
        <div class="note__b">
          <b>{{ item.content }}</b>
          <p>{{ typeHint(item.type) }}</p>
        </div>
        <span class="note__t">{{ formatTime(item.createTime) }}</span>
      </li>
    </ul>

    <div v-if="!loading && total > items.length" class="load-more">
      <button class="dh-btn dh-btn--ghost" type="button" @click="loadMore">加载更多</button>
    </div>

    <div v-if="!loading && items.length > 0" class="mark-all">
      <button class="dh-btn dh-btn--ghost" type="button" :disabled="unread === 0" @click="markAllRead">
        <AppIcon name="check" :size="15" /> 全部标为已读
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from '../components/AppIcon.vue'
import { getNotifications, markNotificationsRead, type NotificationItem } from '../api/treehole'

const router = useRouter()

const tabs = [
  { label: '全部', value: 'all' },
  { label: '回响', value: 'COMMENT' },
  { label: '抱抱', value: 'LIKE' },
  { label: '系统', value: 'REPORT_RESULT' },
]

const items = ref<NotificationItem[]>([])
const total = ref(0)
const unread = ref(0)
const pageNum = ref(1)
const pageSize = 15
const loading = ref(false)
const activeTab = ref('all')

async function load(reset = false) {
  if (loading.value) return
  loading.value = true
  try {
    if (reset) {
      pageNum.value = 1
      items.value = []
    }
    const res = await getNotifications({ pageNum: pageNum.value, pageSize })
    let records = res.data.records || []
    if (activeTab.value !== 'all') {
      records = records.filter((n) => n.type === activeTab.value)
    }
    items.value = reset ? records : items.value.concat(records)
    total.value = res.data.total || 0
    unread.value = items.value.filter((n) => n.isRead === 0).length
  } catch {
    /* ignore */
  } finally {
    loading.value = false
  }
}

function switchTab(value: string) {
  activeTab.value = value
  load(true)
}

function loadMore() {
  pageNum.value += 1
  load(false)
}

async function markAllRead() {
  const unreadIds = items.value.filter((n) => n.isRead === 0).map((n) => n.id)
  if (unreadIds.length === 0) return
  try {
    await markNotificationsRead(unreadIds)
    items.value.forEach((n) => { n.isRead = 1 })
    unread.value = 0
    window.dispatchEvent(new Event('storage'))
  } catch {
    /* ignore */
  }
}

async function openNotification(item: NotificationItem) {
  if (item.isRead === 0) {
    try {
      await markNotificationsRead([item.id])
      item.isRead = 1
      unread.value = Math.max(unread.value - 1, 0)
      window.dispatchEvent(new Event('storage'))
    } catch {
      /* 标记失败不阻塞跳转 */
    }
  }
  if (item.targetType === 'POST' && item.targetId) {
    router.push(`/post/${item.targetId}`)
  }
}

function typeIcon(type: string) {
  if (type === 'LIKE') return 'like'
  if (type === 'COMMENT') return 'comment'
  if (type === 'REPORT_RESULT') return 'shield'
  return 'bell'
}

function typeHint(type: string) {
  if (type === 'LIKE') return '点开看看是谁抱了抱你'
  if (type === 'COMMENT') return '点开查看完整回响'
  if (type === 'REPORT_RESULT') return '举报处理结果通知'
  return '系统消息'
}

function formatTime(value?: string) {
  if (!value) return ''
  const date = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return value
  const diff = Date.now() - date.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)} 天前`
  return date.toLocaleString('zh-CN', { hour12: false })
}

onMounted(() => load(true))
</script>

<style scoped>
.notify { padding-bottom: 80px; }
.page-hd { padding: 44px 0 8px; }
.head-tabs { margin-top: 22px; }
.cnt { font-family: var(--font-mono); font-size: 10.5px; color: var(--accent); }

.notes { display: grid; gap: 10px; margin-top: 22px; }
.note {
  position: relative; display: flex; gap: 14px; padding: 18px 20px; align-items: flex-start;
  cursor: pointer; transition: transform 0.35s var(--ease), border-color 0.35s var(--ease);
}
.note:hover { transform: translateY(-3px); border-color: color-mix(in srgb, var(--accent) 34%, transparent); }
.note.unread {
  border-color: color-mix(in srgb, var(--accent) 30%, transparent);
  background: color-mix(in srgb, var(--accent) 6%, var(--surface));
}
.note.unread::before {
  content: ""; position: absolute; left: -1px; top: 22px; width: 2px; height: 22px; border-radius: 2px; background: var(--accent);
}
.note__ico {
  width: 38px; height: 38px; border-radius: 12px; display: grid; place-items: center; flex-shrink: 0;
  border: 1px solid var(--border); color: var(--accent); background: var(--accent-soft);
}
.note__b { flex: 1; min-width: 0; }
.note__b b { font-size: 14px; font-weight: 500; line-height: 1.6; display: block; }
.note__b p { font-size: 12.5px; color: var(--text-mute); margin-top: 4px; }
.note__t { font-family: var(--font-mono); font-size: 11px; color: var(--text-mute); white-space: nowrap; flex-shrink: 0; }

.load-more, .mark-all { text-align: center; margin-top: 24px; }

@media (max-width: 720px) {
  .page-hd { padding-top: 32px; }
  .note { padding: 15px 16px; }
  .note__t { font-size: 10px; }
}
</style>
