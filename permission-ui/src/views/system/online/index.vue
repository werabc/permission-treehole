<template>
  <div class="page-container">
    <div class="search-bar">
      <el-tag type="success" size="large" style="margin-right: 12px">
        <el-icon><UserFilled /></el-icon>
        <span style="margin-left: 6px">在线用户：{{ tableData.length }} 人</span>
      </el-tag>
      <div style="flex: 1" />
      <el-button :icon="Refresh" @click="fetchData">刷新</el-button>
    </div>

    <div class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="userId" label="用户ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="ip" label="登录IP" width="140" />
        <el-table-column prop="loginTime" label="登录时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-popconfirm title="确定要强制该用户下线吗？" @confirm="handleForceLogout(row.userId)">
              <template #reference>
                <el-button link type="danger" size="small">强制下线</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && tableData.length === 0" description="暂无在线用户" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Refresh, UserFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getOnlineUsers, forceLogout } from '@/api/online'
import type { OnlineUser } from '@/api/online'

const loading = ref(false)
const tableData = ref<OnlineUser[]>([])

async function fetchData() {
  loading.value = true
  try {
    const res = await getOnlineUsers()
    tableData.value = res.data
  } finally {
    loading.value = false
  }
}

async function handleForceLogout(userId: string) {
  await forceLogout(Number(userId))
  ElMessage.success('已强制下线')
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.search-bar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}
.table-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
}
</style>
