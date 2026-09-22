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
  /** 数据权限范围，见后端 DataScope 枚举：1全部 2本集团及以下 3本公司及以下 4本部门及以下 5本部门及以下(限N级) 6本部门 7自定义部门 8仅本人 */
  dataScope: number
  /** dataScope=5 时的层级深度 N */
  dataScopeLevel?: number
  /** 自定义数据范围时逗号分隔的部门ID串 */
  deptIds?: string
  /** 数据范围名称（后端回填） */
  dataScopeName?: string
  /** 是否内置角色（不可删除/不可改编码） */
  builtin?: boolean
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
  /** 组织层级：1-集团 2-公司 3-部门 4+-小组 */
  deptLevel?: number
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
