<template>
  <div class="dashboard">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #667eea, #764ba2)">
          <div class="stat-icon"><el-icon :size="32"><UserFilled /></el-icon></div>
          <div class="stat-content">
            <div class="stat-label">用户总数</div>
            <div class="stat-value">{{ stats.overview?.userCount ?? '-' }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #f093fb, #f5576c)">
          <div class="stat-icon"><el-icon :size="32"><Key /></el-icon></div>
          <div class="stat-content">
            <div class="stat-label">角色总数</div>
            <div class="stat-value">{{ stats.overview?.roleCount ?? '-' }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #4facfe, #00f2fe)">
          <div class="stat-icon"><el-icon :size="32"><Lock /></el-icon></div>
          <div class="stat-content">
            <div class="stat-label">菜单总数</div>
            <div class="stat-value">{{ stats.overview?.menuCount ?? '-' }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #43e97b, #38f9d7)">
          <div class="stat-icon"><el-icon :size="32"><OfficeBuilding /></el-icon></div>
          <div class="stat-content">
            <div class="stat-label">部门总数</div>
            <div class="stat-value">{{ stats.overview?.deptCount ?? '-' }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span class="card-header">用户状态分布</span></template>
          <div ref="userStatusRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span class="card-header">部门人数分布 (Top 10)</span></template>
          <div ref="deptChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header><span class="card-header">最近 7 天登录趋势</span></template>
          <div ref="loginTrendRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 角色信息 + 系统信息 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span class="card-header">我的角色</span></template>
          <el-tag v-for="role in userStore.roles" :key="role" type="success" style="margin: 4px" size="large">
            {{ role }}
          </el-tag>
          <el-empty v-if="userStore.roles.length === 0" description="暂无角色" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span class="card-header">系统信息</span></template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="系统名称">企业级权限管理系统</el-descriptions-item>
            <el-descriptions-item label="后端框架">Spring Boot 3.2 + JDK 17</el-descriptions-item>
            <el-descriptions-item label="前端框架">Vue 3 + Element Plus</el-descriptions-item>
            <el-descriptions-item label="安全框架">Spring Security 6 + JWT</el-descriptions-item>
            <el-descriptions-item label="数据库">MySQL 8.0 + Redis</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { UserFilled, Key, Lock, OfficeBuilding } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getDashboardStatistics } from '@/api/dashboard'

const userStore = useUserStore()
const userStatusRef = ref<HTMLElement>()
const deptChartRef = ref<HTMLElement>()
const loginTrendRef = ref<HTMLElement>()

const stats = reactive({
  overview: { userCount: 0, roleCount: 0, menuCount: 0, deptCount: 0 },
  userStatus: { active: 0, inactive: 0 },
  deptUserCount: [] as Array<{ name: string; count: number }>,
  loginTrend: { dates: [] as string[], counts: [] as number[] },
})

let userStatusChart: echarts.ECharts | null = null
let deptChart: echarts.ECharts | null = null
let loginTrendChart: echarts.ECharts | null = null

async function fetchStats() {
  try {
    const res = await getDashboardStatistics()
    const data = res.data
    Object.assign(stats, data)
    await nextTick()
    renderCharts()
  } catch (e) {
    console.error('Failed to fetch dashboard stats:', e)
  }
}

function renderCharts() {
  // 用户状态饼图
  if (userStatusRef.value) {
    userStatusChart = echarts.init(userStatusRef.value)
    userStatusChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { bottom: '0%', left: 'center' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}: {c}' },
        data: [
          { value: stats.userStatus.active || 0, name: '启用', itemStyle: { color: '#67c23a' } },
          { value: stats.userStatus.inactive || 0, name: '禁用', itemStyle: { color: '#f56c6c' } },
        ],
      }],
    })
  }

  // 部门人数柱状图
  if (deptChartRef.value) {
    deptChart = echarts.init(deptChartRef.value)
    deptChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: stats.deptUserCount.map(d => d.name), axisLabel: { rotate: 30 } },
      yAxis: { type: 'value', name: '人数' },
      series: [{
        type: 'bar',
        data: stats.deptUserCount.map(d => d.count),
        itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#83bff6' }, { offset: 1, color: '#188df0' }]), borderRadius: [4, 4, 0, 0] },
        barWidth: '50%',
      }],
    })
  }

  // 登录趋势折线图
  if (loginTrendRef.value) {
    loginTrendChart = echarts.init(loginTrendRef.value)
    loginTrendChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: stats.loginTrend.dates },
      yAxis: { type: 'value', name: '登录次数' },
      series: [{
        name: '登录次数',
        type: 'line',
        smooth: true,
        data: stats.loginTrend.counts,
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(58,77,233,0.5)' }, { offset: 1, color: 'rgba(58,77,233,0.05)' }]) },
        itemStyle: { color: '#3a4de9' },
        lineStyle: { width: 3 },
      }],
    })
  }
}

function handleResize() {
  userStatusChart?.resize()
  deptChart?.resize()
  loginTrendChart?.resize()
}

onMounted(() => {
  fetchStats()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  userStatusChart?.dispose()
  deptChart?.dispose()
  loginTrendChart?.dispose()
})
</script>

<style scoped>
.stats-row { margin-bottom: 0; }
.stat-card {
  border-radius: 12px; padding: 24px; color: #fff;
  display: flex; align-items: center; gap: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
.stat-icon { opacity: 0.8; }
.stat-label { font-size: 13px; opacity: 0.85; margin-bottom: 4px; }
.stat-value { font-size: 20px; font-weight: 600; }
.card-header { font-weight: 600; font-size: 15px; }
</style>
