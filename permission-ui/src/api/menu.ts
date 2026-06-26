import request from './index'
import type { SysMenu, ApiResult } from '@/types'

export function getMenuTree(params?: { keyword?: string; status?: number }): Promise<ApiResult<SysMenu[]>> {
  return request.get('/menu/tree', { params })
}

export function getMenuTreeSelect(): Promise<ApiResult<SysMenu[]>> {
  return request.get('/menu/tree-select')
}

export function getUserMenus(): Promise<ApiResult<SysMenu[]>> {
  return request.get('/menu/user-menus')
}

export function getMenuById(id: number): Promise<ApiResult<SysMenu>> {
  return request.get(`/menu/${id}`)
}

export function createMenu(data: Partial<SysMenu>): Promise<ApiResult<null>> {
  return request.post('/menu', data)
}

export function updateMenu(id: number, data: Partial<SysMenu>): Promise<ApiResult<null>> {
  return request.put(`/menu/${id}`, data)
}

export function deleteMenu(id: number): Promise<ApiResult<null>> {
  return request.delete(`/menu/${id}`)
}
