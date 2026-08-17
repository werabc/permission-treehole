import request from './index'

export interface Post {
  id: number
  userId?: number
  authorName?: string
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

export function updateProfile(data: { nickname?: string; bio?: string; gender?: number }) {
  return request.put('/th/user/profile', data)
}
