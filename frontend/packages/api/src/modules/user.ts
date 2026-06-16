import { http } from '../client'

export interface UserVO {
  id: number
  studentNo?: string
  username: string
  realName: string
  email?: string
  phone?: string
  userType: number
  deptId?: number
  deptName?: string
  avatarUrl?: string
  status: number
  roles: string[]
  lastLoginAt?: string
  createdAt: string
}

export interface UserQueryParams {
  keyword?: string
  userType?: number
  deptId?: number
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface DeptTreeNode {
  id: number
  deptName: string
  parentId: number | null
  sort: number
  children?: DeptTreeNode[]
}

export interface RoleVO {
  id: number
  roleName: string
  roleCode: string
}

export const userApi = {
  getUsers: (params: UserQueryParams) =>
    http.get<UserQueryParams, PageResult<UserVO>>('/v1/users', { params }),

  getUserById: (userId: number) =>
    http.get<void, UserVO>(`/v1/users/${userId}`),

  updateStatus: (userId: number, status: number) =>
    http.put<void, void>(`/v1/users/${userId}/status`, { status }),

  getDeptTree: () =>
    http.get<void, DeptTreeNode[]>('/v1/depts/tree'),

  getRoles: () =>
    http.get<void, RoleVO[]>('/v1/roles'),

  assignRoles: (userId: number, roleIds: number[]) =>
    http.put<void, void>(`/v1/users/${userId}/roles`, { roleIds }),
}
