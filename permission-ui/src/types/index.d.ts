export interface LoginForm {
  username: string
  password: string
  captchaKey?: string
  captchaCode?: string
}

export interface TokenVO {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

export interface UserInfo {
  userId: number
  username: string
  nickname: string
  deptId: number | null
  deptName: string
  permissions: string[]
  roles: string[]
}

export interface SysUser {
  id: number
  username: string
  nickname: string
  email: string
  phone: string
  avatar: string
  sex: number
  status: number
  deptId: number | null
  deptName: string
  lastLoginTime: string
  lastLoginIp: string
  createTime: string
}

export interface SysRole {
  id: number
  roleName: string
  roleCode: string
  roleDesc: string
  dataScope: number
  status: number
  createTime: string
}

export interface SysMenu {
  id: number
  parentId: number | null
  menuName: string
  menuType: string
  path: string
  component: string
  icon: string
  permission: string
  sort: number
  status: number
  visible: number
  children: SysMenu[]
}

export interface SysDept {
  id: number
  deptName: string
  parentId: number | null
  ancestors: string
  sort: number
  leader: string
  phone: string
  email: string
  status: number
  children: SysDept[]
}

export interface OperationLog {
  id: number
  module: string
  action: string
  method: string
  requestUrl: string
  requestMethod: string
  requestParams: string
  responseResult: string
  executeTime: number
  operator: string
  operatorIp: string
  status: number
  errorMsg: string
  createTime: string
}

export interface LoginLog {
  id: number
  username: string
  ip: string
  location: string
  browser: string
  os: string
  status: number
  message: string
  loginTime: string
}

// Novel types
export interface NovelCategory {
  id: number
  categoryName: string
  categoryDesc: string
  sort: number
  status: number
}

export interface Novel {
  id: number
  title: string
  authorId: number
  authorName: string
  categoryId: number
  categoryName: string
  coverUrl: string
  intro: string
  status: number
  wordCount: number
  clickCount: number
  likeCount: number
  lastChapterTitle: string
  createTime: string
  updateTime: string
}

export interface NovelChapter {
  id: number
  novelId: number
  novelTitle: string
  chapterTitle: string
  chapterNum: number
  content: string
  wordCount: number
  isFree: number
  createTime: string
}

export interface UserBookshelf {
  id: number
  userId: number
  novelId: number
  novelTitle: string
  coverUrl: string
  authorName: string
  lastChapterTitle: string
  createTime: string
}

export interface ReadingHistory {
  id: number
  userId: number
  novelId: number
  chapterId: number
  chapterTitle: string
  novelTitle: string
  coverUrl: string
  authorName: string
  updateTime: string
}

export interface NovelComment {
  id: number
  novelId: number
  chapterId: number
  userId: number
  userName: string
  content: string
  parentId: number
  likeCount: number
  createTime: string
  children: NovelComment[]
}

export interface ChapterNav {
  prev: NovelChapter | null
  next: NovelChapter | null
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}
