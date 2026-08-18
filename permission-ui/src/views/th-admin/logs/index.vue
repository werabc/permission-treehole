<template>
  <div class="page-container">
    <div class="search-bar">
      <el-input v-model="queryParams.keyword" placeholder="搜索操作/模块" clearable style="width: 200px" @keyup.enter="fetchData" @clear="fetchData" />
      <el-select v-model="queryParams.module" placeholder="操作模块" clearable style="width: 140px" @change="fetchData">
        <el-option label="全部" value="" />
        <el-option label="帖子管理" value="帖子管理" />
        <el-option label="评论管理" value="评论管理" />
        <el-option label="内容审核" value="内容审核" />
        <el-option label="举报管理" value="举报管理" />
        <el-option label="分类管理" value="分类管理" />
        <el-option label="树洞帖子" value="树洞帖子" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" @change="handleDateChange" style="width: 240px" />
      <el-button type="primary" @click="fetchData">
        <el-icon><Search /></el-icon>搜索
      </el-button>
      <el-button @click="resetQuery">重置</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border style="margin-top: 20px">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="module" label="操作模块" width="100">
        <template #default="{ row }">
          <el-tag :type="moduleType(row.module)" size="small">{{ row.module }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="action" label="操作" width="120" />
      <el-table-column prop="operator" label="操作人" width="100" />
      <el-table-column prop="operatorIp" label="IP地址" width="120" />
      <el-table-column prop="executeTime" label="耗时(ms)" width="80" align="center">
        <template #default="{ row }">
          <span :class="row.executeTime > 1000 ? 'text-danger' : row.executeTime > 500 ? 'text-warning' : 'text-success'">
            {{ row.executeTime }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="70" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="操作时间" width="170" />
      <el-table-column label="详情" width="80" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize" :total="total" :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper" @size-change="fetchData" @current-change="fetchData" />
    </div>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="操作日志详情" width="700px" destroy-on-close>
      <div v-if="detail" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="操作模块">{{ detail.module }}</el-descriptions-item>
          <el-descriptions-item label="操作类型">{{ detail.action }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{ detail.operator }}</el-descriptions-item>
          <el-descriptions-item label="操作IP">{{ detail.operatorIp }}</el-descriptions-item>
          <el-descriptions-item label="执行耗时">{{ detail.executeTime }}ms</el-descriptions-item>
          <el-descriptions-item label="操作状态">
            <el-tag :type="detail.status === 1 ? 'success' : 'danger'">{{ detail.status === 1 ? '成功' : '失败' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="操作时间" :span="2">{{ detail.createTime }}</el-descriptions-item>
        </el-descriptions>
        <div class="content-section">
          <strong>请求URL：</strong>
          <div class="content-box">{{ detail.requestUrl || '-' }}</div>
        </div>
        <div class="content-section">
          <strong>请求参数：</strong>
          <div class="content-box">{{ detail.requestParams || '无' }}</div>
        </div>
        <div v-if="detail.errorMsg" class="content-section">
          <strong>错误信息：</strong>
          <div class="content-box error-box">{{ detail.errorMsg }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { getOperationLogPage } from '@/api/log'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const detailVisible = ref(false)
const detail = ref<any>(null)
const dateRange = ref<[string, string] | null>(null)

const queryParams = reactive({ pageNum: 1, pageSize: 10, keyword: '', module: '', startDate: '', endDate: '' })

function moduleType(m: string) {
  return { '帖子管理': 'success', '评论管理': 'info', '内容审核': 'warning', '举报管理': 'danger', '分类管理': 'primary', '树洞帖子': '' }[m] || 'info'
}

function handleDateChange(val: [string, string] | null) {
  if (val) {
    queryParams.startDate = val[0]
    queryParams.endDate = val[1]
  } else {
    queryParams.startDate = ''
    queryParams.endDate = ''
  }
  fetchData()
}

function resetQuery() {
  queryParams.keyword = ''
  queryParams.module = ''
  queryParams.startDate = ''
  queryParams.endDate = ''
  dateRange.value = null
  queryParams.pageNum = 1
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getOperationLogPage(queryParams)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function viewDetail(row: any) {
  detail.value = row
  detailVisible.value = true
}

onMounted(fetchData)
</script>

<style scoped>
.search-bar { margin-bottom: 16px; display: flex; gap: 10px; flex-wrap: wrap; }
.pagination { margin-top: 16px; text-align: right; }
.detail-content { line-height: 1.8; }
.content-section { margin-top: 16px; }
.content-box { background: #f8fafc; padding: 12px; border-radius: 8px; margin: 8px 0; white-space: pre-wrap; line-height: 1.6; font-size: 13px; max-height: 200px; overflow-y: auto; }
.error-box { background: #fef0f0; color: #f56c6c; }
.text-success { color: #67c23a; }
.text-warning { color: #e6a23c; }
.text-danger { color: #f56c6c; }
</style>
