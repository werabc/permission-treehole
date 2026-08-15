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

export function getDashboardStatistics(): Promise<{ data: DashboardStats }> {
  return request.get('/dashboard/statistics')
}
