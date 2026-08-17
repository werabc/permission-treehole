<template>
  <div class="page-container">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="8">
        <div class="stat-card" style="background: linear-gradient(135deg, #e6a23c, #f56c6c)">
          <div class="stat-label">待审核帖子</div>
          <div class="stat-value">{{ moderationStats.pendingPosts ?? '-' }}</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card" style="background: linear-gradient(135deg, #409eff, #4facfe)">
          <div class="stat-label">待审核评论</div>
          <div class="stat-value">{{ moderationStats.pendingComments ?? '-' }}</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card" style="background: linear-gradient(135deg, #67c23a, #43e97b)">
          <div class="stat-label">今日已审核</div>
          <div class="stat-value">{{ moderationStats.todayAudited ?? '-' }}</div>
        </div>
      </el-col>
    </el-row>

    <el-tabs v-model="activeTab" style="margin-top: 20px">
      <el-tab-pane label="待审核帖子" name="posts">
        <div class="search-bar">
          <el-button type="success" :disabled="selectedPosts.length === 0" @click="batchAudit('posts', 1)">
            <el-icon><Check /></el-icon>批量通过
          </el-button>
          <el-button type="danger" :disabled="selectedPosts.length === 0" @click="batchAudit('posts', 2)">
            <el-icon><Close /></el-icon>批量拒绝
          </el-button>
          <el-button type="warning" :disabled="selectedPosts.length === 0" @click="batchAudit('posts', 3)">
            <el-icon><Delete /></el-icon>批量删除
          </el-button>
          <span class="selected-count" v-if="selectedPosts.length > 0">已选择 {{ selectedPosts.length }} 项</span>
        </div>
        <el-table :data="postList" v-loading="loading" stripe border @selection-change="selectedPosts = $event.map((e: any) => e.id)">
          <el-table-column type="selection" width="40" />
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
          <el-table-column prop="authorName" label="作者" width="100" />
          <el-table-column prop="categoryName" label="分类" width="100" />
          <el-table-column prop="viewCount" label="浏览" width="60" align="center" />
          <el-table-column prop="createTime" label="发布时间" width="170" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="success" size="small" @click="auditPost(row.id, 1)">通过</el-button>
              <el-button link type="danger" size="small" @click="openRejectDialog(row)">拒绝</el-button>
              <el-button link type="primary" size="small" @click="viewPostDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination">
          <el-pagination v-model:current-page="postPage.pageNum" v-model:page-size="postPage.pageSize" :total="postPage.total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @size-change="fetchPosts" @current-change="fetchPosts" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="待审核评论" name="comments">
        <div class="search-bar">
          <el-button type="success" :disabled="selectedComments.length === 0" @click="batchAudit('comments', 1)">
            <el-icon><Check /></el-icon>批量通过
          </el-button>
          <el-button type="danger" :disabled="selectedComments.length === 0" @click="batchAudit('comments', 2)">
            <el-icon><Close /></el-icon>批量拒绝
          </el-button>
          <span class="selected-count" v-if="selectedComments.length > 0">已选择 {{ selectedComments.length }} 项</span>
        </div>
        <el-table :data="commentList" v-loading="loading" stripe border @selection-change="selectedComments = $event.map((e: any) => e.id)">
          <el-table-column type="selection" width="40" />
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
          <el-table-column prop="authorName" label="评论人" width="100" />
          <el-table-column prop="postId" label="帖子ID" width="80" align="center" />
          <el-table-column prop="createTime" label="评论时间" width="170" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="success" size="small" @click="auditComment(row.id, 1)">通过</el-button>
              <el-button link type="danger" size="small" @click="openRejectDialog(row, 'comment')">拒绝</el-button>
              <el-button link type="primary" size="small" @click="viewCommentDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination">
          <el-pagination v-model:current-page="commentPage.pageNum" v-model:page-size="commentPage.pageSize" :total="commentPage.total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @size-change="fetchComments" @current-change="fetchComments" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 拒绝备注对话框 -->
    <el-dialog v-model="rejectVisible" title="拒绝原因" width="450px">
      <el-form label-width="80px">
        <el-form-item label="原因">
          <el-input v-model="rejectRemark" type="textarea" rows="3" placeholder="请输入拒绝原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>

    <!-- 帖子详情对话框 -->
    <el-dialog v-model="postDetailVisible" title="帖子详情" width="600px" destroy-on-close>
      <div v-if="currentPost" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="帖子ID">{{ currentPost.id }}</el-descriptions-item>
          <el-descriptions-item label="作者">{{ currentPost.authorName }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ currentPost.categoryName }}</el-descriptions-item>
          <el-descriptions-item label="IP">{{ currentPost.ip }}</el-descriptions-item>
          <el-descriptions-item label="浏览">{{ currentPost.viewCount }}</el-descriptions-item>
          <el-descriptions-item label="点赞">{{ currentPost.likeCount }}</el-descriptions-item>
          <el-descriptions-item label="评论">{{ currentPost.commentCount }}</el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ currentPost.createTime }}</el-descriptions-item>
        </el-descriptions>
        <div class="content-section">
          <strong>帖子内容：</strong>
          <div class="content-box">{{ currentPost.content }}</div>
        </div>
      </div>
      <template #footer>
        <el-button type="success" @click="auditPost(currentPost.id, 1); postDetailVisible = false">通过</el-button>
        <el-button type="danger" @click="openRejectDialog(currentPost); postDetailVisible = false">拒绝</el-button>
        <el-button @click="postDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 评论详情对话框 -->
    <el-dialog v-model="commentDetailVisible" title="评论详情" width="550px" destroy-on-close>
      <div v-if="currentComment" class="detail-content">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="评论ID">{{ currentComment.id }}</el-descriptions-item>
          <el-descriptions-item label="评论人">{{ currentComment.authorName }}</el-descriptions-item>
          <el-descriptions-item label="帖子ID">{{ currentComment.postId }}</el-descriptions-item>
          <el-descriptions-item label="IP">{{ currentComment.ip }}</el-descriptions-item>
          <el-descriptions-item label="点赞">{{ currentComment.likeCount }}</el-descriptions-item>
          <el-descriptions-item label="评论时间">{{ currentComment.createTime }}</el-descriptions-item>
        </el-descriptions>
        <div class="content-section">
          <strong>评论内容：</strong>
          <div class="content-box">{{ currentComment.content }}</div>
        </div>
      </div>
      <template #footer>
        <el-button type="success" @click="auditComment(currentComment.id, 1); commentDetailVisible = false">通过</el-button>
        <el-button type="danger" @click="openRejectDialog(currentComment, 'comment'); commentDetailVisible = false">拒绝</el-button>
        <el-button @click="commentDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close, Delete } from '@element-plus/icons-vue'
import { getPendingPosts, getPendingComments, batchAudit, getModerationStats, getThPostDetail, getThCommentDetail } from '@/api/treehole-admin'

const activeTab = ref('posts')
const loading = ref(false)
const selectedPosts = ref<number[]>([])
const selectedComments = ref<number[]>([])
const postList = ref<any[]>([])
const commentList = ref<any[]>([])
const postPage = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const commentPage = reactive({ pageNum: 1, pageSize: 20, total: 0 })
const moderationStats = ref<any>({})
const rejectVisible = ref(false)
const rejectRemark = ref('')
const rejectTarget = ref<any>(null)
const rejectType = ref('post')
const postDetailVisible = ref(false)
const commentDetailVisible = ref(false)
const currentPost = ref<any>(null)
const currentComment = ref<any>(null)

async function fetchStats() {
  try {
    const res = await getModerationStats()
    moderationStats.value = res.data
  } catch (e) {
    console.error('Failed to fetch moderation stats:', e)
  }
}

async function fetchPosts() {
  loading.value = true
  try {
    const res = await getPendingPosts({ pageNum: postPage.pageNum, pageSize: postPage.pageSize })
    postList.value = res.data.records
    postPage.total = res.data.total
  } finally { loading.value = false }
}

async function fetchComments() {
  loading.value = true
  try {
    const res = await getPendingComments({ pageNum: commentPage.pageNum, pageSize: commentPage.pageSize })
    commentList.value = res.data.records
    commentPage.total = res.data.total
  } finally { loading.value = false }
}

async function auditPost(id: number, status: number) {
  await batchAudit({ type: 'post', ids: [id], status })
  ElMessage.success(status === 1 ? '已通过' : '已拒绝')
  fetchPosts()
  fetchStats()
}

async function auditComment(id: number, status: number) {
  await batchAudit({ type: 'comment', ids: [id], status })
  ElMessage.success(status === 1 ? '已通过' : '已拒绝')
  fetchComments()
  fetchStats()
}

async function batchAuditFn(type: string, status: number) {
  const ids = type === 'posts' ? selectedPosts.value : selectedComments.value
  if (ids.length === 0) return
  await ElMessageBox.confirm(`确认批量${status === 1 ? '通过' : status === 2 ? '拒绝' : '删除'}选中的 ${ids.length} 项？`, '批量操作')
  await batchAudit({ type, ids, status })
  ElMessage.success('批量操作成功')
  if (type === 'posts') fetchPosts()
  else fetchComments()
  fetchStats()
}

function openRejectDialog(row: any, type = 'post') {
  rejectTarget.value = row
  rejectType.value = type
  rejectRemark.value = ''
  rejectVisible.value = true
}

async function confirmReject() {
  const status = 2
  if (rejectType.value === 'post') {
    await batchAudit({ type: 'post', ids: [rejectTarget.value.id], status })
  } else {
    await batchAudit({ type: 'comment', ids: [rejectTarget.value.id], status })
  }
  ElMessage.success('已拒绝')
  rejectVisible.value = false
  fetchPosts()
  fetchComments()
  fetchStats()
}

async function viewPostDetail(row: any) {
  const res = await getThPostDetail(row.id)
  currentPost.value = res.data
  postDetailVisible.value = true
}

async function viewCommentDetail(row: any) {
  const res = await getThCommentDetail(row.id)
  currentComment.value = res.data
  commentDetailVisible.value = true
}

onMounted(() => { fetchPosts(); fetchComments(); fetchStats() })
</script>

<style scoped>
.stats-row { margin-bottom: 0; }
.stat-card { border-radius: 12px; padding: 20px; color: #fff; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.stat-label { font-size: 13px; opacity: 0.85; margin-bottom: 4px; }
.stat-value { font-size: 28px; font-weight: 600; }
.search-bar { margin-bottom: 16px; display: flex; gap: 10px; align-items: center; }
.selected-count { color: #409eff; font-size: 13px; }
.pagination { margin-top: 16px; text-align: right; }
.detail-content { line-height: 1.8; }
.content-section { margin-top: 16px; }
.content-box { background: #f8fafc; padding: 12px; border-radius: 8px; margin: 8px 0; white-space: pre-wrap; line-height: 1.6; }
</style>
