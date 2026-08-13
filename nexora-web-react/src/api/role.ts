import request from '@/api/client'
import type { PageResult } from '@/types/api'

export interface RoleQuery {
  pageNum: number
  pageSize: number
  name?: string
}

export interface SysRoleRecord {
  id: number
  code: string
  name: string
  remarks?: string
  createTime?: string
  updateTime?: string
}

export interface SysRolePayload {
  id?: number
  code: string
  name: string
  remarks?: string
}

export const getRoleListApi = (params: RoleQuery) =>
  request<PageResult<SysRoleRecord>>({ url: '/sys/role/', method: 'get', params })

export const createRoleApi = (data: SysRolePayload) =>
  request<void>({ url: '/sys/role/', method: 'post', data })

export const updateRoleApi = (data: SysRolePayload) =>
  request<void>({ url: '/sys/role/', method: 'put', data })

export const deleteRoleApi = (ids: number | number[]) =>
  request<void>({ url: `/sys/role/delete/${ids}`, method: 'delete' })

export const getRoleMenusApi = (roleId: number) =>
  request<number[]>({ url: `/sys/role/menus/${roleId}`, method: 'get' })

export const updateRoleMenusApi = (roleId: number, menuIds: number[]) =>
  request<void>({ url: `/sys/role/menus/${roleId}`, method: 'put', data: menuIds })

export const getAllRoleListApi = () =>
  request<SysRoleRecord[]>({ url: '/sys/role/all', method: 'get' })

export const exportRoleApi = () =>
  request<Blob>({ url: '/sys/role/export', method: 'get', responseType: 'blob' }) as unknown as Promise<Blob>
