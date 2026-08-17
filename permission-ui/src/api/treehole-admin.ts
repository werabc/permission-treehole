import request from './index'

// ==================== 用户管理 ====================
export function getThUserPage(params: { pageNum: number; pageSize: number; keyword?: string; status?: number }) {
  return request.get('/admin/th/user/page', { params })
}
export function getThUserDetail(id: number) {
  return request.get(`/admin/th/user/${id}`)
}
export function muteUser(id: number, hours?: number) {
  return request.put(`/admin/th/user/${id}/mute`, null, { params: { hours } })
}
export function unmuteUser(id: number) {
  return request.put(`/admin/th/user/${id}/unmute`)
}
export function banUser(id: number, days?: number) {
  return request.put(`/admin/th/user/${id}/ban`, null, { params: { days } })
}
export function unbanUser(id: number) {
  return request.put(`/admin/th/user/${id}/unban`)
}
export function getThUserLogs(id: number, params: { pageNum: number; pageSize: number }) {
  return request.get(`/admin/th/user/${id}/logs`, { params })
}
export function getThUserPosts(id: number, params: { pageNum: number; pageSize: number }) {
  return request.get(`/admin/th/user/${id}/posts`, { params })
}
export function getThUserComments(id: number, params: { pageNum: number; pageSize: number }) {
  return request.get(`/admin/th/user/${id}/comments`, { params })
}

// ==================== 内容审核 ====================
export function getPendingPosts(params: { pageNum: number; pageSize: number }) {
  return request.get('/admin/th/moderation/posts', { params })
}
export function getPendingComments(params: { pageNum: number; pageSize: number }) {
  return request.get('/admin/th/moderation/comments', { params })
}
export function batchAudit(data: { type: string; ids: number[]; status: number }) {
  return request.post('/admin/th/moderation/batch-audit', data)
}
export function getModerationStats() {
  return request.get('/admin/th/moderation/stats')
}

// ==================== 帖子管理 ====================
export function getThPostPage(params: { pageNum: number; pageSize: number; status?: number; categoryId?: number; keyword?: string }) {
  return request.get('/admin/th/post/page', { params })
}
export function getThPostDetail(id: number) {
  return request.get(`/admin/th/post/${id}`)
}
export function pinPost(id: number, isTop: number) {
  return request.put(`/admin/th/post/${id}/pin`, null, { params: { isTop } })
}
export function hidePost(id: number, status: number) {
  return request.put(`/admin/th/post/${id}/hide`, null, { params: { status } })
}
export function deleteThPost(id: number) {
  return request.delete(`/admin/th/post/${id}`)
}

// ==================== 评论管理 ====================
export function getThCommentPage(params: { pageNum: number; pageSize: number; postId?: number }) {
  return request.get('/admin/th/comment/page', { params })
}
export function getThCommentDetail(id: number) {
  return request.get(`/admin/th/comment/${id}`)
}
export function hideThComment(id: number, status: number) {
  return request.put(`/admin/th/comment/${id}/hide`, null, { params: { status } })
}
export function deleteThComment(id: number) {
  return request.delete(`/admin/th/comment/${id}`)
}

// ==================== 举报管理 ====================
export function getThReportPage(params: { pageNum: number; pageSize: number; status?: number }) {
  return request.get('/admin/th/report/page', { params })
}
export function getThReportDetail(id: number) {
  return request.get(`/admin/th/report/${id}`)
}
export function handleThReport(id: number, status: number, result?: string) {
  return request.put(`/admin/th/report/${id}/handle`, null, { params: { status, result } })
}
export function batchHandleReports(data: { ids: number[]; status: number; result?: string }) {
  return request.post('/admin/th/report/batch-handle', data)
}
export function getThReportStats() {
  return request.get('/admin/th/report/stats')
}

// ==================== 分类管理 ====================
export function getThCategoryList() {
  return request.get('/admin/th/category/list')
}
export function createThCategory(data: any) {
  return request.post('/admin/th/category', data)
}
export function updateThCategory(id: number, data: any) {
  return request.put(`/admin/th/category/${id}`, data)
}
export function deleteThCategory(id: number) {
  return request.delete(`/admin/th/category/${id}`)
}

// ==================== 公告管理 ====================
export function getThAnnouncementPage(params: { pageNum: number; pageSize: number; type?: string }) {
  return request.get('/admin/th/announcement/page', { params })
}
export function createThAnnouncement(data: any) {
  return request.post('/admin/th/announcement', data)
}
export function updateThAnnouncement(id: number, data: any) {
  return request.put(`/admin/th/announcement/${id}`, data)
}
export function deleteThAnnouncement(id: number) {
  return request.delete(`/admin/th/announcement/${id}`)
}

// ==================== 数据分析 ====================
export function getThAnalyticsOverview() {
  return request.get('/admin/th/analytics/overview')
}
export function getThAnalyticsTrends(days?: number) {
  return request.get('/admin/th/analytics/trends', { params: { days } })
}
export function getThAnalyticsCategories() {
  return request.get('/admin/th/analytics/categories')
}

// ==================== 站点配置 ====================
export function getThSettings() {
  return request.get('/admin/th/settings')
}
export function updateThSettings(data: any) {
  return request.put('/admin/th/settings', data)
}
