<template>
  <div class="page-container">
    <div class="search-bar">
      <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 120px" @change="fetchData">
        <el-option label="全部" :value="undefined" />
        <el-option label="待审核" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已拒绝" :value="2" />
      </el-select>
      <el-button type="primary" @click="fetchData">搜索</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
      <el-table-column prop="categoryName" label="分类" width="100" />
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览" width="60" align="center" />
      <el-table-column prop="likeCount" label="点赞" width="60" align="center" />
      <el-table-column prop="commentCount" label="评论" width="60" align="center" />
      <el-table-column prop="createTime" label="发布时间" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" link type="success" size="small" @click="handleAudit(row, 1)">通过</el-button>
          <el-button v-if="row.status === 0" link type="warning" size="small" @click="handleAudit(row, 2)">拒绝</el-button>
          <el-popconfirm title="确认删除？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button link type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="fetchData"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminPostPage, auditPost, deletePost } from '@/api/treehole-admin'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const queryParams = reactive({ pageNum: 1, pageSize: 10, status: undefined as number | undefined })

async function fetchData() {
  loading.value = true
  try {
    const res = await getAdminPostPage(queryParams)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function statusType(s: number) { return ['', 'success', 'danger'][s] || 'info' }
function statusLabel(s: number) { return ['待审核', '已通过', '已拒绝'][s] || '-' }

async function handleAudit(row: any, status: number) {
  await auditPost(row.id, status)
  ElMessage.success(status === 1 ? '已通过' : '已拒绝')
  fetchData()
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除此帖子？', '删除')
  await deletePost(id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.search-bar { margin-bottom: 16px; }
.pagination { margin-top: 16px; text-align: right; }
</style>
