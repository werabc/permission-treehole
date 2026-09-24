<template>
  <div v-if="post" class="dh-narrow detail">
    <button class="back" type="button" @click="router.back()">
      <AppIcon name="arrow-left" :size="15" /> 返回广场
    </button>

    <!-- ============ 帖子 ============ -->
    <article class="dh-card detail__card" v-reveal>
      <div class="post__top">
        <AppAvatar v-if="post.isAnonymous === 1" :anonymous="true" :size="36" />
        <AppAvatar v-else :src="post.authorAvatar" :name="post.authorName" :size="36" />
        <span class="who">
          <b>{{ post.isAnonymous === 1 ? '匿名' : post.authorName || '未知用户' }}</b>
          <span>{{ formatTime(post.createTime) }}</span>
        </span>
        <span v-if="post.categoryName" class="dh-tag">{{ post.categoryName }}</span>
      </div>

      <p class="detail__body">{{ post.content }}</p>

      <div v-if="post.images && post.images.length > 0" class="post-images">
        <img
          v-for="(img, idx) in post.images"
          :key="idx"
          :src="img"
          class="post-image"
          alt="帖子图片"
          @click="previewImage(img)"
        />
      </div>

      <div class="actbar">
        <button type="button" :class="['act', { on: liked }]" @click="handleLike">
          <AppIcon name="like" :size="15" /> {{ liked ? '已抱抱' : '抱抱' }} · {{ post.likeCount }}
        </button>
        <button type="button" :class="['act', { on: collected }]" @click="handleCollect">
          <AppIcon :name="collected ? 'star-fill' : 'star'" :size="15" /> {{ collected ? '已收藏' : '收藏' }}
        </button>
        <span class="act act--static"><AppIcon name="eye" :size="15" /> {{ post.viewCount }} 次浏览</span>
        <button v-if="isOwner" class="act act--danger" type="button" @click="handleDeletePost">
          <AppIcon name="trash" :size="15" /> 删除
        </button>
        <button class="act act--danger act--end" type="button" @click="openReportDialog">
          <AppIcon name="flag" :size="15" /> 举报
        </button>
      </div>
    </article>

    <!-- ============ 回响 ============ -->
    <div class="dh-section-title">
      <h3>回响</h3>
      <span>{{ commentTotal }} REPLIES</span>
    </div>

    <div class="dh-card composer" v-reveal>
      <textarea
        v-model="commentContent"
        class="composer__in"
        :placeholder="replyTo ? `回复 ${replyToName}…` : '写下你的回响…不必认识，也能懂。'"
      />
      <div class="composer__foot">
        <div class="composer__left">
          <button type="button" :class="['dh-switch', { 'is-on': commentAnonymous }]" @click="commentAnonymous = !commentAnonymous">
            <i /> 匿名回响
          </button>
          <button v-if="replyTo" class="dh-btn dh-btn--quiet dh-btn--sm" type="button" @click="cancelReply">
            取消回复
          </button>
        </div>
        <button class="dh-btn dh-btn--primary dh-btn--sm" type="button" @click="handleComment">
          <AppIcon name="send" :size="14" /> 发送
        </button>
      </div>
    </div>

    <div class="feed">
      <div
        v-for="(comment, i) in comments"
        :key="comment.id"
        class="dh-card cmt"
        v-reveal="Math.min(i, 6) * 60"
      >
        <AppAvatar v-if="comment.isAnonymous === 1" :anonymous="true" :size="34" />
        <AppAvatar v-else :src="comment.authorAvatar" :name="comment.authorName" :size="34" />
        <div class="cmt__body">
          <b>{{ comment.isAnonymous === 1 ? '匿名' : comment.authorName || '未知用户' }}</b>
          <span v-if="comment.replyUserName" class="cmt__reply">→ {{ comment.replyUserName }}</span>
          <p>{{ comment.content }}</p>
          <div class="cmt__foot">
            <span>{{ formatTime(comment.createTime) }}</span>
            <button type="button" :class="['cmt__act', { on: comment.liked }]" @click="handleCommentLike(comment)">
              <AppIcon name="like" :size="12" /> {{ comment.likeCount || 0 }}
            </button>
            <button type="button" class="cmt__act" @click="setReplyTarget(comment)">
              <AppIcon name="comment" :size="12" /> 回复
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="comments.length === 0 && !loading" class="dh-state">
      <p>还没有人回响</p>
      <p style="font-size: 13px; margin-top: 6px">来做第一个懂 TA 的人</p>
    </div>
  </div>

  <div v-else-if="loading" class="dh-narrow" style="padding-top: 40px">
    <div class="dh-skeleton" style="height: 260px" />
  </div>

  <div v-else class="dh-state">帖子不存在或已被删除</div>

  <!-- ============ 举报弹窗 ============ -->
  <el-dialog v-model="reportDialogVisible" title="举报" width="440px" :close-on-click-modal="false">
    <el-form label-width="80px">
      <el-form-item label="举报原因">
        <el-select v-model="reportForm.reason" placeholder="请选择举报原因" style="width: 100%">
          <el-option label="色情低俗" value="色情低俗" />
          <el-option label="政治敏感" value="政治敏感" />
          <el-option label="人身攻击" value="人身攻击" />
          <el-option label="广告垃圾" value="广告垃圾" />
          <el-option label="谣言诈骗" value="谣言诈骗" />
          <el-option label="侵犯隐私" value="侵犯隐私" />
          <el-option label="其他" value="其他" />
        </el-select>
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="reportForm.description" type="textarea" :rows="3" placeholder="请描述具体情况…" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="reportDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="reportSubmitting" @click="submitReport">提交举报</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElDialog, ElForm, ElFormItem, ElSelect, ElOption, ElInput, ElButton } from 'element-plus'
import AppIcon from '../components/AppIcon.vue'
import AppAvatar from '../components/AppAvatar.vue'
import {
  getPostDetail, likePost, unlikePost, isPostLiked, getCommentPage, createComment,
  likeComment, unlikeComment,
  submitReport as submitReportApi, toggleCollect, isCollected, deletePost as removePost,
} from '../api/treehole'
import { isLoggedIn, getUserIdFromToken } from '../api/auth'
import type { Post, Comment } from '../api/treehole'

const route = useRoute()
const router = useRouter()
const post = ref<Post | null>(null)
const comments = ref<Comment[]>([])
const commentTotal = ref(0)
const commentContent = ref('')
const commentAnonymous = ref(true)
const liked = ref(false)
const collected = ref(false)
const loading = ref(false)

const initial = computed(() => (post.value?.authorName || '?').trim().charAt(0).toUpperCase() || '?')

// 路由参数可能是非数字（手输地址 / 半截跳转），直接透传会打成 /api/th/post/NaN 触发 500
const postId = computed(() => {
  const n = Number(route.params.id)
  return Number.isInteger(n) && n > 0 ? n : null
})

// 匿名帖不暴露删除入口，避免去匿名化
const isOwner = computed(() => {
  if (!post.value) return false
  if (post.value.isAnonymous === 1) return false
  const myId = getUserIdFromToken()
  return !!myId && !!post.value.userId && myId === post.value.userId
})

// Reply state
const replyTo = ref<number | null>(null)
const replyToName = ref('')
const replyUserId = ref<number | null>(null)

// Report dialog
const reportDialogVisible = ref(false)
const reportSubmitting = ref(false)
const reportForm = ref({ reason: '', description: '' })

async function fetchPost() {
  if (postId.value === null) {
    post.value = null
    loading.value = false
    return
  }
  loading.value = true
  try {
    const res = await getPostDetail(postId.value)
    post.value = res.data
  } catch {
    post.value = null
  } finally {
    loading.value = false
  }
}

async function fetchComments() {
  if (postId.value === null) return
  try {
    const res = await getCommentPage({ pageNum: 1, pageSize: 50, postId: postId.value })
    comments.value = res.data.records || []
    commentTotal.value = res.data.total || 0
  } catch {
    /* ignore */
  }
}

/**
 * 回填"我是否已抱抱"。
 * 这个接口后端一直存在，但前端从没调用过 —— 结果是刷新后状态显示为未点赞，
 * 此时点击走 likePost（服务端幂等直接返回），前端却把 likeCount 又加了一次，数字会越点越偏。
 */
async function fetchLiked() {
  if (!isLoggedIn() || postId.value === null) return
  try {
    const res = await isPostLiked(postId.value)
    liked.value = res.data
  } catch {
    /* 失败时保持未点赞，不影响阅读 */
  }
}

async function fetchCollected() {
  if (!isLoggedIn() || !post.value) return
  try {
    const res = await isCollected(post.value.id)
    collected.value = res.data
  } catch {
    /* 未登录或失败时不展示收藏态 */
  }
}

async function handleLike() {
  if (!post.value) return
  // 未登录时先引导登录：否则会打到接口拿 401，被拦截器登出并弹回登录页
  if (!isLoggedIn()) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  try {
    if (liked.value) {
      await unlikePost(post.value.id)
      post.value.likeCount--
      liked.value = false
    } else {
      await likePost(post.value.id)
      post.value.likeCount++
      liked.value = true
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '操作失败')
  }
}

async function handleCollect() {
  if (!post.value) return
  if (!isLoggedIn()) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  try {
    const res = await toggleCollect(post.value.id)
    collected.value = res.data
    ElMessage.success(res.data ? '已收藏' : '已取消收藏')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

async function handleDeletePost() {
  if (!post.value) return
  try {
    await ElMessageBox.confirm('删除后帖子及其评论、点赞、收藏都会被清除，确定删除？', '删除帖子', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await removePost(post.value.id)
    ElMessage.success('已删除')
    router.push('/')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '删除失败')
  }
}

async function handleCommentLike(comment: Comment) {
  if (!isLoggedIn()) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  // liked 由列表接口回填；据此决定点赞还是取消，数字才不会越点越多
  const wasLiked = comment.liked === true
  try {
    if (wasLiked) {
      await unlikeComment(comment.id)
      comment.likeCount = Math.max((comment.likeCount || 0) - 1, 0)
      comment.liked = false
    } else {
      await likeComment(comment.id)
      comment.likeCount = (comment.likeCount || 0) + 1
      comment.liked = true
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

function setReplyTarget(comment: Comment) {
  replyTo.value = comment.id
  replyToName.value = comment.authorName || '匿名用户'
  replyUserId.value = comment.userId || null
}

function cancelReply() {
  replyTo.value = null
  replyToName.value = ''
  replyUserId.value = null
}

async function handleComment() {
  if (!commentContent.value.trim() || !post.value) return
  if (!isLoggedIn()) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  try {
    await createComment({
      postId: post.value.id,
      content: commentContent.value,
      parentId: replyTo.value || undefined,
      replyUserId: replyUserId.value || undefined,
      isAnonymous: commentAnonymous.value ? 1 : 0,
    })
    commentContent.value = ''
    cancelReply()
    ElMessage.success('已发送回响')
    fetchComments()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '评论失败')
  }
}

function openReportDialog() {
  reportForm.value = { reason: '', description: '' }
  reportDialogVisible.value = true
}

async function submitReport() {
  if (!reportForm.value.reason) {
    ElMessage.warning('请选择举报原因')
    return
  }
  reportSubmitting.value = true
  try {
    await submitReportApi({
      targetType: 'POST',
      targetId: post.value!.id,
      reason: reportForm.value.reason,
      description: reportForm.value.description,
    })
    ElMessage.success('举报已提交，我们会尽快处理')
    reportDialogVisible.value = false
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '举报失败')
  } finally {
    reportSubmitting.value = false
  }
}

function previewImage(src: string) {
  window.open(src, '_blank')
}

function formatTime(time: string) {
  const date = new Date(time)
  const diff = Date.now() - date.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  return date.toLocaleString('zh-CN', { hour12: false })
}

onMounted(() => {
  // 两个用户态都要回填：只用本地布尔值会让刷新后的状态与点赞数都不准
  fetchPost().then(() => {
    fetchLiked()
    fetchCollected()
  })
  fetchComments()
})
</script>

<style scoped>
.detail { padding: 32px 0 80px; }
.back {
  display: inline-flex; align-items: center; gap: 8px; font-size: 13px; color: var(--text-dim);
  padding: 8px 14px; border-radius: 999px; transition: all 0.28s var(--ease);
}
.back:hover { color: var(--text); background: var(--surface); }

.detail__card { margin-top: 18px; padding: 32px 34px; border-radius: var(--r-xl); }
.post__top { display: flex; align-items: center; gap: 10px; margin-bottom: 18px; }
.post__top .dh-tag { margin-left: auto; }
.who { display: flex; flex-direction: column; line-height: 1.25; }
.who b { font-size: 14px; font-weight: 500; }
.who span { font-size: 11.5px; color: var(--text-mute); font-family: var(--font-mono); }

.detail__body { font-size: 17px; line-height: 1.9; white-space: pre-wrap; word-break: break-word; }

.post-images { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-top: 20px; }
.post-image { width: 100%; aspect-ratio: 1; object-fit: cover; border-radius: var(--r-sm); cursor: pointer; transition: transform 0.32s var(--ease); }
.post-image:hover { transform: scale(1.02); }

.actbar { display: flex; gap: 8px; margin-top: 26px; padding-top: 22px; border-top: 1px solid var(--border); flex-wrap: wrap; }
.act {
  display: inline-flex; align-items: center; gap: 8px; padding: 9px 17px; border-radius: 999px;
  font-size: 13px; color: var(--text-dim); border: 1px solid var(--border); transition: all 0.3s var(--ease);
}
.act:hover { color: var(--text); border-color: var(--border-strong); transform: translateY(-2px); }
.act.on { color: var(--accent); border-color: var(--accent-line); background: var(--accent-soft); }
.act--static { border-color: transparent; }
.act--static:hover { transform: none; border-color: transparent; color: var(--text-dim); }
.act--danger:hover { color: var(--danger); border-color: color-mix(in srgb, var(--danger) 45%, transparent); }
.act--end { margin-left: auto; }

.feed { display: grid; gap: 12px; }

.composer { margin-bottom: 18px; padding: 18px 20px; }
.composer__in { width: 100%; min-height: 86px; font-size: 15px; line-height: 1.75; resize: vertical; }
.composer__in::placeholder { color: var(--text-mute); }
.composer__foot { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 14px; flex-wrap: wrap; }
.composer__left { display: flex; align-items: center; gap: 12px; }

.cmt { display: flex; gap: 13px; padding: 18px 20px; }
.cmt__body { min-width: 0; flex: 1; }
.cmt__body b { font-size: 13.5px; font-weight: 500; }
.cmt__reply { font-size: 12px; color: var(--text-mute); margin-left: 6px; }
.cmt__body p { font-size: 14.5px; line-height: 1.72; color: var(--text-dim); margin-top: 5px; white-space: pre-wrap; word-break: break-word; }
.cmt__foot { display: flex; align-items: center; gap: 14px; margin-top: 10px; font-size: 11.5px; color: var(--text-mute); font-family: var(--font-mono); }
.cmt__act { display: inline-flex; align-items: center; gap: 5px; font-size: 11.5px; color: var(--text-mute); transition: color 0.28s var(--ease); font-family: inherit; }
.cmt__act:hover { color: var(--accent); }
.cmt__act.on { color: var(--accent); }

@media (max-width: 720px) {
  .detail__card, .composer { padding: 20px; }
  .act--end { margin-left: 0; }
}
</style>
