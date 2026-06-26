import request from './index'
import type { SysDept, ApiResult } from '@/types'

export function getDeptTree(params?: { keyword?: string; status?: number }): Promise<ApiResult<SysDept[]>> {
  return request.get('/dept/tree', { params })
}

export function getDeptTreeSelect(): Promise<ApiResult<SysDept[]>> {
  return request.get('/dept/tree-select')
}

export function getDeptById(id: number): Promise<ApiResult<SysDept>> {
  return request.get(`/dept/${id}`)
}

export function createDept(data: Partial<SysDept>): Promise<ApiResult<null>> {
  return request.post('/dept', data)
}

export function updateDept(id: number, data: Partial<SysDept>): Promise<ApiResult<null>> {
  return request.put(`/dept/${id}`, data)
}

export function deleteDept(id: number): Promise<ApiResult<null>> {
  return request.delete(`/dept/${id}`)
}
