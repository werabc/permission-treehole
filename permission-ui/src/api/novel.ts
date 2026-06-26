import request from './index'
import type { ApiResult, PageResult, Novel, NovelCategory, NovelChapter, UserBookshelf, ReadingHistory, NovelComment, ChapterNav } from '@/types'

// ============ Category ============
export function getCategoryList(): Promise<ApiResult<NovelCategory[]>> {
  return request.get('/novel/category/list')
}

export function getCategoryPage(params: { pageNum: number; pageSize: number }): Promise<ApiResult<PageResult<NovelCategory>>> {
  return request.get('/novel/category/page', { params })
}

export function createCategory(data: Partial<NovelCategory>): Promise<ApiResult<null>> {
  return request.post('/novel/category', data)
}

export function updateCategory(id: number, data: Partial<NovelCategory>): Promise<ApiResult<null>> {
  return request.put(`/novel/category/${id}`, data)
}

export function deleteCategories(ids: number[]): Promise<ApiResult<null>> {
  return request.delete(`/novel/category/${ids.join(',')}`)
}

// ============ Novel ============
export function getPublishedNovels(params: { pageNum: number; pageSize: number; keyword?: string; categoryId?: number }): Promise<ApiResult<PageResult<Novel>>> {
  return request.get('/novel/published', { params })
}

export function getNovelPage(params: { pageNum: number; pageSize: number; keyword?: string; categoryId?: number; status?: number }): Promise<ApiResult<PageResult<Novel>>> {
  return request.get('/novel/page', { params })
}

export function getMyNovels(params: { pageNum: number; pageSize: number }): Promise<ApiResult<PageResult<Novel>>> {
  return request.get('/novel/my', { params })
}

export function getNovelDetail(id: number): Promise<ApiResult<Novel>> {
  return request.get(`/novel/${id}`)
}

export function createNovel(data: { title: string; categoryId: number; intro: string; coverUrl?: string }): Promise<ApiResult<null>> {
  return request.post('/novel', data)
}

export function updateNovel(id: number, data: Partial<Novel>): Promise<ApiResult<null>> {
  return request.put(`/novel/${id}`, data)
}

export function deleteNovels(ids: number[]): Promise<ApiResult<null>> {
  return request.delete(`/novel/${ids.join(',')}`)
}

// ============ Chapter ============
export function getChaptersByNovel(novelId: number): Promise<ApiResult<NovelChapter[]>> {
  return request.get(`/novel/chapter/list/${novelId}`)
}

export function getChapterDetail(id: number): Promise<ApiResult<NovelChapter>> {
  return request.get(`/novel/chapter/${id}`)
}

export function getChapterNav(id: number): Promise<ApiResult<ChapterNav>> {
  return request.get(`/novel/chapter/${id}/nav`)
}

export function createChapter(data: { novelId: number; chapterTitle: string; content: string; isFree?: number }): Promise<ApiResult<null>> {
  return request.post('/novel/chapter', data)
}

export function updateChapter(id: number, data: { chapterTitle?: string; content?: string }): Promise<ApiResult<null>> {
  return request.put(`/novel/chapter/${id}`, data)
}

export function deleteChapter(id: number): Promise<ApiResult<null>> {
  return request.delete(`/novel/chapter/${id}`)
}

// ============ Bookshelf ============
export function getBookshelf(): Promise<ApiResult<UserBookshelf[]>> {
  return request.get('/bookshelf')
}

export function checkBookshelf(novelId: number): Promise<ApiResult<boolean>> {
  return request.get(`/bookshelf/check/${novelId}`)
}

export function addBookshelf(novelId: number): Promise<ApiResult<null>> {
  return request.post(`/bookshelf/${novelId}`)
}

export function removeBookshelf(novelId: number): Promise<ApiResult<null>> {
  return request.delete(`/bookshelf/${novelId}`)
}

// ============ Reading History ============
export function getReadingHistory(): Promise<ApiResult<ReadingHistory[]>> {
  return request.get('/reading-history')
}

export function saveReadingHistory(data: { novelId: number; chapterId: number; chapterTitle: string }): Promise<ApiResult<null>> {
  return request.post('/reading-history', data)
}

// ============ Comment ============
export function getComments(novelId: number): Promise<ApiResult<NovelComment[]>> {
  return request.get(`/novel/comment/list/${novelId}`)
}

export function addComment(data: { novelId: number; content: string; parentId?: number; chapterId?: number }): Promise<ApiResult<null>> {
  return request.post('/novel/comment', data)
}

export function deleteComment(id: number): Promise<ApiResult<null>> {
  return request.delete(`/novel/comment/${id}`)
}
