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
  avatar?: string
  email?: string
  phone?: string
  sex?: number
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
