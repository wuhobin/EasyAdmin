import request from '@/api/client'

export type MenuType = 'CATALOG' | 'MENU' | 'BUTTON'

export interface SysMenuRecord {
  id: number
  parentId: number
  path: string
  component: string
  title: string
  sort: number
  icon: string
  type: MenuType
  redirect: string
  name: string
  hidden: number
  isExternal: number
  perm: string
  children?: SysMenuRecord[]
}

export interface SysMenuPayload extends Partial<SysMenuRecord> {
  parentId: number
  path?: string
  component?: string
  title: string
  sort: number
  icon?: string
  type: MenuType
  hidden: number
  isExternal: number
  perm?: string
}

export const getMenuTreeApi = () => request<SysMenuRecord[]>({ url: '/sys/menu/tree', method: 'get' })

export const createMenuApi = (data: SysMenuPayload) =>
  request<void>({ url: '/sys/menu', method: 'post', data })

export const updateMenuApi = (data: SysMenuPayload) =>
  request<void>({ url: '/sys/menu', method: 'put', data })

export const deleteMenuApi = (id: number) =>
  request<void>({ url: `/sys/menu/${id}`, method: 'delete' })
