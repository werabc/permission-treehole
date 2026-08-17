<template>
  <div class="detail" v-if="post">
    <div class="post-card">
      <div class="post-header">
        <div class="post-author">
          <span class="author-avatar">{{ post.authorName?.charAt(0) || '?' }}</span>
          <div>
            <span class="author-name">{{ post.authorName || '匿名用户' }}</span>
            <span class="post-time">{{ formatTime(post.createTime) }}</span>
          </div>
        </div>
        <span class="th-tag th-tag-blue">{{ post.categoryName || '树洞' }}</span>
      </div>

      <p class="post-content">{{ post.content }}</p>

      <!-- 图片展示 -->
      <div v-if="post.images && post.images.length > 0" class="post-images">
        <img
          v-for="(img, idx) in post.images"
          :key="idx"
          :src="img"
          class="post-image"
          @click="previewImage(img)"
          alt="帖子图片"
        />
      </div>

      <div class="post-actions">
        <button :class="['action-btn', { liked }]" @click="handleLike">
          👍 {{ post.likeCount }}
        </button>
        <span class="action">👁 {{ post.viewCount }}</span>
        <button class="action-btn report-btn" @click="openReportDialog">
          🚩 举报
        </button>
      </div>
    </div>

    <!-- 评论区域 -->
    <div class="comment-section">
      <h3>评论 ({{ commentTotal }})</h3>

      <div class="comment-input">
        <textarea v-model="commentContent" :placeholder="replyTo ? `回复 ${replyToName}...` : '写下你的评论...'" rows="3"></textarea>
        <div class="comment-input-actions">
          <label class="anonymous-check">
            <input type="checkbox" v-model="commentAnonymous" /> 匿名
          </label>
          <div>
            <button v-if="replyTo" class="th-btn th-btn-ghost btn-sm" @click="cancelReply">取消回复</button>
            <button class="th-btn th-btn-primary btn-sm" @click="handleComment">发表评论</button>
          </div>
        </div>
      </div>

      <div class="comment-list">
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <div class="comment-header">
            <span class="comment-avatar">{{ comment.authorName?.charAt(0) || '?' }}</span>
            <span class="comment-author">{{ comment.authorName || '匿名用户' }}</span>
            <span v-if="comment.replyUserName" class="reply-arrow">
              → {{ comment.replyUserName }}
            </span>
            <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
          </div>
          <p class="comment-content">{{ comment.content }}</p>
          <div class="comment-footer">
            <button class="comment-like-btn" @click="handleCommentLike(comment)">
              👍 {{ comment.likeCount }}
            </button>
            <button class="comment-reply-btn" @click="setReplyTarget(comment)">
              💬 回复
            </button>
          </div>
        </div>
      </div>

      <div v-if="comments.length === 0 && !loading" class="empty">暂无评论，来抢沙发吧！</div>
    </div>
  </div>
  <div v-else class="loading">加载中...</div>

  <!-- 举报对话框 -->
  <el-dialog v-model="reportDialogVisible" title="举报" width="450px" :close-on-click-modal="false">
    <el-form label-width="80px">
      <el-form-item label="举报原因">
        <el-select v-model="reportForm.reason" placeholder="请选择举报原因">
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
        <el-input v-model="reportForm.description" type="textarea" rows="3" placeholder="请描述具体情况..." />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="reportDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="reportSubmitting" @click="submitReport">提交举报</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElDialog, ElForm, ElFormItem, ElSelect, ElOption, ElInput, ElButton } from 'element-plus'
import { getPostDetail, likePost, unlikePost, getCommentPage, createComment, likeComment, submitReport } from '../api/treehole'
import type { Post, Comment } from '../api/treehole'

const route = useRoute()
const post = ref<Post | null>(null)
const comments = ref<Comment[]>([])
const commentTotal = ref(0)
const commentContent = ref('')
const commentAnonymous = ref(false)
const liked = ref(false)
const loading = ref(false)

// Reply state
const replyTo = ref<number | null>(null)
const replyToName = ref('')
const replyUserId = ref<number | null>(null)

// Report dialog
const reportDialogVisible = ref(false)
const reportSubmitting = ref(false)
const reportForm = ref({ reason: '', description: '' })

async function fetchPost() {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const res = await getPostDetail(id)
    post.value = res.data
    if (post.value?.authorName) {
      post.value.authorName = post.value.authorName
    }
  } finally {
    loading.value = false
  }
}

async function fetchComments() {
  const id = Number(route.params.id)
  try {
    const res = await getCommentPage({ pageNum: 1, pageSize: 50, postId: id })
    comments.value = res.data.records
    commentTotal.value = res.data.total
  } catch (e) {
    console.error('加载评论失败:', e)
  }
}

async function handleLike() {
  if (!post.value) return
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
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleCommentLike(comment: Comment) {
  try {
    await likeComment(comment.id)
    comment.likeCount++
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
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
    ElMessage.success('评论成功')
    fetchComments()
  } catch (e: any) {
    ElMessage.error(e.message || '评论失败')
  }
}

function openReportDialog() {
  if (!post.value) return
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
    await submitReport({
      targetType: 'POST',
      targetId: post.value!.id,
      reason: reportForm.value.reason,
      description: reportForm.value.description,
    })
    ElMessage.success('举报提交成功')
    reportDialogVisible.value = false
  } catch (e: any) {
    ElMessage.error(e.message || '举报失败')
  } finally {
    reportSubmitting.value = false
  }
}

function previewImage(src: string) {
  window.open(src, '_blank')
}

function formatTime(time: string) {
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
  fetchPost()
  fetchComments()
})
</script>

<style scoped>
.detail {
  max-width: 700px;
  margin: 0 auto;
}

.post-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.post-author {
  display: flex;
  align-items: center;
  gap: 10px;
}

.author-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}

.author-name {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
}

.post-time {
  font-size: 12px;
  color: #94a3b8;
  margin-left: 8px;
}

.post-content {
  font-size: 16px;
  color: #1e293b;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
  margin-bottom: 16px;
}

.post-images {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 16px;
}

.post-image {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.2s;
}

.post-image:hover {
  transform: scale(1.02);
}

.post-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: 20px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 13px;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #e2e8f0;
}

.action-btn.liked {
  background: #dbeafe;
  color: #3b82f6;
}

.report-btn {
  margin-left: auto;
  color: #ef4444;
  background: #fef2f2;
}

.report-btn:hover {
  background: #fee2e2;
}

.action {
  font-size: 13px;
  color: #64748b;
}

.comment-section {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.comment-section h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.comment-input textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  margin-bottom: 8px;
  resize: vertical;
  font-family: inherit;
}

.comment-input textarea:focus {
  border-color: #3b82f6;
  outline: none;
}

.comment-input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.anonymous-check {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #64748b;
}

.comment-list {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-item {
  padding: 12px 14px;
  background: #f8fafc;
  border-radius: 10px;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.comment-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
}

.comment-author {
  font-size: 13px;
  font-weight: 500;
  color: #334155;
}

.reply-arrow {
  font-size: 12px;
  color: #94a3b8;
}

.comment-time {
  font-size: 12px;
  color: #94a3b8;
  margin-left: auto;
}

.comment-content {
  font-size: 14px;
  color: #334155;
  white-space: pre-wrap;
  word-break: break-word;
  margin-bottom: 8px;
}

.comment-footer {
  display: flex;
  gap: 12px;
}

.comment-like-btn,
.comment-reply-btn {
  font-size: 12px;
  color: #94a3b8;
  background: none;
  cursor: pointer;
  transition: color 0.2s;
}

.comment-like-btn:hover,
.comment-reply-btn:hover {
  color: #3b82f6;
}

.empty {
  text-align: center;
  padding: 30px;
  color: #94a3b8;
  font-size: 14px;
}

.loading {
  text-align: center;
  padding: 60px;
  color: #94a3b8;
}

.btn-sm {
  padding: 6px 14px;
  font-size: 13px;
}
</style>
