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
            <div class="stat-trend">
              <el-icon v-if="userTrend > 0" color="#67c23a"><CaretTop /></el-icon>
              <el-icon v-else color="#f56c6c"><CaretBottom /></el-icon>
              <span>{{ Math.abs(userTrend) }}% 较上周</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #f093fb, #f5576c)">
          <div class="stat-icon"><el-icon :size="32"><Document /></el-icon></div>
          <div class="stat-content">
            <div class="stat-label">帖子总数</div>
            <div class="stat-value">{{ postCount }}</div>
            <div class="stat-trend">
              <el-icon v-if="postTrend > 0" color="#67c23a"><CaretTop /></el-icon>
              <el-icon v-else color="#f56c6c"><CaretBottom /></el-icon>
              <span>{{ Math.abs(postTrend) }}% 较上周</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #4facfe, #00f2fe)">
          <div class="stat-icon"><el-icon :size="32"><ChatDotRound /></el-icon></div>
          <div class="stat-content">
            <div class="stat-label">评论总数</div>
            <div class="stat-value">{{ commentCount }}</div>
            <div class="stat-trend">
              <el-icon v-if="commentTrend > 0" color="#67c23a"><CaretTop /></el-icon>
              <el-icon v-else color="#f56c6c"><CaretBottom /></el-icon>
              <span>{{ Math.abs(commentTrend) }}% 较上周</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #43e97b, #38f9d7)">
          <div class="stat-icon"><el-icon :size="32"><Warning /></el-icon></div>
          <div class="stat-content">
            <div class="stat-label">待处理举报</div>
            <div class="stat-value">{{ pendingReports }}</div>
            <div class="stat-trend">
              <span style="font-size: 12px; opacity: 0.85">需及时处理</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>最近 7 天趋势</span>
              <el-radio-group v-model="trendDays" size="small" @change="fetchTrends">
                <el-radio-button :value="7">7天</el-radio-button>
                <el-radio-button :value="14">14天</el-radio-button>
                <el-radio-button :value="30">30天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" style="height: 320px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header><span class="card-header">用户状态分布</span></template>
          <div ref="userStatusRef" style="height: 320px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span class="card-header">部门人数分布 (Top 10)</span></template>
          <div ref="deptChartRef" style="height: 280px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span class="card-header">帖子状态分布</span></template>
          <div ref="postStatusRef" style="height: 280px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 待办事项 + 系统健康 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>待办事项</span>
              <el-badge :value="pendingItems.length" :hidden="pendingItems.length === 0" type="danger" />
            </div>
          </template>
          <div class="pending-list">
            <div v-for="item in pendingItems" :key="item.id" class="pending-item">
              <el-icon :size="16" :color="item.color"><component :is="item.icon" /></el-icon>
              <span class="pending-text">{{ item.text }}</span>
              <el-tag :type="item.type" size="small">{{ item.count }}</el-tag>
            </div>
            <el-empty v-if="pendingItems.length === 0" description="暂无待办事项" :image-size="60" />
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span class="card-header">系统健康</span></template>
          <div class="health-list">
            <div v-for="item in healthItems" :key="item.name" class="health-item">
              <div class="health-info">
                <el-icon :size="16" :color="item.status === 'good' ? '#67c23a' : item.status === 'warning' ? '#e6a23c' : '#f56c6c'">
                  <CircleCheck v-if="item.status === 'good'" />
                  <Warning v-else-if="item.status === 'warning'" />
                  <CircleClose v-else />
                </el-icon>
                <span class="health-name">{{ item.name }}</span>
              </div>
              <span class="health-value">{{ item.value }}</span>
            </div>
          </div>
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
          <el-empty v-if="userStore.roles.length === 0" description="暂无角色" :image-size="60" />
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
import { ref, reactive, onMounted, onUnmounted, nextTick, computed } from 'vue'
import * as echarts from 'echarts'
import { UserFilled, Key, Lock, OfficeBuilding, Document, ChatDotRound, Warning, CaretTop, CaretBottom, CircleCheck, CircleClose, Flag, View } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getDashboardStatistics } from '@/api/dashboard'
import { getThAnalyticsOverview, getThAnalyticsTrends } from '@/api/treehole-admin'

const userStore = useUserStore()
const trendChartRef = ref<HTMLElement>()
const userStatusRef = ref<HTMLElement>()
const deptChartRef = ref<HTMLElement>()
const postStatusRef = ref<HTMLElement>()

const trendDays = ref(7)
const postCount = ref(0)
const commentCount = ref(0)
const pendingReports = ref(0)
const userTrend = ref(5.2)
const postTrend = ref(3.8)
const commentTrend = ref(-1.2)

const stats = reactive({
  overview: { userCount: 0, roleCount: 0, menuCount: 0, deptCount: 0 },
  userStatus: { active: 0, inactive: 0 },
  deptUserCount: [] as Array<{ name: string; count: number }>,
  loginTrend: { dates: [] as string[], counts: [] as number[] },
})

let trendChart: echarts.ECharts | null = null
let userStatusChart: echarts.ECharts | null = null
let deptChart: echarts.ECharts | null = null
let postStatusChart: echarts.ECharts | null = null

const pendingItems = computed(() => {
  const items = []
  if (pendingReports.value > 0) {
    items.push({ id: 1, icon: 'Flag', text: '待处理举报', count: pendingReports.value, type: 'danger', color: '#f56c6c' })
  }
  if (pendingPosts.value > 0) {
    items.push({ id: 2, icon: 'Document', text: '待审核帖子', count: pendingPosts.value, type: 'warning', color: '#e6a23c' })
  }
  if (pendingComments.value > 0) {
    items.push({ id: 3, icon: 'ChatDotRound', text: '待审核评论', count: pendingComments.value, type: 'info', color: '#409eff' })
  }
  return items
})

const pendingPosts = ref(0)
const pendingComments = ref(0)

const healthItems = ref([
  { name: '数据库连接', status: 'good' as const, value: '正常' },
  { name: 'Redis 缓存', status: 'good' as const, value: '正常' },
  { name: 'API 响应', status: 'good' as const, value: '< 100ms' },
  { name: '磁盘使用', status: 'warning' as const, value: '72%' },
  { name: '内存使用', status: 'good' as const, value: '58%' },
])

async function fetchStats() {
  try {
    const res = await getDashboardStatistics()
    Object.assign(stats, res.data)
    await nextTick()
    renderDeptChart()
    renderUserStatusChart()
  } catch (e) {
    console.error('Failed to fetch dashboard stats:', e)
  }
}

async function fetchAnalytics() {
  try {
    const res = await getThAnalyticsOverview()
    postCount.value = res.data.stats?.postCount ?? 0
    commentCount.value = res.data.stats?.commentCount ?? 0
    pendingReports.value = res.data.pendingReports ?? 0
    pendingPosts.value = res.data.postStatus?.pending ?? 0
    pendingComments.value = 0
    await nextTick()
    renderPostStatusChart()
  } catch (e) {
    console.error('Failed to fetch analytics:', e)
  }
}

async function fetchTrends() {
  try {
    const res = await getThAnalyticsTrends(trendDays.value)
    renderTrendChart(res.data)
  } catch (e) {
    console.error('Failed to fetch trends:', e)
  }
}

function renderTrendChart(data: any) {
  if (!trendChartRef.value) return
  trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['用户', '帖子', '评论'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: data.dates },
    yAxis: { type: 'value' },
    series: [
      { name: '用户', type: 'line', smooth: true, data: data.userTrend || [], itemStyle: { color: '#667eea' }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(102,126,234,0.3)' }, { offset: 1, color: 'rgba(102,126,234,0.02)' }]) } },
      { name: '帖子', type: 'line', smooth: true, data: data.postTrend || [], itemStyle: { color: '#f093fb' }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(240,147,251,0.3)' }, { offset: 1, color: 'rgba(240,147,251,0.02)' }]) } },
      { name: '评论', type: 'line', smooth: true, data: data.commentTrend || [], itemStyle: { color: '#4facfe' }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(79,172,254,0.3)' }, { offset: 1, color: 'rgba(79,172,254,0.02)' }]) } },
    ],
  })
}

function renderUserStatusChart() {
  if (!userStatusRef.value) return
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

function renderDeptChart() {
  if (!deptChartRef.value) return
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

function renderPostStatusChart() {
  if (!postStatusRef.value) return
  postStatusChart = echarts.init(postStatusRef.value)
  postStatusChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: '0%', left: 'center' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}: {c}' },
      data: [
        { value: 0, name: '已通过', itemStyle: { color: '#67c23a' } },
        { value: 0, name: '待审核', itemStyle: { color: '#e6a23c' } },
        { value: 0, name: '已拒绝', itemStyle: { color: '#f56c6c' } },
      ],
    }],
  })
}

function handleResize() {
  trendChart?.resize()
  userStatusChart?.resize()
  deptChart?.resize()
  postStatusChart?.resize()
}

onMounted(() => {
  fetchStats()
  fetchAnalytics()
  fetchTrends()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  userStatusChart?.dispose()
  deptChart?.dispose()
  postStatusChart?.dispose()
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
.stat-value { font-size: 24px; font-weight: 600; }
.stat-trend { font-size: 12px; opacity: 0.85; margin-top: 4px; display: flex; align-items: center; gap: 2px; }
.card-header { font-weight: 600; font-size: 15px; display: flex; justify-content: space-between; align-items: center; }
.pending-list { max-height: 200px; overflow-y: auto; }
.pending-item { display: flex; align-items: center; gap: 10px; padding: 10px 0; border-bottom: 1px solid #f0f0f0; }
.pending-item:last-child { border-bottom: none; }
.pending-text { flex: 1; font-size: 14px; }
.health-list { display: flex; flex-direction: column; gap: 14px; }
.health-item { display: flex; justify-content: space-between; align-items: center; }
.health-info { display: flex; align-items: center; gap: 8px; }
.health-name { font-size: 14px; }
.health-value { font-size: 13px; color: #909399; }
</style>
