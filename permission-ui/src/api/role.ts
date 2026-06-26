import request from './index'
import type { SysRole, ApiResult, PageResult } from '@/types'

export function getRolePage(params: { pageNum: number; pageSize: number; keyword?: string }): Promise<ApiResult<PageResult<SysRole>>> {
  return request.get('/role/page', { params })
}

export function getRoleById(id: number): Promise<ApiResult<SysRole>> {
  return request.get(`/role/${id}`)
}

export function createRole(data: Partial<SysRole>): Promise<ApiResult<null>> {
  return request.post('/role', data)
}

export function updateRole(id: number, data: Partial<SysRole>): Promise<ApiResult<null>> {
  return request.put(`/role/${id}`, data)
}

export function deleteRoles(ids: number[]): Promise<ApiResult<null>> {
  return request.delete(`/role/${ids.join(',')}`)
}

export function updateRoleStatus(id: number, status: number): Promise<ApiResult<null>> {
  return request.put(`/role/${id}/status`, { status })
}

export function assignMenus(id: number, menuIds: number[]): Promise<ApiResult<null>> {
  return request.put(`/role/${id}/menus`, { menuIds })
}

export function getRoleMenuIds(id: number): Promise<ApiResult<number[]>> {
  return request.get(`/role/${id}/menus`)
}

export function getAllRoles(): Promise<ApiResult<SysRole[]>> {
  return request.get('/role/all')
}
