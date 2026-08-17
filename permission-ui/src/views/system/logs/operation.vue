<template>
  <div class="page-container">
    <div class="search-bar">
      <el-input v-model="queryParams.keyword" placeholder="搜索操作/模块" clearable style="width: 200px" @keyup.enter="fetchData" @clear="fetchData" />
      <el-select v-model="queryParams.module" placeholder="操作模块" clearable style="width: 140px" @change="fetchData">
        <el-option label="用户管理" value="USER" />
        <el-option label="帖子管理" value="POST" />
        <el-option label="评论管理" value="COMMENT" />
        <el-option label="举报管理" value="REPORT" />
        <el-option label="分类管理" value="CATEGORY" />
        <el-option label="系统设置" value="SYSTEM" />
        <el-option label="登录认证" value="AUTH" />
      </el-select>
      <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 100px" @change="fetchData">
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
          <div class="stat-label">今日操作数</div>
          <div class="stat-value">{{ todayCount }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #67c23a, #43e97b)">
          <div class="stat-label">成功率</div>
          <div class="stat-value">{{ successRate }}%</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #e6a23c, #f56c6c)">
          <div class="stat-label">平均耗时</div>
          <div class="stat-value">{{ avgTime }}ms</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #909399, #b0b3b8)">
          <div class="stat-label">活跃操作员</div>
          <div class="stat-value">{{ activeOperators }}</div>
        </div>
      </el-col>
    </el-row>

    <el-table :data="tableData" v-loading="loading" stripe border style="margin-top: 20px">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="module" label="操作模块" width="100">
        <template #default="{ row }">
          <el-tag :type="moduleType(row.module)" size="small">{{ moduleLabel(row.module) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="action" label="操作" width="120" />
      <el-table-column prop="method" label="方法" min-width="180" show-overflow-tooltip />
      <el-table-column prop="requestMethod" label="请求方式" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.requestMethod === 'GET' ? 'success' : row.requestMethod === 'POST' ? 'primary' : row.requestMethod === 'PUT' ? 'warning' : 'danger'" size="small">
            {{ row.requestMethod }}
          </el-tag>
        </template>
      </el-table-column>
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
          <el-descriptions-item label="操作模块">
            <el-tag :type="moduleType(detail.module)">{{ moduleLabel(detail.module) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="操作类型">{{ detail.action }}</el-descriptions-item>
          <el-descriptions-item label="请求方式">
            <el-tag :type="detail.requestMethod === 'GET' ? 'success' : detail.requestMethod === 'POST' ? 'primary' : detail.requestMethod === 'PUT' ? 'warning' : 'danger'">
              {{ detail.requestMethod }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="请求URL">{{ detail.requestUrl }}</el-descriptions-item>
          <el-descriptions-item label="操作方法" :span="2">{{ detail.method }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{ detail.operator }}</el-descriptions-item>
          <el-descriptions-item label="操作IP">{{ detail.operatorIp }}</el-descriptions-item>
          <el-descriptions-item label="执行耗时">{{ detail.executeTime }}ms</el-descriptions-item>
          <el-descriptions-item label="操作状态">
            <el-tag :type="detail.status === 1 ? 'success' : 'danger'">{{ detail.status === 1 ? '成功' : '失败' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="操作时间" :span="2">{{ detail.createTime }}</el-descriptions-item>
        </el-descriptions>
        <div class="content-section">
          <strong>请求参数：</strong>
          <div class="content-box">{{ detail.requestParams || '无' }}</div>
        </div>
        <div class="content-section">
          <strong>响应结果：</strong>
          <div class="content-box">{{ detail.responseResult || '无' }}</div>
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
const statusFilter = ref<number | undefined>(undefined)
const todayCount = ref(156)
const successRate = ref(98.5)
const avgTime = ref(45)
const activeOperators = ref(8)

const queryParams = reactive({ pageNum: 1, pageSize: 10, keyword: '', module: '', startDate: '', endDate: '' })

function moduleLabel(m: string) {
  return { USER: '用户管理', POST: '帖子管理', COMMENT: '评论管理', REPORT: '举报管理', CATEGORY: '分类管理', SYSTEM: '系统设置', AUTH: '登录认证' }[m] || m
}
function moduleType(m: string) {
  return { USER: 'primary', POST: 'success', COMMENT: 'info', REPORT: 'danger', CATEGORY: 'warning', SYSTEM: '', AUTH: 'warning' }[m] || 'info'
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
    const res = await getOperationLogPage(params)
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
.content-section { margin-top: 16px; }
.content-box { background: #f8fafc; padding: 12px; border-radius: 8px; margin: 8px 0; white-space: pre-wrap; line-height: 1.6; font-size: 13px; max-height: 200px; overflow-y: auto; }
.error-box { background: #fef0f0; color: #f56c6c; }
.text-success { color: #67c23a; }
.text-warning { color: #e6a23c; }
.text-danger { color: #f56c6c; }
</style>
