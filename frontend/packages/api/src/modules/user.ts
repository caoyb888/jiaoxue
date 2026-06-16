import { http } from '../client'

export interface UserVO {
  id: number
  studentNo?: string
  username: string
  realName: string
  email?: string
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

export const userApi = {
  getUsers: (params: UserQueryParams) =>
    http.get<UserQueryParams, PageResult<UserVO>>('/v1/users', { params }),

  getUserById: (userId: number) =>
    http.get<void, UserVO>(`/v1/users/${userId}`),
}
