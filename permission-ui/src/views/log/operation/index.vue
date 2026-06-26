<template>
  <div class="page-container">
    <div class="search-bar">
      <el-input v-model="queryParams.keyword" placeholder="操作人/操作内容" clearable style="width: 220px" @keyup.enter="fetchData" />
      <el-select v-model="queryParams.module" placeholder="操作模块" clearable style="width: 140px">
        <el-option label="用户管理" value="用户管理" />
        <el-option label="角色管理" value="角色管理" />
        <el-option label="菜单管理" value="菜单管理" />
        <el-option label="部门管理" value="部门管理" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期"
        end-placeholder="结束日期" format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width: 260px"
        @change="onDateChange" />
      <el-button type="primary" :icon="Search" @click="fetchData">搜索</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
    </div>

    <div class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="module" label="操作模块" width="100" />
        <el-table-column prop="action" label="操作类型" width="110" />
        <el-table-column prop="operator" label="操作人" width="110" />
        <el-table-column prop="requestMethod" label="请求方式" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="methodTag(row.requestMethod)" size="small">{{ row.requestMethod }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestUrl" label="请求地址" min-width="180" show-overflow-tooltip />
        <el-table-column label="执行时长" width="100" align="center">
          <template #default="{ row }">{{ row.executeTime }}ms</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operatorIp" label="IP" width="140" />
        <el-table-column prop="createTime" label="操作时间" width="170" />
        <el-table-column label="操作" width="70" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50]" :total="total" layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData" @current-change="fetchData"
        />
      </div>
    </div>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="操作日志详情" width="700px">
      <el-descriptions :column="2" border v-if="currentLog">
        <el-descriptions-item label="操作模块">{{ currentLog.module }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ currentLog.action }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ currentLog.operator }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ currentLog.operatorIp }}</el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ currentLog.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="请求URL">{{ currentLog.requestUrl }}</el-descriptions-item>
        <el-descriptions-item label="执行方法" :span="2">{{ currentLog.method }}</el-descriptions-item>
        <el-descriptions-item label="执行时长">{{ currentLog.executeTime }}ms</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentLog.status === 1 ? 'success' : 'danger'" size="small">
            {{ currentLog.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <div class="json-block">{{ currentLog.requestParams }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="响应结果" :span="2">
          <div class="json-block">{{ currentLog.responseResult }}</div>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentLog.errorMsg" label="错误信息" :span="2">
          <div class="json-block" style="color: #f56c6c">{{ currentLog.errorMsg }}</div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getOperationLogPage } from '@/api/log'
import type { OperationLog } from '@/types'

const loading = ref(false)
const tableData = ref<OperationLog[]>([])
const total = ref(0)
const dateRange = ref<string[] | null>(null)

const queryParams = reactive({
  pageNum: 1, pageSize: 10, keyword: '', module: '', startDate: '', endDate: '',
})

function onDateChange(val: string[] | null) {
  queryParams.startDate = val?.[0] || ''
  queryParams.endDate = val?.[1] || ''
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getOperationLogPage(queryParams)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

function resetQuery() {
  queryParams.keyword = ''; queryParams.module = ''
  queryParams.startDate = ''; queryParams.endDate = ''
  dateRange.value = null; queryParams.pageNum = 1
  fetchData()
}

function methodTag(method: string): 'success' | 'primary' | 'warning' | 'danger' | 'info' {
  const map: Record<string, 'success' | 'primary' | 'warning' | 'danger' | 'info'> = {
    GET: 'success', POST: 'primary', PUT: 'warning', DELETE: 'danger',
  }
  return map[method] || 'info'
}

const detailVisible = ref(false)
const currentLog = ref<OperationLog | null>(null)

function showDetail(log: OperationLog) {
  currentLog.value = log
  detailVisible.value = true
}

onMounted(fetchData)
</script>

<style scoped>
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.json-block { max-height: 200px; overflow-y: auto; white-space: pre-wrap; word-break: break-all;
  font-size: 12px; background: #f5f7fa; padding: 10px; border-radius: 6px; }
</style>
