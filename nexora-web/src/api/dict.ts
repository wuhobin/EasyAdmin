import request from '@/api/client'
import type { PageResult } from '@/types/api'

export interface DictQuery {
  pageNum: number
  pageSize: number
  name?: string
  status?: number
}

export interface SysDictRecord {
  id: number
  name: string
  type: string
  status: number
  remark?: string
  sort?: number
  createTime?: string
  updateTime?: string
}

export interface SysDictPayload {
  id?: number
  name: string
  type: string
  status: number
  remark?: string
  sort?: number
}

export interface DictDataQuery {
  pageNum: number
  pageSize: number
  dictId: number
}

export interface SysDictDataRecord {
  id: number
  dictId: number
  label: string
  value: string
  style?: string
  isDefault?: string
  sort: number
  remark?: string
  status: number
}

export interface SysDictDataPayload {
  id?: number
  dictId: number
  label: string
  value: string
  sort: number
  status: number
  remark?: string
  style?: string
  isDefault?: string
}

export const getDictListApi = (params: DictQuery) =>
  request<PageResult<SysDictRecord>>({ url: '/sys/dict', method: 'get', params })

export const addDictApi = (data: SysDictPayload) =>
  request<void>({ url: '/sys/dict/add', method: 'post', data })

export const updateDictApi = (data: SysDictPayload) =>
  request<void>({ url: '/sys/dict/update', method: 'put', data })

export const deleteDictApi = (ids: number | number[]) =>
  request<void>({ url: `/sys/dict/delete/${ids}`, method: 'delete' })

export const getDictDataListApi = (params: DictDataQuery) =>
  request<PageResult<SysDictDataRecord>>({ url: '/sys/dictData/list', method: 'get', params })

export const addDictDataApi = (data: SysDictDataPayload) =>
  request<void>({ url: '/sys/dictData/add', method: 'post', data })

export const updateDictDataApi = (data: SysDictDataPayload) =>
  request<void>({ url: '/sys/dictData/update', method: 'put', data })

export const deleteDictDataApi = (ids: number | number[]) =>
  request<void>({ url: `/sys/dictData/delete/${ids}`, method: 'delete' })
