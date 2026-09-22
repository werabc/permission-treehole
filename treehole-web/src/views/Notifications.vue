<template>
  <div class="notify-page">
    <div class="page-head">
      <h2>消息中心</h2>
      <button
        class="mark-all"
        type="button"
        :disabled="unread === 0 || loading"
        @click="markAllRead"
      >全部已读{{ unread > 0 ? `（${unread}）` : '' }}</button>
    </div>

    <div class="tabs">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        class="tab-btn"
        :class="{ active: activeTab === tab.value }"
        type="button"
        @click="switchTab(tab.value)"
      >{{ tab.label }}</button>
    </div>

    <div v-if="loading" class="state">加载中…</div>
    <div v-else-if="items.length === 0" class="state">暂时没有消息</div>

    <ul v-else class="notify-list">
      <li
        v-for="item in items"
        :key="item.id"
        class="notify-item"
        :class="{ unread: item.isRead === 0 }"
        @click="openNotification(item)"
      >
        <span class="type-dot" :style="{ background: typeColor(item.type) }"></span>
        <div class="notify-body">
          <p class="content">{{ item.content }}</p>
          <span class="time">{{ formatTime(item.createTime) }}</span>
        </div>
        <span v-if="item.isRead === 0" class="unread-flag">未读</span>
      </li>
    </ul>

    <div v-if="!loading && total > items.length" class="load-more">
      <button type="button" @click="loadMore">加载更多</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getNotifications, markNotificationsRead, type NotificationItem } from '../api/treehole'

const router = useRouter()

const tabs = [
  { label: '全部', value: 'all' },
  { label: '点赞', value: 'LIKE' },
  { label: '评论', value: 'COMMENT' },
  { label: '系统', value: 'REPORT_RESULT' }
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
    const res = await getNotifications({
      pageNum: pageNum.value,
      pageSize,
      unreadOnly: activeTab.value === 'unread'
    })
    let records = res.data.records || []
    if (isTypeTab()) {
      records = records.filter((n: NotificationItem) => n.type === activeTab.value)
    }
    items.value = reset ? records : items.value.concat(records)
    total.value = res.data.total || 0
    unread.value = items.value.filter((n) => n.isRead === 0).length
  } catch (e) {
    console.error('加载通知失败', e)
  } finally {
    loading.value = false
  }
}

function isTypeTab() {
  return activeTab.value !== 'all' && activeTab.value !== 'unread'
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
  } catch (e) {
    console.error('标记已读失败', e)
  }
}

async function openNotification(item: NotificationItem) {
  if (item.isRead === 0) {
    try {
      await markNotificationsRead([item.id])
      item.isRead = 1
      unread.value = Math.max(unread.value - 1, 0)
      window.dispatchEvent(new Event('storage'))
    } catch (e) { /* 忽略标记失败，不阻塞跳转 */ }
  }
  if (item.targetType === 'POST' && item.targetId) {
    router.push(`/post/${item.targetId}`)
  }
}

function typeColor(type: string) {
  if (type === 'LIKE') return '#f59e0b'
  if (type === 'COMMENT') return '#3b82f6'
  if (type === 'REPORT_RESULT') return '#10b981'
  return '#94a3b8'
}

function formatTime(value?: string) {
  if (!value) return ''
  const date = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}

onMounted(() => load(true))
</script>

<style scoped>
.notify-page { max-width: 720px; margin: 0 auto; }
.page-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.page-head h2 { font-size: 20px; color: #1e293b; margin: 0; }
.mark-all {
  background: #eff6ff; color: #3b82f6; border: 1px solid #bfdbfe;
  font-size: 13px; padding: 6px 14px; border-radius: 8px; cursor: pointer;
}
.mark-all:disabled { opacity: 0.5; cursor: not-allowed; }
.tabs { display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.tab-btn {
  background: #f8fafc; border: 1px solid #e2e8f0; color: #64748b;
  font-size: 13px; padding: 6px 14px; border-radius: 20px; cursor: pointer;
  transition: all 0.2s;
}
.tab-btn:hover { border-color: #93c5fd; color: #3b82f6; }
.tab-btn.active { background: #3b82f6; border-color: #3b82f6; color: #fff; }
.notify-list { list-style: none; padding: 0; margin: 0; }
.notify-item {
  display: flex; align-items: flex-start; gap: 12px;
  background: #fff; border: 1px solid #e2e8f0; border-radius: 10px;
  padding: 14px 16px; margin-bottom: 10px; cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}
.notify-item:hover { border-color: #bfdbfe; background: #f8fbff; }
.notify-item.unread { border-left: 3px solid #3b82f6; }
.type-dot { width: 8px; height: 8px; border-radius: 50%; margin-top: 7px; flex: 0 0 auto; }
.notify-body { flex: 1; min-width: 0; }
.content { margin: 0 0 4px; color: #1e293b; font-size: 14px; line-height: 1.6; word-break: break-word; }
.time { color: #94a3b8; font-size: 12px; }
.unread-flag { color: #3b82f6; font-size: 12px; flex: 0 0 auto; }
.state { text-align: center; color: #94a3b8; padding: 48px 0; font-size: 14px; }
.load-more { text-align: center; margin: 16px 0 8px; }
.load-more button {
  background: #fff; border: 1px solid #e2e8f0; color: #64748b;
  padding: 8px 20px; border-radius: 8px; cursor: pointer; font-size: 13px;
}
</style>
