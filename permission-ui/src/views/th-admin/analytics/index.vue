<template>
  <div class="page-container">
    <!-- 概览卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #667eea, #764ba2)">
          <div class="stat-label">用户总数</div>
          <div class="stat-value">{{ overview.stats?.userCount ?? '-' }}</div>
          <div class="stat-trend">活跃率 {{ overview.userActiveRate ?? '-' }}%</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #f093fb, #f5576c)">
          <div class="stat-label">帖子总数</div>
          <div class="stat-value">{{ overview.stats?.postCount ?? '-' }}</div>
          <div class="stat-trend">今日新增 {{ overview.todayPosts ?? '-' }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #4facfe, #00f2fe)">
          <div class="stat-label">评论总数</div>
          <div class="stat-value">{{ overview.stats?.commentCount ?? '-' }}</div>
          <div class="stat-trend">今日新增 {{ overview.todayComments ?? '-' }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #43e97b, #38f9d7)">
          <div class="stat-label">待处理举报</div>
          <div class="stat-value">{{ overview.pendingReports ?? '-' }}</div>
          <div class="stat-trend">本周 {{ overview.weekReports ?? '-' }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 趋势图 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>趋势分析</span>
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
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <!-- 用户状态分布 -->
      <el-col :span="8">
        <el-card shadow="never">
          <template #header><span class="card-header">用户状态分布</span></template>
          <div ref="userStatusRef" style="height: 280px"></div>
        </el-card>
      </el-col>
      <!-- 帖子状态分布 -->
      <el-col :span="8">
        <el-card shadow="never">
          <template #header><span class="card-header">帖子状态分布</span></template>
          <div ref="postStatusRef" style="height: 280px"></div>
        </el-card>
      </el-col>
      <!-- 分类分布 -->
      <el-col :span="8">
        <el-card shadow="never">
          <template #header><span class="card-header">分类热度分布</span></template>
          <div ref="categoryRef" style="height: 280px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <!-- 用户增长趋势 -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span class="card-header">用户增长趋势</span></template>
          <div ref="userGrowthRef" style="height: 280px"></div>
        </el-card>
      </el-col>
      <!-- 评论活跃度 -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span class="card-header">评论活跃度</span></template>
          <div ref="commentActivityRef" style="height: 280px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 热门帖子排行 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header><span class="card-header">热门帖子排行 (Top 10)</span></template>
          <el-table :data="topPosts" stripe border>
            <el-table-column type="index" label="排名" width="60" align="center" />
            <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
            <el-table-column prop="authorName" label="作者" width="100" />
            <el-table-column prop="categoryName" label="分类" width="80" />
            <el-table-column prop="viewCount" label="浏览" width="80" align="center" />
            <el-table-column prop="likeCount" label="点赞" width="80" align="center" />
            <el-table-column prop="commentCount" label="评论" width="80" align="center" />
            <el-table-column prop="createTime" label="发布时间" width="170" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getThAnalyticsOverview, getThAnalyticsTrends, getThAnalyticsCategories } from '@/api/treehole-admin'

const overview = reactive<any>({})
const trendDays = ref(7)
const topPosts = ref<any[]>([])
const trendChartRef = ref<HTMLElement>()
const userStatusRef = ref<HTMLElement>()
const postStatusRef = ref<HTMLElement>()
const categoryRef = ref<HTMLElement>()
const userGrowthRef = ref<HTMLElement>()
const commentActivityRef = ref<HTMLElement>()

let trendChart: echarts.ECharts | null = null
let userStatusChart: echarts.ECharts | null = null
let postStatusChart: echarts.ECharts | null = null
let categoryChart: echarts.ECharts | null = null
let userGrowthChart: echarts.ECharts | null = null
let commentActivityChart: echarts.ECharts | null = null

async function fetchOverview() {
  const res = await getThAnalyticsOverview()
  Object.assign(overview, res.data)
  await nextTick()
  renderUserStatusChart()
  renderPostStatusChart()
  topPosts.value = res.data.topPosts || []
}

async function fetchTrends() {
  const res = await getThAnalyticsTrends(trendDays.value)
  renderTrendChart(res.data)
  renderUserGrowthChart(res.data)
  renderCommentActivityChart(res.data)
}

async function fetchCategories() {
  const res = await getThAnalyticsCategories()
  renderCategoryChart(res.data)
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
      { name: '用户', type: 'line', smooth: true, data: data.userTrend, itemStyle: { color: '#667eea' }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(102,126,234,0.3)' }, { offset: 1, color: 'rgba(102,126,234,0.02)' }]) } },
      { name: '帖子', type: 'line', smooth: true, data: data.postTrend, itemStyle: { color: '#f093fb' }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(240,147,251,0.3)' }, { offset: 1, color: 'rgba(240,147,251,0.02)' }]) } },
      { name: '评论', type: 'line', smooth: true, data: data.commentTrend, itemStyle: { color: '#4facfe' }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(79,172,254,0.3)' }, { offset: 1, color: 'rgba(79,172,254,0.02)' }]) } },
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
      type: 'pie', radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}: {c}' },
      data: [
        { value: overview.userStatus?.active || 0, name: '正常', itemStyle: { color: '#67c23a' } },
        { value: overview.userStatus?.banned || 0, name: '封禁', itemStyle: { color: '#f56c6c' } },
        { value: overview.userStatus?.muted || 0, name: '禁言', itemStyle: { color: '#e6a23c' } },
      ],
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
      type: 'pie', radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}: {c}' },
      data: [
        { value: overview.postStatus?.approved || 0, name: '已通过', itemStyle: { color: '#67c23a' } },
        { value: overview.postStatus?.pending || 0, name: '待审核', itemStyle: { color: '#e6a23c' } },
        { value: overview.postStatus?.rejected || 0, name: '已拒绝', itemStyle: { color: '#f56c6c' } },
      ],
    }],
  })
}

function renderCategoryChart(data: any) {
  if (!categoryRef.value) return
  categoryChart = echarts.init(categoryRef.value)
  const categories = data?.categories || []
  categoryChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: '0%', left: 'center' },
    series: [{
      type: 'pie', radius: ['30%', '65%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}: {c}' },
      data: categories.map((c: any) => ({ value: c.count, name: c.name })),
    }],
  })
}

function renderUserGrowthChart(data: any) {
  if (!userGrowthRef.value) return
  userGrowthChart = echarts.init(userGrowthRef.value)
  userGrowthChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: data.dates },
    yAxis: { type: 'value', name: '新增用户' },
    series: [{
      type: 'bar',
      data: data.userTrend,
      itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#667eea' }, { offset: 1, color: '#764ba2' }]), borderRadius: [4, 4, 0, 0] },
      barWidth: '40%',
    }],
  })
}

function renderCommentActivityChart(data: any) {
  if (!commentActivityRef.value) return
  commentActivityChart = echarts.init(commentActivityRef.value)
  commentActivityChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: data.dates },
    yAxis: { type: 'value', name: '评论数' },
    series: [{
      type: 'line',
      smooth: true,
      data: data.commentTrend,
      itemStyle: { color: '#4facfe' },
      lineStyle: { width: 3 },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(79,172,254,0.4)' }, { offset: 1, color: 'rgba(79,172,254,0.02)' }]) },
    }],
  })
}

function handleResize() {
  trendChart?.resize()
  userStatusChart?.resize()
  postStatusChart?.resize()
  categoryChart?.resize()
  userGrowthChart?.resize()
  commentActivityChart?.resize()
}

onMounted(() => {
  fetchOverview()
  fetchTrends()
  fetchCategories()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  userStatusChart?.dispose()
  postStatusChart?.dispose()
  categoryChart?.dispose()
  userGrowthChart?.dispose()
  commentActivityChart?.dispose()
})
</script>

<style scoped>
.stats-row { margin-bottom: 0; }
.stat-card { border-radius: 12px; padding: 24px; color: #fff; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.stat-label { font-size: 13px; opacity: 0.85; margin-bottom: 4px; }
.stat-value { font-size: 28px; font-weight: 600; }
.stat-trend { font-size: 12px; opacity: 0.85; margin-top: 6px; }
.card-header { font-weight: 600; font-size: 15px; display: flex; justify-content: space-between; align-items: center; }
</style>
