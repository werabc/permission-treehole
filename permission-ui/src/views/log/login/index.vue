<template>
  <div class="page-container">
    <div class="search-bar">
      <el-input v-model="queryParams.keyword" placeholder="用户名/IP" clearable style="width: 220px" @keyup.enter="fetchData" />
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期"
        end-placeholder="结束日期" format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width: 260px"
        @change="onDateChange" />
      <el-button type="primary" :icon="Search" @click="fetchData">搜索</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
    </div>

    <div class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="ip" label="登录IP" width="150" />
        <el-table-column prop="location" label="登录地点" width="140" />
        <el-table-column prop="browser" label="浏览器" min-width="140" show-overflow-tooltip />
        <el-table-column prop="os" label="操作系统" width="150" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="提示信息" min-width="160" show-overflow-tooltip />
        <el-table-column prop="loginTime" label="登录时间" width="170" />
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50]" :total="total" layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData" @current-change="fetchData"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getLoginLogPage } from '@/api/log'
import type { LoginLog } from '@/types'

const loading = ref(false)
const tableData = ref<LoginLog[]>([])
const total = ref(0)
const dateRange = ref<string[] | null>(null)

const queryParams = reactive({
  pageNum: 1, pageSize: 10, keyword: '', startDate: '', endDate: '',
})

function onDateChange(val: string[] | null) {
  queryParams.startDate = val?.[0] || ''
  queryParams.endDate = val?.[1] || ''
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getLoginLogPage(queryParams)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

function resetQuery() {
  queryParams.keyword = ''; queryParams.startDate = ''; queryParams.endDate = ''
  dateRange.value = null; queryParams.pageNum = 1
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
