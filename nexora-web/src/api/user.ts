import request from '@/api/client'
import type { PageResult } from '@/types/api'

export interface UserQuery {
  pageNum: number
  pageSize: number
  nickname?: string
  email?: string
  status?: number
}

export interface SysUserRecord {
  id: number
  status: number
  lastLoginTime?: string
  nickname: string
  avatar?: string
  ip?: string
  ipLocation?: string
  mobile?: string
  email: string | null
  sex: number
  roleIds: number[]
  createTime?: string
}

export interface SysUserPayload {
  id?: number
  nickname?: string
  email?: string
  password?: string
  mobile?: string
  sex?: number
  status?: number
  roleIds?: number[]
}

export interface UserProfileRecord {
  id: number
  status: number
  nickname: string
  avatar?: string
  mobile?: string
  email: string | null
  sex: number
  createTime?: string
  lastLoginTime?: string
}

export interface UserProfileResult {
  sysUser: UserProfileRecord
  roles: string[]
}

export interface UserProfilePayload {
  nickname: string
  mobile?: string
  sex: number
  avatar?: string
}

export const getUserListApi = (params: UserQuery) =>
  request<PageResult<SysUserRecord>>({ url: '/sys/user', method: 'get', params })

export const createUserApi = (data: SysUserPayload) =>
  request<void>({ url: '/sys/user', method: 'post', data })

export const updateUserApi = (data: SysUserPayload) =>
  request<void>({ url: '/sys/user', method: 'put', data })

export const deleteUserApi = (ids: number | number[]) =>
  request<void>({ url: `/sys/user/delete/${ids}`, method: 'delete' })

export const resetPasswordApi = (id: number, password: string) =>
  request<boolean>({ url: '/sys/user/reset', method: 'put', data: { id, password } })

export const auditUserApi = (id: number) =>
  request<void>({ url: `/sys/user/audit/${id}`, method: 'put' })

export const verifyPasswordApi = (password: string) =>
  request<boolean>({ url: `/sys/user/verifyPassword/${encodeURIComponent(password)}`, method: 'get' })

export const getUserProfileApi = () =>
  request<UserProfileResult>({ url: '/sys/user/profile', method: 'get' })

export const updateUserProfileApi = (data: UserProfilePayload) =>
  request<void>({ url: '/sys/user/updProfile', method: 'put', data })

export const updateUserPasswordApi = (oldPassword: string, newPassword: string) =>
  request<void>({ url: '/sys/user/updatePwd', method: 'put', data: { oldPassword, newPassword } })

export const sendChangeEmailCodeApi = (email: string) =>
  request<void>({ url: '/sys/user/profile/email/sendCode', method: 'post', data: { email } })

export const changeEmailApi = (email: string, code: string) =>
  request<void>({ url: '/sys/user/profile/changeEmail', method: 'put', data: { email, code } })

export const bindEmailApi = (email: string, code: string, password: string) =>
  request<void>({ url: '/sys/user/profile/bindEmail', method: 'put', data: { email, code, password } })
