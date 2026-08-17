<template>
  <div class="page-container">
    <div class="search-bar">
      <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 120px" @change="fetchData">
        <el-option label="全部" :value="undefined" />
        <el-option label="已通过" :value="1" />
        <el-option label="待审核" :value="0" />
        <el-option label="已拒绝" :value="2" />
      </el-select>
      <el-select v-model="queryParams.categoryId" placeholder="分类" clearable style="width: 120px" @change="fetchData">
        <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
      </el-select>
      <el-input v-model="queryParams.keyword" placeholder="搜索内容/作者" clearable style="width: 220px" @keyup.enter="fetchData" @clear="fetchData" />
      <el-button type="primary" @click="fetchData">
        <el-icon><Search /></el-icon>搜索
      </el-button>
      <el-button @click="resetQuery">重置</el-button>
    </div>

    <!-- 批量操作栏 -->
    <div class="batch-bar">
      <el-button type="success" :disabled="selectedRows.length === 0" @click="handleBatchAudit(1)">
        <el-icon><Check /></el-icon>批量通过
      </el-button>
      <el-button type="danger" :disabled="selectedRows.length === 0" @click="handleBatchAudit(2)">
        <el-icon><Close /></el-icon>批量拒绝
      </el-button>
      <el-button type="warning" :disabled="selectedRows.length === 0" @click="handleBatchPin(1)">
        <el-icon><Top /></el-icon>批量置顶
      </el-button>
      <el-button type="info" :disabled="selectedRows.length === 0" @click="handleBatchPin(0)">批量取消置顶</el-button>
      <span class="selected-count" v-if="selectedRows.length > 0">已选择 {{ selectedRows.length }} 项</span>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border @selection-change="selectedRows = $event" @sort-change="handleSortChange">
      <el-table-column type="selection" width="40" />
      <el-table-column prop="id" label="ID" width="60" sortable />
      <el-table-column prop="content" label="内容" min-width="250" show-overflow-tooltip />
      <el-table-column prop="authorName" label="作者" width="100" />
      <el-table-column prop="categoryName" label="分类" width="80" />
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="置顶" width="60" align="center">
        <template #default="{ row }">
          <el-icon v-if="row.isTop === 1" color="#e6a23c"><Top /></el-icon>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览" width="60" align="center" sortable />
      <el-table-column prop="likeCount" label="点赞" width="60" align="center" sortable />
      <el-table-column prop="commentCount" label="评论" width="60" align="center" sortable />
      <el-table-column prop="createTime" label="发布时间" width="170" sortable />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
          <el-button v-if="row.status === 0" link type="success" size="small" @click="handleAudit(row, 1)">通过</el-button>
          <el-button v-if="row.status === 0" link type="danger" size="small" @click="handleAudit(row, 2)">拒绝</el-button>
          <el-button v-if="row.isTop === 1" link type="warning" size="small" @click="handlePin(row, 0)">取消置顶</el-button>
          <el-button v-else link type="warning" size="small" @click="handlePin(row, 1)">置顶</el-button>
          <el-button v-if="row.status === 1" link type="info" size="small" @click="handleHide(row, 0)">隐藏</el-button>
          <el-button v-else link type="success" size="small" @click="handleHide(row, 1)">恢复</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize" :total="total" :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper" @size-change="fetchData" @current-change="fetchData" />
    </div>

    <!-- 帖子详情对话框 -->
    <el-dialog v-model="detailVisible" title="帖子详情" width="650px" destroy-on-close>
      <div v-if="detail" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="帖子ID">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="作者">{{ detail.authorName }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ detail.categoryName }}</el-descriptions-item>
          <el-descriptions-item label="IP">{{ detail.ip }}</el-descriptions-item>
          <el-descriptions-item label="浏览">{{ detail.viewCount }}</el-descriptions-item>
          <el-descriptions-item label="点赞">{{ detail.likeCount }}</el-descriptions-item>
          <el-descriptions-item label="评论">{{ detail.commentCount }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(detail.status)">{{ statusLabel(detail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="发布时间" :span="2">{{ detail.createTime }}</el-descriptions-item>
          <el-descriptions-item label="审核备注" :span="2">{{ detail.auditRemark || '无' }}</el-descriptions-item>
        </el-descriptions>
        <div class="content-section">
          <strong>帖子内容：</strong>
          <div class="content-box">{{ detail.content }}</div>
        </div>
      </div>
      <template #footer>
        <el-button v-if="detail && detail.status === 0" type="success" @click="handleAudit(detail, 1); detailVisible = false">通过</el-button>
        <el-button v-if="detail && detail.status === 0" type="danger" @click="handleAudit(detail, 2); detailVisible = false">拒绝</el-button>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 审核备注对话框 -->
    <el-dialog v-model="auditVisible" title="审核操作" width="450px">
      <el-form label-width="80px">
        <el-form-item label="操作">
          <el-tag :type="auditStatus === 1 ? 'success' : 'danger'">{{ auditStatus === 1 ? '通过' : '拒绝' }}</el-tag>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="auditRemark" type="textarea" rows="3" placeholder="请输入审核备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAudit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Top, Search, Check, Close } from '@element-plus/icons-vue'
import { getThPostPage, getThPostDetail, pinPost, hidePost, deleteThPost, batchAudit } from '@/api/treehole-admin'
import { getThCategoryList } from '@/api/treehole-admin'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const detailVisible = ref(false)
const auditVisible = ref(false)
const detail = ref<any>(null)
const selectedRows = ref<any[]>([])
const categories = ref<any[]>([])
const auditStatus = ref(1)
const auditRemark = ref('')
const currentRow = ref<any>(null)

const queryParams = reactive({ pageNum: 1, pageSize: 10, status: undefined as number | undefined, categoryId: undefined as number | undefined, keyword: '', sortField: '', sortOrder: '' })

function statusLabel(s: number) { return ['待审核', '已通过', '已拒绝'][s] || '-' }
function statusType(s: number) { return ['warning', 'success', 'danger'][s] || 'info' }

function handleSortChange({ prop, order }: any) {
  queryParams.sortField = prop
  queryParams.sortOrder = order === 'ascending' ? 'asc' : order === 'descending' ? 'desc' : ''
  fetchData()
}

function resetQuery() {
  queryParams.status = undefined
  queryParams.categoryId = undefined
  queryParams.keyword = ''
  queryParams.pageNum = 1
  fetchData()
}

async function fetchCategories() {
  try {
    const res = await getThCategoryList()
    categories.value = res.data
  } catch (e) {
    console.error('Failed to fetch categories:', e)
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getThPostPage(queryParams)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

async function viewDetail(row: any) {
  const res = await getThPostDetail(row.id)
  detail.value = res.data
  detailVisible.value = true
}

function handleAudit(row: any, status: number) {
  currentRow.value = row
  auditStatus.value = status
  auditRemark.value = ''
  auditVisible.value = true
}

async function confirmAudit() {
  await batchAudit({ type: 'post', ids: [currentRow.value.id], status: auditStatus.value })
  ElMessage.success(auditStatus.value === 1 ? '已通过' : '已拒绝')
  auditVisible.value = false
  fetchData()
}

async function handleBatchAudit(status: number) {
  const ids = selectedRows.value.map((r: any) => r.id)
  if (ids.length === 0) return
  await ElMessageBox.confirm(`确认批量${status === 1 ? '通过' : '拒绝'}选中的 ${ids.length} 个帖子？`, '批量操作')
  await batchAudit({ type: 'post', ids, status })
  ElMessage.success('批量操作成功')
  fetchData()
}

async function handlePin(row: any, isTop: number) {
  await pinPost(row.id, isTop)
  ElMessage.success(isTop === 1 ? '已置顶' : '已取消置顶')
  fetchData()
}

async function handleBatchPin(isTop: number) {
  const ids = selectedRows.value.map((r: any) => r.id)
  if (ids.length === 0) return
  for (const id of ids) {
    await pinPost(id, isTop)
  }
  ElMessage.success(isTop === 1 ? '批量置顶成功' : '批量取消置顶成功')
  fetchData()
}

async function handleHide(row: any, status: number) {
  await hidePost(row.id, status)
  ElMessage.success(status === 1 ? '已恢复' : '已隐藏')
  fetchData()
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确认删除此帖子？此操作不可恢复！', '删除警告', { type: 'warning' })
  await deleteThPost(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(() => { fetchData(); fetchCategories() })
</script>

<style scoped>
.search-bar { margin-bottom: 12px; display: flex; gap: 10px; flex-wrap: wrap; }
.batch-bar { margin-bottom: 12px; display: flex; gap: 10px; align-items: center; }
.selected-count { color: #409eff; font-size: 13px; }
.pagination { margin-top: 16px; text-align: right; }
.detail-content { line-height: 1.8; }
.content-section { margin-top: 16px; }
.content-box { background: #f8fafc; padding: 12px; border-radius: 8px; margin: 8px 0; white-space: pre-wrap; line-height: 1.6; }
</style>
