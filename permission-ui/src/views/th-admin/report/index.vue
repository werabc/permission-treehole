<template>
  <div class="page-container">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #e6a23c, #f56c6c)">
          <div class="stat-label">待处理</div>
          <div class="stat-value">{{ reportStats.pending ?? '-' }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #67c23a, #43e97b)">
          <div class="stat-label">已成立</div>
          <div class="stat-value">{{ reportStats.approved ?? '-' }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #909399, #b0b3b8)">
          <div class="stat-label">不成立</div>
          <div class="stat-value">{{ reportStats.rejected ?? '-' }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #409eff, #4facfe)">
          <div class="stat-label">总计</div>
          <div class="stat-value">{{ reportStats.total ?? '-' }}</div>
        </div>
      </el-col>
    </el-row>

    <div class="search-bar" style="margin-top: 20px">
      <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 120px" @change="fetchData">
        <el-option label="全部" :value="undefined" />
        <el-option label="待处理" :value="0" />
        <el-option label="已成立" :value="1" />
        <el-option label="不成立" :value="2" />
      </el-select>
      <el-select v-model="targetTypeFilter" placeholder="举报类型" clearable style="width: 120px" @change="fetchData">
        <el-option label="全部" :value="undefined" />
        <el-option label="帖子" :value="'POST'" />
        <el-option label="评论" :value="'COMMENT'" />
      </el-select>
      <el-button type="primary" @click="fetchData">
        <el-icon><Search /></el-icon>搜索
      </el-button>
      <el-button @click="resetQuery">重置</el-button>
    </div>

    <!-- 批量操作栏 -->
    <div class="batch-bar">
      <el-button type="success" :disabled="selectedRows.length === 0" @click="handleBatch(1)">
        <el-icon><Check /></el-icon>批量成立
      </el-button>
      <el-button type="danger" :disabled="selectedRows.length === 0" @click="handleBatch(2)">
        <el-icon><Close /></el-icon>批量不成立
      </el-button>
      <span class="selected-count" v-if="selectedRows.length > 0">已选择 {{ selectedRows.length }} 项</span>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="40" :selectable="(row: any) => row.status === 0" />
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="targetType" label="类型" width="80">
        <template #default="{ row }">
          <el-tag size="small" :type="row.targetType === 'POST' ? 'primary' : 'info'">{{ row.targetType === 'POST' ? '帖子' : '评论' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="targetId" label="目标ID" width="80" align="center" />
      <el-table-column prop="reason" label="举报原因" width="120" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="reporterName" label="举报人" width="100" />
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'warning' : row.status === 1 ? 'success' : 'info'" size="small">
            {{ ['待处理', '已成立', '不成立'][row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="举报时间" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
          <template v-if="row.status === 0">
            <el-button link type="success" size="small" @click="handleReport(row, 1)">成立</el-button>
            <el-button link type="danger" size="small" @click="handleReport(row, 2)">不成立</el-button>
          </template>
          <span v-else class="handled-text">{{ row.handleResult || '已处理' }}</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize" :total="total" :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper" @size-change="fetchData" @current-change="fetchData" />
    </div>

    <!-- 举报详情对话框 -->
    <el-dialog v-model="detailVisible" title="举报详情" width="550px" destroy-on-close>
      <div v-if="detail" class="detail-content">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="举报类型">{{ detail.targetType === 'POST' ? '帖子' : '评论' }}</el-descriptions-item>
          <el-descriptions-item label="目标ID">{{ detail.targetId }}</el-descriptions-item>
          <el-descriptions-item label="举报原因">{{ detail.reason }}</el-descriptions-item>
          <el-descriptions-item label="描述">{{ detail.description || '无' }}</el-descriptions-item>
          <el-descriptions-item label="举报人">{{ detail.reporterName }}</el-descriptions-item>
          <el-descriptions-item label="处理结果">{{ detail.handleResult || '未处理' }}</el-descriptions-item>
          <el-descriptions-item label="处理人">{{ detail.handlerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="举报时间">{{ detail.createTime }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button v-if="detail && detail.status === 0" type="success" @click="handleReport(detail, 1); detailVisible = false">成立</el-button>
        <el-button v-if="detail && detail.status === 0" type="danger" @click="handleReport(detail, 2); detailVisible = false">不成立</el-button>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 处理对话框 -->
    <el-dialog v-model="handleVisible" title="处理举报" width="450px">
      <el-form label-width="80px">
        <el-form-item label="处理结果">
          <el-tag :type="handleStatus === 1 ? 'success' : 'danger'" size="large">{{ handleStatus === 1 ? '举报成立' : '举报不成立' }}</el-tag>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="handleResult" type="textarea" rows="3" placeholder="请输入处理备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmHandle">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Check, Close } from '@element-plus/icons-vue'
import { getThReportPage, getThReportDetail, handleThReport, batchHandleReports, getThReportStats } from '@/api/treehole-admin'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const detailVisible = ref(false)
const handleVisible = ref(false)
const detail = ref<any>(null)
const selectedRows = ref<any[]>([])
const targetTypeFilter = ref<string | undefined>(undefined)
const handleStatus = ref(1)
const handleResult = ref('')
const currentRow = ref<any>(null)
const reportStats = ref<any>({})

const queryParams = reactive({ pageNum: 1, pageSize: 10, status: undefined as number | undefined })

function resetQuery() {
  queryParams.status = undefined
  targetTypeFilter.value = undefined
  queryParams.pageNum = 1
  fetchData()
}

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows.filter((r: any) => r.status === 0)
}

async function fetchStats() {
  try {
    const res = await getThReportStats()
    reportStats.value = res.data
  } catch (e) {
    console.error('Failed to fetch report stats:', e)
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getThReportPage(queryParams)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

async function viewDetail(row: any) {
  const res = await getThReportDetail(row.id)
  detail.value = res.data
  detailVisible.value = true
}

function handleReport(row: any, status: number) {
  currentRow.value = row
  handleStatus.value = status
  handleResult.value = status === 1 ? '举报成立' : '举报不成立'
  handleVisible.value = true
}

async function confirmHandle() {
  try {
    await handleThReport(currentRow.value.id, handleStatus.value, handleResult.value)
    ElMessage.success('处理成功')
    handleVisible.value = false
    await fetchData()
    await fetchStats()
  } catch (e: any) {
    ElMessage.error(e.message || '处理失败')
  }
}

async function handleBatch(status: number) {
  const ids = selectedRows.value.map((r: any) => r.id)
  if (ids.length === 0) return
  try {
    await ElMessageBox.confirm(`确认批量处理选中的 ${ids.length} 条举报？`, '批量操作')
    await batchHandleReports({ ids, status, result: status === 1 ? '举报成立' : '举报不成立' })
    ElMessage.success('批量处理成功')
    await fetchData()
    await fetchStats()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '批量处理失败')
    }
  }
}

onMounted(() => { fetchData(); fetchStats() })
</script>

<style scoped>
.stats-row { margin-bottom: 0; }
.stat-card { border-radius: 12px; padding: 20px; color: #fff; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.stat-label { font-size: 13px; opacity: 0.85; margin-bottom: 4px; }
.stat-value { font-size: 28px; font-weight: 600; }
.search-bar { margin-bottom: 12px; display: flex; gap: 10px; flex-wrap: wrap; }
.batch-bar { margin-bottom: 12px; display: flex; gap: 10px; align-items: center; }
.selected-count { color: #409eff; font-size: 13px; }
.pagination { margin-top: 16px; text-align: right; }
.detail-content { line-height: 1.8; }
.handled-text { color: #909399; font-size: 12px; }
</style>
