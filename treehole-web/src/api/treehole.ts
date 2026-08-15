import request from './index'

export interface Post {
  id: number
  categoryId?: number
  categoryName?: string
  content: string
  images?: string[]
  isAnonymous: number
  isTop: number
  status: number
  viewCount: number
  likeCount: number
  commentCount: number
  createTime: string
}

export interface Comment {
  id: number
  postId: number
  parentId?: number
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
  postCount: number
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

export function createPost(data: { content: string; categoryId?: number }) {
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

export function createComment(data: { postId: number; content: string; parentId?: number }) {
  return request.post('/th/comment', data) as Promise<{ data: number }>
}
