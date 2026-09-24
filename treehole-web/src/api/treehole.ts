import request from './index'

export interface Post {
  id: number
  userId?: number
  authorName?: string
  /** 作者头像；匿名帖后端恒为空，用于避免去匿名化 */
  authorAvatar?: string
  categoryId?: number
  categoryName?: string
  title?: string
  content: string
  images?: string[]
  isAnonymous: number
  isTop: number
  status: number
  viewCount: number
  likeCount: number
  commentCount: number
  reportCount?: number
  createTime: string
}

export interface Comment {
  id: number
  postId: number
  userId?: number
  authorName?: string
  /** 作者头像；匿名评论后端恒为空 */
  authorAvatar?: string
  /** 当前登录用户是否已点赞（由列表接口回填） */
  liked?: boolean
  parentId?: number
  replyUserId?: number
  replyUserName?: string
  content: string
  isAnonymous: number
  likeCount: number
  createTime: string
}

export interface Category {
  id: number
  name: string
  code: string
  icon?: string
  description?: string
  sort?: number
  postCount: number
}

export interface ReportPayload {
  targetType: 'POST' | 'COMMENT'
  targetId: number
  reason: string
  description?: string
}

export function getCategoryList() {
  return request.get('/th/category/list') as Promise<{ data: Category[] }>
}

export function getActiveAnnouncements() {
  return request.get('/th/announcements') as Promise<{ data: Announcement[] }>
}

export interface Announcement {
  id: number
  title: string
  content: string
  type: string
  status: number
  publishTime: string
  expireTime: string
  createTime: string
}

export function getPostPage(params: { pageNum: number; pageSize: number; categoryId?: number; keyword?: string }) {
  return request.get('/th/post/page', { params }) as Promise<{ data: { records: Post[]; total: number } }>
}

export function getPostDetail(id: number) {
  return request.get(`/th/post/${id}`) as Promise<{ data: Post }>
}

export function createPost(data: { content: string; categoryId?: number; isAnonymous?: number }) {
  return request.post('/th/post', data) as Promise<{ data: number }>
}

export function likePost(id: number) {
  return request.post(`/th/post/${id}/like`)
}

export function unlikePost(id: number) {
  return request.delete(`/th/post/${id}/like`)
}

export function getCommentPage(params: { pageNum: number; pageSize: number; postId: number }) {
  return request.get('/th/comment/page', { params }) as Promise<{ data: { records: Comment[]; total: number } }>
}

export function createComment(data: { postId: number; content: string; parentId?: number; replyUserId?: number; isAnonymous?: number }) {
  return request.post('/th/comment', data) as Promise<{ data: number }>
}

export function likeComment(id: number) {
  return request.post(`/th/comment/${id}/like`)
}

/** 取消点赞评论（后端原先只有点赞、没有取消，属接口缺口） */
export function unlikeComment(id: number) {
  return request.delete(`/th/comment/${id}/like`)
}

/**
 * 查询当前用户是否已点赞该帖子。
 * 后端一直有这个接口，但前端从未调用 —— 导致刷新后"已抱抱"状态丢失，
 * 且点赞数会被本地乐观自增算错。
 */
export function isPostLiked(id: number) {
  return request.get(`/th/post/${id}/liked`) as Promise<{ data: boolean }>
}

export function submitReport(data: ReportPayload) {
  return request.post('/th/report', data)
}

export function getMyPosts(params: { pageNum: number; pageSize: number }) {
  return request.get('/th/user/posts', { params }) as Promise<{ data: { records: any[]; total: number } }>
}

export function getReceivedComments(params: { pageNum: number; pageSize: number }) {
  return request.get('/th/user/received-comments', { params }) as Promise<{ data: { records: any[]; total: number } }>
}

export function getMyComments(params: { pageNum: number; pageSize: number }) {
  return request.get('/th/user/my-comments', { params }) as Promise<{ data: { records: any[]; total: number } }>
}

export function getNotifications(params: { pageNum: number; pageSize: number; unreadOnly?: boolean }) {
  return request.get('/th/user/notifications', { params }) as Promise<{ data: { records: any[]; total: number } }>
}

export function getUnreadCount() {
  return request.get('/th/user/unread-count') as Promise<{ data: number }>
}

export function markNotificationsRead(ids: number[]) {
  return request.put('/th/user/notifications/read', { ids })
}

export function updateProfile(data: { nickname?: string; bio?: string; gender?: number; avatar?: string; email?: string }) {
  return request.put('/th/user/profile', data)
}

// ==================== v1.1.0 新增 ====================

/** 搜索帖子（内容 + 作者昵称） */
export function searchPosts(params: { keyword: string; pageNum: number; pageSize: number }) {
  return request.get('/th/search', { params }) as Promise<{ data: { records: Post[]; total: number } }>
}

/** 删除自己的帖子 */
export function deletePost(id: number) {
  return request.delete(`/th/post/${id}`)
}

/** 删除自己的评论 */
export function deleteComment(id: number) {
  return request.delete(`/th/comment/${id}`)
}

/** 切换收藏状态，返回 true=已收藏 */
export function toggleCollect(postId: number) {
  return request.post(`/th/collect/${postId}`) as Promise<{ data: boolean }>
}

/** 是否已收藏 */
export function isCollected(postId: number) {
  return request.get(`/th/post/${postId}/collected`) as Promise<{ data: boolean }>
}

/** 我收藏的帖子 */
export function getMyCollects(params: { pageNum: number; pageSize: number }) {
  return request.get('/th/user/collects', { params }) as Promise<{ data: { records: Post[]; total: number } }>
}

/** 收藏数量 */
export function getCollectCount() {
  return request.get('/th/user/collect-count') as Promise<{ data: number }>
}

/**
 * 上传头像。
 * 注意：这里必须让浏览器自己带 Content-Type（含 boundary），
 * 手动写 'multipart/form-data' 会丢掉 boundary，后端直接报 400。
 */
export function uploadAvatar(file: File) {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/file/avatar', fd) as Promise<{ data: string }>
}

/** 他人公开主页信息 */
export function getPublicProfile(userId: number) {
  return request.get(`/th/user/public/${userId}`) as Promise<{ data: PublicProfile }>
}

/** 指定用户的公开帖子 */
export function getUserPosts(userId: number, params: { pageNum: number; pageSize: number }) {
  return request.get(`/th/user/${userId}/posts`, { params }) as Promise<{ data: { records: Post[]; total: number } }>
}

export interface PublicProfile {
  id: number
  nickname: string
  avatar?: string
  bio?: string
  gender?: number
  postCount: number
  commentCount: number
  createTime: string
}

export interface NotificationItem {
  id: number
  senderId?: number
  type: string
  targetType?: string
  targetId?: number
  content: string
  isRead: number
  createTime: string
}
