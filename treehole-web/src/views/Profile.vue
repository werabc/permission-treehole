<template>
  <div class="dh-wrap prof">
    <!-- ============ 头部 ============ -->
    <div class="dh-card prof__head" v-reveal>
      <div class="prof__av">{{ userInitial }}</div>
      <div class="prof__info">
        <h2>{{ userInfo.nickname || '未设置昵称' }}</h2>
        <p>@{{ userInfo.username }} <span v-if="userInfo.bio">· {{ userInfo.bio }}</span></p>
        <div class="prof__tags">
          <span class="dh-tag">已发布 {{ userInfo.postCount || 0 }} 条心事</span>
          <span class="dh-tag">写下 {{ userInfo.commentCount || 0 }} 条回响</span>
          <span v-if="unreadCount > 0" class="dh-tag dh-tag--pin">{{ unreadCount }} 条未读消息</span>
        </div>
      </div>
      <button class="dh-btn dh-btn--ghost" type="button" @click="openEdit">
        <AppIcon name="pencil" :size="15" /> 编辑资料
      </button>
    </div>

    <!-- ============ 数据 ============ -->
    <div class="bento">
      <button class="dh-card dh-card--hover bento__cell" type="button" @click="switchTab('posts')">
        <b>{{ userInfo.postCount || 0 }}</b><span>发布的心事</span>
      </button>
      <button class="dh-card dh-card--hover bento__cell" type="button" @click="switchTab('comments')">
        <b>{{ userInfo.commentCount || 0 }}</b><span>写下的回响</span>
      </button>
      <button class="dh-card dh-card--hover bento__cell" type="button" @click="switchTab('collects')">
        <b>{{ collectCount }}</b><span>收藏的心事</span>
      </button>
      <button class="dh-card dh-card--hover bento__cell" type="button" @click="showNotifications = true">
        <b>{{ unreadCount }}</b><span>未读消息</span>
      </button>
    </div>

    <!-- ============ 页签 ============ -->
    <div class="dh-tabs prof__tabs">
      <button type="button" :class="{ 'is-on': activeTab === 'posts' }" @click="switchTab('posts')">我的心事</button>
      <button type="button" :class="{ 'is-on': activeTab === 'received' }" @click="switchTab('received')">谁回响了我</button>
      <button type="button" :class="{ 'is-on': activeTab === 'comments' }" @click="switchTab('comments')">我的回响</button>
      <button type="button" :class="{ 'is-on': activeTab === 'collects' }" @click="switchTab('collects')">我的收藏</button>
    </div>

    <!-- ============ 内容 ============ -->
    <div class="prof__body">
      <!-- 我的心事 -->
      <template v-if="activeTab === 'posts'">
        <div v-if="myPosts.length === 0" class="dh-state">
          <p>还没有发过心事</p>
          <router-link to="/publish" class="dh-btn dh-btn--ghost" style="margin-top: 14px">去写第一条</router-link>
        </div>
        <div v-else class="feed">
          <article
            v-for="(post, i) in myPosts"
            :key="post.id"
            class="dh-card dh-card--hover item"
            v-reveal="Math.min(i, 6) * 60"
            @click="$router.push(`/post/${post.id}`)"
          >
            <p class="item__text">{{ post.content }}</p>
            <div class="item__foot">
              <span class="dh-metric"><AppIcon name="eye" :size="13" /> {{ post.viewCount }}</span>
              <span class="dh-metric"><AppIcon name="like" :size="13" /> {{ post.likeCount }}</span>
              <span class="dh-metric"><AppIcon name="comment" :size="13" /> {{ post.commentCount }}</span>
              <span class="item__time">{{ formatTime(post.createTime) }}</span>
              <span class="item__acts">
                <button class="mini mini--danger" type="button" @click.stop="handleDeletePost(post)">
                  <AppIcon name="trash" :size="13" /> 删除
                </button>
              </span>
            </div>
          </article>
        </div>
      </template>

      <!-- 我的收藏 -->
      <template v-else-if="activeTab === 'collects'">
        <div v-if="myCollects.length === 0" class="dh-state">还没有收藏过心事</div>
        <div v-else class="feed">
          <PostCard
            v-for="(post, i) in myCollects"
            :key="post.id"
            v-reveal="Math.min(i, 6) * 60"
            :post="post"
            show-collect
            :collected="true"
            @collect="handleUncollect"
          />
        </div>
      </template>

      <!-- 谁回响了我 -->
      <template v-else-if="activeTab === 'received'">
        <div v-if="receivedComments.length === 0" class="dh-state">还没有人回响你的心事</div>
        <div v-else class="feed">
          <article
            v-for="(item, i) in receivedComments"
            :key="item.commentId"
            class="dh-card dh-card--hover item"
            v-reveal="Math.min(i, 6) * 60"
            @click="$router.push(`/post/${item.postId}`)"
          >
            <div class="item__head">
              <span class="dh-av">{{ (item.commenterName || '?').charAt(0).toUpperCase() }}</span>
              <b>{{ item.commenterName }}</b>
              <span class="item__hint">回响了你的心事</span>
              <span class="item__time">{{ formatTime(item.createTime) }}</span>
            </div>
            <p class="item__text">{{ item.content }}</p>
            <p class="item__quote">原帖：{{ item.postContent?.slice(0, 60) }}…</p>
          </article>
        </div>
      </template>

      <!-- 我的回响 -->
      <template v-else>
        <div v-if="myComments.length === 0" class="dh-state">还没有发表过回响</div>
        <div v-else class="feed">
          <article
            v-for="(item, i) in myComments"
            :key="item.commentId"
            class="dh-card dh-card--hover item"
            v-reveal="Math.min(i, 6) * 60"
            @click="$router.push(`/post/${item.postId}`)"
          >
            <p class="item__text">{{ item.content }}</p>
            <p class="item__quote">原帖：{{ item.postContent?.slice(0, 60) }}…</p>
            <div class="item__foot">
              <span class="item__time">{{ formatTime(item.createTime) }}</span>
              <span class="item__acts">
                <button class="mini mini--danger" type="button" @click.stop="handleDeleteComment(item)">
                  <AppIcon name="trash" :size="13" /> 删除
                </button>
              </span>
            </div>
          </article>
        </div>
      </template>
    </div>

    <!-- ============ 编辑资料 ============ -->
    <el-dialog v-model="showEditDialog" title="编辑资料" width="440px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" maxlength="20" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="editForm.gender">
            <el-radio :value="0">保密</el-radio>
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="个人简介">
          <el-input v-model="editForm.bio" type="textarea" :rows="3" maxlength="200" placeholder="介绍一下自己…" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>

    <!-- ============ 通知抽屉 ============ -->
    <el-drawer v-model="showNotifications" title="消息" direction="rtl" size="360px">
      <div v-if="notifications.length === 0" class="dh-state dh-state--sm">暂无消息</div>
      <div v-for="notif in notifications" :key="notif.id" :class="['notif', { unread: !notif.isRead }]">
        <p>{{ notif.content }}</p>
        <span>{{ formatTime(notif.createTime) }}</span>
      </div>
      <template #footer>
        <div class="drawer-foot">
          <router-link to="/notifications" @click="showNotifications = false">进入消息中心 →</router-link>
          <el-button v-if="notifications.length > 0" size="small" @click="markAllRead">全部已读</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ElMessage, ElMessageBox, ElDialog, ElForm, ElFormItem, ElInput, ElButton, ElRadioGroup, ElRadio, ElDrawer,
} from 'element-plus'
import PostCard from '../components/PostCard.vue'
import AppIcon from '../components/AppIcon.vue'
import { getUserInfo } from '../api/auth'
import {
  getMyPosts, getReceivedComments, getMyComments, getMyCollects, getCollectCount,
  deletePost as removePost, deleteComment as removeComment, toggleCollect,
  getNotifications, getUnreadCount, markNotificationsRead, updateProfile,
} from '../api/treehole'

const userInfo = ref<any>({})
const activeTab = ref('posts')
const myPosts = ref<any[]>([])
const myCollects = ref<any[]>([])
const receivedComments = ref<any[]>([])
const myComments = ref<any[]>([])
const notifications = ref<any[]>([])
const unreadCount = ref(0)
const collectCount = ref(0)
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
    userInfo.value = res.data || {}
  } catch {
    ElMessage.error('加载用户信息失败')
  }
}

function openEdit() {
  editForm.nickname = userInfo.value.nickname || ''
  editForm.bio = userInfo.value.bio || ''
  editForm.gender = userInfo.value.gender || 0
  showEditDialog.value = true
}

async function loadMyPosts() {
  try {
    const res = await getMyPosts({ pageNum: 1, pageSize: 20 })
    myPosts.value = res.data.records || []
  } catch { /* ignore */ }
}

async function loadReceivedComments() {
  try {
    const res = await getReceivedComments({ pageNum: 1, pageSize: 20 })
    receivedComments.value = res.data.records || []
  } catch { /* ignore */ }
}

async function loadMyComments() {
  try {
    const res = await getMyComments({ pageNum: 1, pageSize: 20 })
    myComments.value = res.data.records || []
  } catch { /* ignore */ }
}

async function loadMyCollects() {
  try {
    const res = await getMyCollects({ pageNum: 1, pageSize: 20 })
    myCollects.value = res.data.records || []
  } catch { /* ignore */ }
}

async function loadCollectCount() {
  try {
    const res = await getCollectCount()
    collectCount.value = res.data || 0
  } catch { /* ignore */ }
}

/** 首次切到某页签才按需加载，减少首屏请求 */
function switchTab(tab: string) {
  activeTab.value = tab
  if (tab === 'posts' && myPosts.value.length === 0) loadMyPosts()
  if (tab === 'received' && receivedComments.value.length === 0) loadReceivedComments()
  if (tab === 'comments' && myComments.value.length === 0) loadMyComments()
  if (tab === 'collects' && myCollects.value.length === 0) loadMyCollects()
}

async function handleDeletePost(post: any) {
  try {
    await ElMessageBox.confirm('删除后该帖子的评论、点赞、收藏也会一并清除，确定删除？', '删除帖子', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await removePost(post.id)
    ElMessage.success('已删除')
    myPosts.value = myPosts.value.filter((p) => p.id !== post.id)
    userInfo.value.postCount = Math.max((userInfo.value.postCount || 1) - 1, 0)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '删除失败')
  }
}

async function handleDeleteComment(item: any) {
  try {
    await ElMessageBox.confirm('确定删除这条回响？', '删除回响', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await removeComment(item.commentId)
    ElMessage.success('已删除')
    myComments.value = myComments.value.filter((c) => c.commentId !== item.commentId)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '删除失败')
  }
}

async function handleUncollect(post: any) {
  try {
    await toggleCollect(post.id)
    myCollects.value = myCollects.value.filter((p) => p.id !== post.id)
    collectCount.value = Math.max(collectCount.value - 1, 0)
    ElMessage.success('已取消收藏')
  } catch {
    ElMessage.error('操作失败')
  }
}

async function loadNotifications() {
  try {
    const res = await getNotifications({ pageNum: 1, pageSize: 20 })
    notifications.value = res.data.records || []
  } catch { /* ignore */ }
}

async function loadUnreadCount() {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data || 0
  } catch { /* ignore */ }
}

async function saveProfile() {
  saving.value = true
  try {
    await updateProfile(editForm)
    ElMessage.success('资料已保存')
    showEditDialog.value = false
    await loadUserInfo()
    window.dispatchEvent(new Event('storage'))
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function markAllRead() {
  const unreadIds = notifications.value.filter((n: any) => !n.isRead).map((n: any) => n.id)
  if (unreadIds.length === 0) return
  try {
    await markNotificationsRead(unreadIds)
    await loadNotifications()
    await loadUnreadCount()
    ElMessage.success('已标记为已读')
  } catch { /* ignore */ }
}

function formatTime(time: string) {
  if (!time) return ''
  const date = new Date(time.replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return time
  const diff = Date.now() - date.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)} 天前`
  return date.toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadUserInfo()
  loadMyPosts()
  loadCollectCount()
  loadUnreadCount()
})
</script>

<style scoped>
.prof { padding: 40px 0 70px; }

.prof__head { display: flex; align-items: center; gap: 24px; padding: 30px 32px; border-radius: var(--r-xl); }
.prof__av {
  width: 84px; height: 84px; border-radius: 26px; display: grid; place-items: center; flex-shrink: 0;
  font-size: 32px; font-weight: 700; color: #04140f;
  background: linear-gradient(140deg, var(--accent), var(--violet));
  box-shadow: 0 16px 40px -16px color-mix(in srgb, var(--accent) 70%, transparent);
}
.prof__info { flex: 1; min-width: 0; }
.prof__info h2 { font-size: 24px; letter-spacing: -0.02em; font-weight: 700; }
.prof__info p { color: var(--text-dim); font-size: 13.5px; margin-top: 5px; word-break: break-word; }
.prof__tags { display: flex; gap: 8px; margin-top: 12px; flex-wrap: wrap; }

.bento { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin-top: 12px; }
.bento__cell { padding: 18px 20px; text-align: left; }
.bento__cell b { display: block; font-family: var(--font-mono); font-size: 23px; font-weight: 500; letter-spacing: -0.02em; }
.bento__cell span { font-size: 12px; color: var(--text-mute); }

.prof__tabs { margin-top: 26px; }
.prof__body { margin-top: 20px; }
.feed { display: grid; gap: 12px; }

.item { padding: 20px 22px; cursor: pointer; }
.item__head { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.item__head b { font-size: 13.5px; font-weight: 500; }
.item__hint { font-size: 12px; color: var(--text-mute); }
.item__text { font-size: 15px; line-height: 1.75; white-space: pre-wrap; word-break: break-word; }
.item__quote { margin-top: 10px; font-size: 12.5px; color: var(--text-mute); line-height: 1.6; }
.item__foot { display: flex; align-items: center; gap: 16px; margin-top: 14px; flex-wrap: wrap; }
.item__time { font-family: var(--font-mono); font-size: 11.5px; color: var(--text-mute); }
.item__acts { margin-left: auto; display: flex; gap: 6px; }

.mini {
  display: inline-flex; align-items: center; gap: 6px; font-size: 12px; padding: 6px 12px;
  border-radius: 999px; color: var(--text-dim); border: 1px solid var(--border); transition: all 0.28s var(--ease);
}
.mini--danger:hover { color: var(--danger); border-color: color-mix(in srgb, var(--danger) 45%, transparent); background: color-mix(in srgb, var(--danger) 12%, transparent); }

.notif { padding: 14px 16px; border-bottom: 1px solid var(--border); }
.notif.unread { background: var(--accent-soft); border-radius: var(--r-sm); border-left: 3px solid var(--accent); }
.notif p { font-size: 14px; line-height: 1.6; }
.notif span { font-size: 12px; color: var(--text-mute); }
.drawer-foot { display: flex; align-items: center; justify-content: space-between; }
.drawer-foot a { color: var(--accent); font-size: 13px; }
.drawer-foot a:hover { text-decoration: underline; }

@media (max-width: 980px) {
  .bento { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 720px) {
  .prof { padding: 26px 0 60px; }
  .prof__head { flex-direction: column; align-items: flex-start; padding: 22px; }
  .prof__head .dh-btn { width: 100%; }
}
</style>
