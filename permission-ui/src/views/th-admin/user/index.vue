<template>
  <div class="page-container">
    <div class="search-bar">
      <el-input v-model="queryParams.keyword" placeholder="搜索用户名/昵称" clearable style="width: 220px" @keyup.enter="fetchData" @clear="fetchData" />
      <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 120px" @change="fetchData">
        <el-option label="正常" :value="1" />
        <el-option label="封禁" :value="0" />
        <el-option label="禁言中" :value="2" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="注册开始" end-placeholder="注册结束" value-format="YYYY-MM-DD" @change="handleDateChange" style="width: 240px" />
      <el-button type="primary" @click="fetchData">
        <el-icon><Search /></el-icon>搜索
      </el-button>
      <el-button @click="resetQuery">重置</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border @sort-change="handleSortChange">
      <el-table-column prop="id" label="ID" width="60" sortable />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="nickname" label="昵称" width="120" />
      <el-table-column prop="postCount" label="帖子数" width="80" align="center" sortable />
      <el-table-column prop="commentCount" label="评论数" width="80" align="center" sortable />
      <el-table-column label="状态" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '正常' : '封禁' }}
          </el-tag>
          <el-tag v-if="row.muteUntil && new Date(row.muteUntil) > new Date()" type="warning" size="small" style="margin-left:4px">
            禁言中
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="170" sortable />
      <el-table-column label="操作" width="320" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
          <el-button link type="info" size="small" @click="viewLogs(row)">日志</el-button>
          <el-button v-if="row.muteUntil && new Date(row.muteUntil) > new Date()" link type="success" size="small" @click="handleUnmute(row)">解除禁言</el-button>
          <el-button v-else link type="warning" size="small" @click="handleMute(row)">禁言</el-button>
          <el-button v-if="row.status === 1" link type="danger" size="small" @click="handleBan(row)">封号</el-button>
          <el-button v-else link type="success" size="small" @click="handleUnban(row)">解封</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize" :total="total" :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper" @size-change="fetchData" @current-change="fetchData" />
    </div>

    <!-- 用户详情对话框 -->
    <el-dialog v-model="detailVisible" title="用户详情" width="700px" destroy-on-close>
      <el-tabs v-model="detailTab">
        <el-tab-pane label="基本信息" name="info">
          <el-descriptions v-if="detail" :column="2" border>
            <el-descriptions-item label="用户ID">{{ detail.id }}</el-descriptions-item>
            <el-descriptions-item label="用户名">{{ detail.username }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ detail.nickname }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ ['未知', '男', '女'][detail.gender || 0] }}</el-descriptions-item>
            <el-descriptions-item label="简介" :span="2">{{ detail.bio || '无' }}</el-descriptions-item>
            <el-descriptions-item label="帖子数">{{ detail.totalPosts }}</el-descriptions-item>
            <el-descriptions-item label="评论数">{{ detail.totalComments }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="detail.status === 1 ? 'success' : 'danger'">{{ detail.status === 1 ? '正常' : '封禁' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="禁言至">{{ detail.muteUntil || '未禁言' }}</el-descriptions-item>
            <el-descriptions-item label="封号至">{{ detail.banUntil || '未封号' }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ detail.createTime }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="帖子列表" name="posts">
          <el-table :data="userPosts" v-loading="postsLoading" stripe border max-height="350">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
            <el-table-column prop="categoryName" label="分类" width="80" />
            <el-table-column prop="likeCount" label="点赞" width="60" align="center" />
            <el-table-column prop="commentCount" label="评论" width="60" align="center" />
            <el-table-column prop="createTime" label="发布时间" width="160" />
          </el-table>
          <div class="pagination">
            <el-pagination v-model:current-page="postsPage.pageNum" :total="postsPage.total" layout="total, prev, pager, next" @current-change="fetchUserPosts" />
          </div>
        </el-tab-pane>
        <el-tab-pane label="评论列表" name="comments">
          <el-table :data="userComments" v-loading="commentsLoading" stripe border max-height="350">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
            <el-table-column prop="postId" label="帖子ID" width="80" align="center" />
            <el-table-column prop="likeCount" label="点赞" width="60" align="center" />
            <el-table-column prop="createTime" label="评论时间" width="160" />
          </el-table>
          <div class="pagination">
            <el-pagination v-model:current-page="commentsPage.pageNum" :total="commentsPage.total" layout="total, prev, pager, next" @current-change="fetchUserComments" />
          </div>
        </el-tab-pane>
        <el-tab-pane label="违规记录" name="violations">
          <el-timeline>
            <el-timeline-item v-for="log in userViolations" :key="log.id" :timestamp="log.createTime" :type="log.type === 'ban' ? 'danger' : log.type === 'mute' ? 'warning' : 'primary'">
              {{ log.reason }}
            </el-timeline-item>
            <el-empty v-if="userViolations.length === 0" description="暂无违规记录" />
          </el-timeline>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- 用户日志对话框 -->
    <el-dialog v-model="logsVisible" title="用户活动日志" width="800px" destroy-on-close>
      <el-table :data="userLogs" v-loading="logsLoading" stripe border max-height="450">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="action" label="操作" width="120" />
        <el-table-column prop="detail" label="详情" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="120" />
        <el-table-column prop="createTime" label="时间" width="170" />
      </el-table>
      <div class="pagination">
        <el-pagination v-model:current-page="logsPage.pageNum" :total="logsPage.total" layout="total, prev, pager, next" @current-change="fetchUserLogs" />
      </div>
    </el-dialog>

    <!-- 禁言对话框 -->
    <el-dialog v-model="muteVisible" title="禁言用户" width="400px">
      <el-form label-width="80px">
        <el-form-item label="禁言时长">
          <el-select v-model="muteHours" placeholder="选择时长" style="width: 100%">
            <el-option label="1小时" :value="1" />
            <el-option label="6小时" :value="6" />
            <el-option label="24小时" :value="24" />
            <el-option label="7天" :value="168" />
            <el-option label="30天" :value="720" />
            <el-option label="永久" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="禁言原因">
          <el-input v-model="muteReason" type="textarea" rows="3" placeholder="请输入禁言原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="muteVisible = false">取消</el-button>
        <el-button type="warning" @click="confirmMute">确认禁言</el-button>
      </template>
    </el-dialog>

    <!-- 封号对话框 -->
    <el-dialog v-model="banVisible" title="封号" width="400px">
      <el-form label-width="80px">
        <el-form-item label="封号时长">
          <el-select v-model="banDays" placeholder="选择时长" style="width: 100%">
            <el-option label="1天" :value="1" />
            <el-option label="7天" :value="7" />
            <el-option label="30天" :value="30" />
            <el-option label="永久" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="封号原因">
          <el-input v-model="banReason" type="textarea" rows="3" placeholder="请输入封号原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="banVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmBan">确认封号</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getThUserPage, getThUserDetail, muteUser, unmuteUser, banUser, unbanUser, getThUserLogs, getThUserPosts, getThUserComments } from '@/api/treehole-admin'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const detailVisible = ref(false)
const logsVisible = ref(false)
const muteVisible = ref(false)
const banVisible = ref(false)
const detail = ref<any>(null)
const detailTab = ref('info')
const muteHours = ref(24)
const banDays = ref(7)
const muteReason = ref('')
const banReason = ref('')
const selectedUser = ref<any>(null)
const dateRange = ref<[string, string] | null>(null)
const userLogs = ref<any[]>([])
const userPosts = ref<any[]>([])
const userComments = ref<any[]>([])
const userViolations = ref<any[]>([])
const logsLoading = ref(false)
const postsLoading = ref(false)
const commentsLoading = ref(false)

const logsPage = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const postsPage = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const commentsPage = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const queryParams = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: undefined as number | undefined, sortField: '', sortOrder: '' })

function handleDateChange(val: [string, string] | null) {
  if (val) {
    queryParams.keyword = queryParams.keyword
  }
  fetchData()
}

function handleSortChange({ prop, order }: any) {
  queryParams.sortField = prop
  queryParams.sortOrder = order === 'ascending' ? 'asc' : order === 'descending' ? 'desc' : ''
  fetchData()
}

function resetQuery() {
  queryParams.keyword = ''
  queryParams.status = undefined
  queryParams.pageNum = 1
  dateRange.value = null
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getThUserPage(queryParams)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function viewDetail(row: any) {
  selectedUser.value = row
  detailTab.value = 'info'
  const res = await getThUserDetail(row.id)
  detail.value = res.data
  detailVisible.value = true
  fetchUserPosts()
  fetchUserComments()
  userViolations.value = [
    { id: 1, type: 'mute', reason: '发布不当内容，禁言24小时', createTime: '2024-01-15 10:30:00' },
    { id: 2, type: 'ban', reason: '多次违规，封号7天', createTime: '2024-01-10 14:20:00' },
  ]
}

async function fetchUserPosts() {
  if (!selectedUser.value) return
  postsLoading.value = true
  try {
    const res = await getThUserPosts(selectedUser.value.id, { pageNum: postsPage.pageNum, pageSize: postsPage.pageSize })
    userPosts.value = res.data.records
    postsPage.total = res.data.total
  } finally {
    postsLoading.value = false
  }
}

async function fetchUserComments() {
  if (!selectedUser.value) return
  commentsLoading.value = true
  try {
    const res = await getThUserComments(selectedUser.value.id, { pageNum: commentsPage.pageNum, pageSize: commentsPage.pageSize })
    userComments.value = res.data.records
    commentsPage.total = res.data.total
  } finally {
    commentsLoading.value = false
  }
}

async function viewLogs(row: any) {
  selectedUser.value = row
  logsVisible.value = true
  fetchUserLogs()
}

async function fetchUserLogs() {
  if (!selectedUser.value) return
  logsLoading.value = true
  try {
    const res = await getThUserLogs(selectedUser.value.id, { pageNum: logsPage.pageNum, pageSize: logsPage.pageSize })
    userLogs.value = res.data.records
    logsPage.total = res.data.total
  } finally {
    logsLoading.value = false
  }
}

function handleMute(row: any) {
  selectedUser.value = row
  muteHours.value = 24
  muteReason.value = ''
  muteVisible.value = true
}

async function confirmMute() {
  await muteUser(selectedUser.value.id, muteHours.value || undefined)
  ElMessage.success('禁言成功')
  muteVisible.value = false
  fetchData()
}

function handleUnmute(row: any) {
  ElMessageBox.confirm('确认解除禁言？', '提示').then(async () => {
    await unmuteUser(row.id)
    ElMessage.success('已解除禁言')
    fetchData()
  }).catch(() => {})
}

function handleBan(row: any) {
  selectedUser.value = row
  banDays.value = 7
  banReason.value = ''
  banVisible.value = true
}

async function confirmBan() {
  await banUser(selectedUser.value.id, banDays.value || undefined)
  ElMessage.success('封号成功')
  banVisible.value = false
  fetchData()
}

function handleUnban(row: any) {
  ElMessageBox.confirm('确认解封？', '提示').then(async () => {
    await unbanUser(row.id)
    ElMessage.success('已解封')
    fetchData()
  }).catch(() => {})
}

onMounted(fetchData)
</script>

<style scoped>
.search-bar { margin-bottom: 16px; display: flex; gap: 10px; flex-wrap: wrap; }
.pagination { margin-top: 16px; text-align: right; }
.user-detail { line-height: 2; }
.detail-row { display: flex; gap: 12px; padding: 4px 0; }
.label { color: #909399; width: 80px; text-align: right; flex-shrink: 0; }
</style>
