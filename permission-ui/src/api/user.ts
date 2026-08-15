import request from './index'
import type { SysUser, ApiResult, PageResult } from '@/types'

export interface UserQuery {
  pageNum: number
  pageSize: number
  keyword?: string
  deptId?: number
  status?: number
}

export function getUserPage(params: UserQuery): Promise<ApiResult<PageResult<SysUser>>> {
  return request.get('/user/page', { params })
}

export function getUserById(id: number): Promise<ApiResult<SysUser>> {
  return request.get(`/user/${id}`)
}

export function createUser(data: Partial<SysUser>): Promise<ApiResult<null>> {
  return request.post('/user', data)
}

export function updateUser(id: number, data: Partial<SysUser>): Promise<ApiResult<null>> {
  return request.put(`/user/${id}`, data)
}

export function deleteUsers(ids: number[]): Promise<ApiResult<null>> {
  return request.delete(`/user/${ids.join(',')}`)
}

export function updateUserStatus(id: number, status: number): Promise<ApiResult<null>> {
  return request.put(`/user/${id}/status`, { status })
}

export function resetPassword(id: number, password: string): Promise<ApiResult<null>> {
  return request.put(`/user/${id}/reset-password`, { password })
}

export function updatePassword(oldPassword: string, newPassword: string): Promise<ApiResult<null>> {
  return request.put('/user/update-password', { oldPassword, newPassword })
}

export function assignRoles(id: number, roleIds: number[]): Promise<ApiResult<null>> {
  return request.put(`/user/${id}/roles`, { roleIds })
}

export function getUserRoleIds(id: number): Promise<ApiResult<number[]>> {
  return request.get(`/user/${id}/roles`)
}

export function exportUsers(): Promise<Blob> {
  return request.get('/user/export', { responseType: 'blob' })
}

export function batchUpdateStatus(ids: number[], status: number): Promise<ApiResult<null>> {
  return request.put('/user/batch-status', { ids, status })
}
