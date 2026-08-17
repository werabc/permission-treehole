<template>
  <div class="profile-page">
    <div class="profile-header">
      <div class="profile-avatar">{{ userInitial }}</div>
      <div class="profile-info">
        <h2>{{ userInfo.nickname || '未设置昵称' }}</h2>
        <p class="profile-username">@{{ userInfo.username }}</p>
        <p class="profile-bio">{{ userInfo.bio || '这个人很懒，什么都没写~' }}</p>
        <div class="profile-stats">
          <div class="stat">
            <span class="stat-value">{{ userInfo.postCount || 0 }}</span>
            <span class="stat-label">帖子</span>
          </div>
          <div class="stat">
            <span class="stat-value">{{ userInfo.commentCount || 0 }}</span>
            <span class="stat-label">评论</span>
          </div>
          <div class="stat" @click="showNotifications = true" style="cursor:pointer">
            <span class="stat-value">
              {{ unreadCount }}
              <span v-if="unreadCount > 0" class="badge">{{ unreadCount }}</span>
            </span>
            <span class="stat-label">通知</span>
          </div>
        </div>
      </div>
      <button class="th-btn th-btn-ghost btn-edit" @click="showEditDialog = true">编辑资料</button>
    </div>

    <div class="profile-tabs">
      <button :class="['tab-btn', { active: activeTab === 'posts' }]" @click="activeTab = 'posts'">我的帖子</button>
      <button :class="['tab-btn', { active: activeTab === 'received' }]" @click="activeTab = 'received'">谁评论了我</button>
      <button :class="['tab-btn', { active: activeTab === 'comments' }]" @click="activeTab = 'comments'">我的评论</button>
    </div>

    <div class="profile-content">
      <div v-if="activeTab === 'posts'">
        <div v-if="myPosts.length === 0" class="empty">还没有发过帖子，去发布第一条吧！</div>
        <div v-for="post in myPosts" :key="post.id" class="post-item" @click="$router.push(`/post/${post.id}`)">
          <p class="post-content">{{ post.content }}</p>
          <div class="post-meta">
            <span>👍 {{ post.likeCount }}</span>
            <span>💬 {{ post.commentCount }}</span>
            <span>👁 {{ post.viewCount }}</span>
            <span class="post-time">{{ formatTime(post.createTime) }}</span>
          </div>
        </div>
      </div>

      <div v-if="activeTab === 'received'">
        <div v-if="receivedComments.length === 0" class="empty">还没有人评论你的帖子</div>
        <div v-for="item in receivedComments" :key="item.commentId" class="comment-item" @click="$router.push(`/post/${item.postId}`)">
          <div class="comment-header">
            <span class="commenter">{{ item.commenterName }}</span>
            <span class="reply-arrow">评论了你的帖子</span>
          </div>
          <p class="comment-content">{{ item.content }}</p>
          <p class="original-post">原帖: {{ item.postContent?.substring(0, 50) }}...</p>
          <span class="comment-time">{{ formatTime(item.createTime) }}</span>
        </div>
      </div>

      <div v-if="activeTab === 'comments'">
        <div v-if="myComments.length === 0" class="empty">还没有发表过评论</div>
        <div v-for="item in myComments" :key="item.commentId" class="comment-item" @click="$router.push(`/post/${item.postId}`)">
          <p class="comment-content">{{ item.content }}</p>
          <p class="original-post">原帖: {{ item.postContent?.substring(0, 50) }}...</p>
          <span class="comment-time">{{ formatTime(item.createTime) }}</span>
        </div>
      </div>
    </div>

    <!-- 编辑资料对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑资料" width="450px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="editForm.gender">
            <el-radio :value="0">未知</el-radio>
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="个人简介">
          <el-input v-model="editForm.bio" type="textarea" :rows="3" placeholder="介绍一下自己..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>

    <!-- 通知抽屉 -->
    <el-drawer v-model="showNotifications" title="通知" direction="rtl" size="350px">
      <div v-if="notifications.length === 0" class="empty">暂无通知</div>
      <div v-for="notif in notifications" :key="notif.id" class="notif-item" :class="{ unread: !notif.isRead }">
        <p class="notif-content">{{ notif.content }}</p>
        <span class="notif-time">{{ formatTime(notif.createTime) }}</span>
      </div>
      <template #footer>
        <el-button v-if="notifications.length > 0" @click="markAllRead" size="small">全部已读</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElDialog, ElForm, ElFormItem, ElInput, ElButton, ElRadioGroup, ElRadio, ElDrawer } from 'element-plus'
import { getUserInfo } from '../api/auth'
import {
  getMyPosts, getReceivedComments, getMyComments,
  getNotifications, getUnreadCount, markNotificationsRead, updateProfile
} from '../api/treehole'

const userInfo = ref<any>({})
const activeTab = ref('posts')
const myPosts = ref<any[]>([])
const receivedComments = ref<any[]>([])
const myComments = ref<any[]>([])
const notifications = ref<any[]>([])
const unreadCount = ref(0)
const showEditDialog = ref(false)
const showNotifications = ref(false)
const saving = ref(false)
const editForm = reactive({ nickname: '', bio: '', gender: 0 })

const userInitial = computed(() => {
  const name = userInfo.value.nickname || userInfo.value.username || '?'
  return name.charAt(0).toUpperCase()
})

async function loadUserInfo() {
  try {
    const res = await getUserInfo()
    userInfo.value = res.data
    editForm.nickname = res.data.nickname || ''
    editForm.bio = res.data.bio || ''
    editForm.gender = res.data.gender || 0
  } catch (e: any) {
    ElMessage.error('加载用户信息失败')
  }
}

async function loadMyPosts() {
  try {
    const res = await getMyPosts({ pageNum: 1, pageSize: 20 })
    myPosts.value = res.data.records
  } catch (e) { /* ignore */ }
}

async function loadReceivedComments() {
  try {
    const res = await getReceivedComments({ pageNum: 1, pageSize: 20 })
    receivedComments.value = res.data.records
  } catch (e) { /* ignore */ }
}

async function loadMyComments() {
  try {
    const res = await getMyComments({ pageNum: 1, pageSize: 20 })
    myComments.value = res.data.records
  } catch (e) { /* ignore */ }
}

async function loadNotifications() {
  try {
    const res = await getNotifications({ pageNum: 1, pageSize: 20 })
    notifications.value = res.data.records
  } catch (e) { /* ignore */ }
}

async function loadUnreadCount() {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data
  } catch (e) { /* ignore */ }
}

async function saveProfile() {
  saving.value = true
  try {
    await updateProfile(editForm)
    ElMessage.success('保存成功')
    showEditDialog.value = false
    await loadUserInfo()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function markAllRead() {
  const unreadIds = notifications.value.filter((n: any) => !n.isRead).map((n: any) => n.id)
  if (unreadIds.length > 0) {
    try {
      await markNotificationsRead(unreadIds)
      await loadNotifications()
      await loadUnreadCount()
      ElMessage.success('已标记为已读')
    } catch (e) { /* ignore */ }
  }
}

function formatTime(time: string) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  return date.toLocaleDateString()
}

onMounted(() => {
  loadUserInfo()
  loadMyPosts()
  loadReceivedComments()
  loadMyComments()
  loadNotifications()
  loadUnreadCount()
})
</script>

<style scoped>
.profile-page {
  max-width: 700px;
  margin: 0 auto;
}

.profile-header {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.profile-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 600;
  flex-shrink: 0;
}

.profile-info {
  flex: 1;
}

.profile-info h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 2px;
}

.profile-username {
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 8px;
}

.profile-bio {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 12px;
}

.profile-stats {
  display: flex;
  gap: 24px;
}

.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  position: relative;
}

.badge {
  position: absolute;
  top: -8px;
  right: -12px;
  background: #ef4444;
  color: #fff;
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 10px;
  min-width: 16px;
  text-align: center;
}

.stat-label {
  font-size: 12px;
  color: #94a3b8;
}

.btn-edit {
  flex-shrink: 0;
}

.profile-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  background: #fff;
  border-radius: 12px;
  padding: 6px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.tab-btn {
  flex: 1;
  padding: 10px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
  transition: all 0.2s;
}

.tab-btn:hover {
  background: #f1f5f9;
}

.tab-btn.active {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
}

.profile-content {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  min-height: 300px;
}

.post-item {
  padding: 16px 0;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
  transition: background 0.2s;
}

.post-item:hover {
  background: #f8fafc;
  margin: 0 -20px;
  padding: 16px 20px;
  border-radius: 8px;
}

.post-item:last-child {
  border-bottom: none;
}

.post-content {
  font-size: 15px;
  color: #334155;
  line-height: 1.6;
  margin-bottom: 8px;
  white-space: pre-wrap;
  word-break: break-word;
}

.post-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #94a3b8;
}

.post-time {
  margin-left: auto;
}

.comment-item {
  padding: 14px 0;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
}

.comment-item:hover {
  background: #f8fafc;
  margin: 0 -20px;
  padding: 14px 20px;
  border-radius: 8px;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.commenter {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
}

.reply-arrow {
  font-size: 12px;
  color: #94a3b8;
}

.comment-content {
  font-size: 14px;
  color: #334155;
  margin-bottom: 4px;
}

.original-post {
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 4px;
}

.comment-time {
  font-size: 12px;
  color: #94a3b8;
}

.empty {
  text-align: center;
  padding: 60px 20px;
  color: #94a3b8;
  font-size: 14px;
}

.notif-item {
  padding: 14px 16px;
  border-bottom: 1px solid #f1f5f9;
  border-radius: 8px;
  margin-bottom: 8px;
}

.notif-item.unread {
  background: #eff6ff;
  border-left: 3px solid #3b82f6;
}

.notif-content {
  font-size: 14px;
  color: #334155;
  margin-bottom: 4px;
}

.notif-time {
  font-size: 12px;
  color: #94a3b8;
}
</style>
