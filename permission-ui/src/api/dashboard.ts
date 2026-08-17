import request from './index'

export interface DashboardStats {
  overview: {
    userCount: number
    roleCount: number
    menuCount: number
    deptCount: number
  }
  userStatus: {
    active: number
    inactive: number
  }
  deptUserCount: Array<{ name: string; count: number }>
  loginTrend: {
    dates: string[]
    counts: number[]
  }
}

export interface TreeholeStats {
  overview: {
    userCount: number
    postCount: number
    commentCount: number
    reportCount: number
  }
  postStatus: {
    approved: number
    pending: number
    rejected: number
  }
  userStatus: {
    active: number
    banned: number
  }
  pendingReports: number
}

export interface AdminStats {
  overview: {
    userCount: number
    roleCount: number
    menuCount: number
    deptCount: number
  }
  userStatus: {
    active: number
    inactive: number
  }
  deptUserCount: Array<{ name: string; count: number }>
  loginTrend: {
    dates: string[]
    counts: number[]
  }
}

export interface PendingItems {
  pendingPosts: number
  pendingReports: number
}

export interface RealtimeStats {
  newUsersToday: number
  newPostsToday: number
  newCommentsToday: number
}

export interface TrendData {
  dates: string[]
  userTrend: number[]
  postTrend: number[]
  commentTrend: number[]
}

export interface DashboardOverview {
  admin: AdminStats
  treehole: TreeholeStats
  pending: PendingItems
  trends: TrendData
}

// 综合仪表盘数据
export function getDashboardOverview(): Promise<{ data: DashboardOverview }> {
  return request.get('/dashboard/overview')
}

// 管理员系统统计
export function getAdminStats(): Promise<{ data: AdminStats }> {
  return request.get('/dashboard/admin-stats')
}

// 树洞系统统计
export function getTreeholeStats(): Promise<{ data: TreeholeStats }> {
  return request.get('/dashboard/treehole-stats')
}

// 待处理事项
export function getPendingItems(): Promise<{ data: PendingItems }> {
  return request.get('/dashboard/pending')
}

// 趋势数据
export function getTrends(days?: number): Promise<{ data: TrendData }> {
  return request.get('/dashboard/trends', { params: { days } })
}

// 实时统计
export function getRealtimeStats(): Promise<{ data: RealtimeStats }> {
  return request.get('/dashboard/realtime')
}

// 管理员系统统计(兼容旧接口)
export function getDashboardStatistics(): Promise<{ data: DashboardStats }> {
  return request.get('/dashboard/statistics')
}
