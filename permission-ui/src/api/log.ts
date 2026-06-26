import request from './index'
import type { OperationLog, LoginLog, ApiResult, PageResult } from '@/types'

export interface LogQuery {
  pageNum: number
  pageSize: number
  keyword?: string
  module?: string
  startDate?: string
  endDate?: string
}

export function getOperationLogPage(params: LogQuery): Promise<ApiResult<PageResult<OperationLog>>> {
  return request.get('/log/operation/page', { params })
}

export function getLoginLogPage(params: LogQuery): Promise<ApiResult<PageResult<LoginLog>>> {
  return request.get('/log/login/page', { params })
}
