import request from './index'

export function getAdminPostPage(params: { pageNum: number; pageSize: number; status?: number }) {
  return request.get('/admin/treehole/post/page', { params })
}

export function auditPost(id: number, status: number) {
  return request.put(`/admin/treehole/post/${id}/audit`, null, { params: { status } })
}

export function deletePost(id: number) {
  return request.delete(`/admin/treehole/post/${id}`)
}

export function getAdminCommentPage(params: { pageNum: number; pageSize: number }) {
  return request.get('/admin/treehole/comment/page', { params })
}

export function deleteComment(id: number) {
  return request.delete(`/admin/treehole/comment/${id}`)
}

export function getAdminCategoryList() {
  return request.get('/admin/treehole/category/list')
}

export function createCategory(data: any) {
  return request.post('/admin/treehole/category', data)
}

export function updateCategory(id: number, data: any) {
  return request.put(`/admin/treehole/category/${id}`, data)
}

export function deleteCategory(id: number) {
  return request.delete(`/admin/treehole/category/${id}`)
}

export function getAdminReportPage(params: { pageNum: number; pageSize: number }) {
  return request.get('/admin/treehole/report/page', { params })
}

export function handleReport(id: number, status: number, result?: string) {
  return request.put(`/admin/treehole/report/${id}/handle`, null, { params: { status, result } })
}

export function getStatistics() {
  return request.get('/admin/treehole/statistics')
}
