<template>
  <div class="page-container">
    <div class="search-bar">
      <el-input v-model="queryParams.postId" placeholder="帖子ID" clearable style="width: 150px" @keyup.enter="fetchData" @clear="fetchData" type="number" />
      <el-input v-model="keyword" placeholder="搜索评论内容/评论人" clearable style="width: 220px" @keyup.enter="fetchData" @clear="fetchData" />
      <el-select v-model="visibilityFilter" placeholder="可见性" clearable style="width: 120px" @change="fetchData">
        <el-option label="全部" :value="undefined" />
        <el-option label="可见" :value="1" />
        <el-option label="已隐藏" :value="0" />
      </el-select>
      <el-button type="primary" @click="fetchData">
        <el-icon><Search /></el-icon>搜索
      </el-button>
      <el-button @click="resetQuery">重置</el-button>
    </div>

    <!-- 批量操作栏 -->
    <div class="batch-bar">
      <el-button type="warning" :disabled="selectedRows.length === 0" @click="handleBatchHide">
        <el-icon><Hide /></el-icon>批量隐藏
      </el-button>
      <el-button type="success" :disabled="selectedRows.length === 0" @click="handleBatchShow">
        <el-icon><View /></el-icon>批量显示
      </el-button>
      <el-button type="danger" :disabled="selectedRows.length === 0" @click="handleBatchDelete">
        <el-icon><Delete /></el-icon>批量删除
      </el-button>
      <span class="selected-count" v-if="selectedRows.length > 0">已选择 {{ selectedRows.length }} 项</span>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border @selection-change="selectedRows = $event">
      <el-table-column type="selection" width="40" />
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="content" label="评论内容" min-width="250" show-overflow-tooltip />
      <el-table-column prop="authorName" label="评论人" width="100" />
      <el-table-column prop="postId" label="帖子ID" width="80" align="center">
        <template #default="{ row }">
          <el-link type="primary" @click="queryParams.postId = row.postId; fetchData()">#{{ row.postId }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="likeCount" label="点赞" width="60" align="center" />
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.hidden === 0 ? 'success' : 'info'" size="small">{{ row.hidden === 0 ? '可见' : '已隐藏' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="评论时间" width="170" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
          <el-button v-if="row.hidden === 0" link type="warning" size="small" @click="handleHide(row, 1)">隐藏</el-button>
          <el-button v-else link type="success" size="small" @click="handleHide(row, 0)">显示</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize" :total="total" :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper" @size-change="fetchData" @current-change="fetchData" />
    </div>

    <!-- 评论详情对话框 -->
    <el-dialog v-model="detailVisible" title="评论详情" width="550px" destroy-on-close>
      <div v-if="detail" class="detail-content">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="评论ID">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="评论人">{{ detail.authorName }}</el-descriptions-item>
          <el-descriptions-item label="原帖内容">{{ detail.postContent || '-' }}</el-descriptions-item>
          <el-descriptions-item label="IP">{{ detail.ip }}</el-descriptions-item>
          <el-descriptions-item label="点赞">{{ detail.likeCount }}</el-descriptions-item>
          <el-descriptions-item label="评论时间">{{ detail.createTime }}</el-descriptions-item>
        </el-descriptions>
        <div class="content-section">
          <strong>评论内容：</strong>
          <div class="content-box">{{ detail.content }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Hide, View, Delete } from '@element-plus/icons-vue'
import { getThCommentPage, getThCommentDetail, hideThComment, deleteThComment } from '@/api/treehole-admin'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const detailVisible = ref(false)
const detail = ref<any>(null)
const selectedRows = ref<any[]>([])
const keyword = ref('')
const visibilityFilter = ref<number | undefined>(undefined)

const queryParams = reactive({ pageNum: 1, pageSize: 10, postId: undefined as number | undefined })

function resetQuery() {
  queryParams.postId = undefined
  keyword.value = ''
  visibilityFilter.value = undefined
  queryParams.pageNum = 1
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getThCommentPage(queryParams)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

async function viewDetail(row: any) {
  const res = await getThCommentDetail(row.id)
  detail.value = res.data
  detailVisible.value = true
}

async function handleHide(row: any, status: number) {
  await hideThComment(row.id, status)
  ElMessage.success(status === 1 ? '已隐藏' : '已显示')
  fetchData()
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确认删除此评论？此操作不可恢复！', '删除警告', { type: 'warning' })
  await deleteThComment(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

async function handleBatchHide() {
  const ids = selectedRows.value.map((r: any) => r.id)
  if (ids.length === 0) return
  await ElMessageBox.confirm(`确认批量隐藏选中的 ${ids.length} 条评论？`, '批量操作')
  for (const id of ids) {
    await hideThComment(id, 1)
  }
  ElMessage.success('批量隐藏成功')
  fetchData()
}

async function handleBatchShow() {
  const ids = selectedRows.value.map((r: any) => r.id)
  if (ids.length === 0) return
  await ElMessageBox.confirm(`确认批量显示选中的 ${ids.length} 条评论？`, '批量操作')
  for (const id of ids) {
    await hideThComment(id, 0)
  }
  ElMessage.success('批量显示成功')
  fetchData()
}

async function handleBatchDelete() {
  const ids = selectedRows.value.map((r: any) => r.id)
  if (ids.length === 0) return
  await ElMessageBox.confirm(`确认批量删除选中的 ${ids.length} 条评论？此操作不可恢复！`, '批量删除', { type: 'warning' })
  for (const id of ids) {
    await deleteThComment(id)
  }
  ElMessage.success('批量删除成功')
  fetchData()
}

onMounted(fetchData)
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
