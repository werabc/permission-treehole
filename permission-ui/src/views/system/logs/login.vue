<template>
  <div class="page-container">
    <div class="search-bar">
      <el-input v-model="queryParams.keyword" placeholder="搜索用户名/IP" clearable style="width: 200px" @keyup.enter="fetchData" @clear="fetchData" />
      <el-select v-model="statusFilter" placeholder="登录状态" clearable style="width: 120px" @change="fetchData">
        <el-option label="全部" :value="undefined" />
        <el-option label="成功" :value="1" />
        <el-option label="失败" :value="0" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" @change="handleDateChange" style="width: 240px" />
      <el-button type="primary" @click="fetchData">
        <el-icon><Search /></el-icon>搜索
      </el-button>
      <el-button @click="resetQuery">重置</el-button>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #409eff, #4facfe)">
          <div class="stat-label">今日登录</div>
          <div class="stat-value">{{ todayLogins }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #67c23a, #43e97b)">
          <div class="stat-label">登录成功</div>
          <div class="stat-value">{{ successLogins }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #f56c6c, #e6a23c)">
          <div class="stat-label">登录失败</div>
          <div class="stat-value">{{ failLogins }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #909399, #b0b3b8)">
          <div class="stat-label">独立用户</div>
          <div class="stat-value">{{ uniqueUsers }}</div>
        </div>
      </el-col>
    </el-row>

    <el-table :data="tableData" v-loading="loading" stripe border style="margin-top: 20px">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="ip" label="IP地址" width="130" />
      <el-table-column prop="location" label="登录地点" width="150" />
      <el-table-column prop="browser" label="浏览器" width="120" show-overflow-tooltip />
      <el-table-column prop="os" label="操作系统" width="120" />
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="消息" min-width="150" show-overflow-tooltip />
      <el-table-column prop="loginTime" label="登录时间" width="170" />
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
    <el-dialog v-model="detailVisible" title="登录日志详情" width="600px" destroy-on-close>
      <div v-if="detail" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户名">{{ detail.username }}</el-descriptions-item>
          <el-descriptions-item label="登录状态">
            <el-tag :type="detail.status === 1 ? 'success' : 'danger'">{{ detail.status === 1 ? '成功' : '失败' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="IP地址">{{ detail.ip }}</el-descriptions-item>
          <el-descriptions-item label="登录地点">{{ detail.location || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="浏览器">{{ detail.browser || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="操作系统">{{ detail.os || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="消息" :span="2">{{ detail.message || '-' }}</el-descriptions-item>
          <el-descriptions-item label="登录时间" :span="2">{{ detail.loginTime }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { getLoginLogPage } from '@/api/log'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const detailVisible = ref(false)
const detail = ref<any>(null)
const dateRange = ref<[string, string] | null>(null)
const statusFilter = ref<number | undefined>(undefined)
const todayLogins = ref(234)
const successLogins = ref(228)
const failLogins = ref(6)
const uniqueUsers = ref(156)

const queryParams = reactive({ pageNum: 1, pageSize: 10, keyword: '', startDate: '', endDate: '' })

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
  queryParams.startDate = ''
  queryParams.endDate = ''
  dateRange.value = null
  statusFilter.value = undefined
  queryParams.pageNum = 1
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const params = { ...queryParams }
    if (statusFilter.value !== undefined) {
      params.keyword = params.keyword
    }
    const res = await getLoginLogPage(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

function viewDetail(row: any) {
  detail.value = row
  detailVisible.value = true
}

onMounted(fetchData)
</script>

<style scoped>
.search-bar { margin-bottom: 16px; display: flex; gap: 10px; flex-wrap: wrap; }
.stats-row { margin-bottom: 0; }
.stat-card { border-radius: 12px; padding: 20px; color: #fff; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.stat-label { font-size: 13px; opacity: 0.85; margin-bottom: 4px; }
.stat-value { font-size: 28px; font-weight: 600; }
.pagination { margin-top: 16px; text-align: right; }
.detail-content { line-height: 1.8; }
</style>
