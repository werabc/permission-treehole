<template>
  <div class="statistics">
    <el-row :gutter="20">
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #667eea, #764ba2)">
          <div class="stat-label">总帖子数</div>
          <div class="stat-value">{{ stats.totalPosts ?? '-' }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #f093fb, #f5576c)">
          <div class="stat-label">待审核</div>
          <div class="stat-value">{{ stats.pendingPosts ?? '-' }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #4facfe, #00f2fe)">
          <div class="stat-label">总评论数</div>
          <div class="stat-value">{{ stats.totalComments ?? '-' }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #43e97b, #38f9d7)">
          <div class="stat-label">待处理举报</div>
          <div class="stat-value">{{ stats.pendingReports ?? '-' }}</div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getStatistics } from '@/api/treehole-admin'

const stats = ref<any>({})

async function fetchStats() {
  const res = await getStatistics()
  stats.value = res.data
}

onMounted(fetchStats)
</script>

<style scoped>
.stat-card {
  border-radius: 12px;
  padding: 24px;
  color: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
.stat-label { font-size: 13px; opacity: 0.85; margin-bottom: 4px; }
.stat-value { font-size: 28px; font-weight: 600; }
</style>
