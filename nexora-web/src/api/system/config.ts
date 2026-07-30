import request, { type ApiResponse } from '@/utils/request'

export interface SysConfigRecord {
  id: number
  configKey: string
  configValue: string
  remark?: string
  createTime: string
  updateTime: string
}

export interface SysConfigQuery {
  pageNum: number
  pageSize: number
  configKey?: string
}

export interface SysConfigForm {
  id?: number
  configKey: string
  configValue: string
  remark?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

export function getConfigListApi(params: SysConfigQuery) {
  return request<PageResult<SysConfigRecord>>({
    url: '/sys/config',
    method: 'get',
    params
  })
}

export function getConfigValueApi(configKey: string): Promise<ApiResponse<string | null>> {
  return request<string | null>({
    url: `/sys/config/value/${encodeURIComponent(configKey)}`,
    method: 'get'
  })
}

export function addConfigApi(data: SysConfigForm) {
  return request<void>({
    url: '/sys/config/add',
    method: 'post',
    data
  })
}

export function updateConfigApi(data: SysConfigForm) {
  return request<void>({
    url: '/sys/config/update',
    method: 'put',
    data
  })
}

export function deleteConfigApi(id: number) {
  return request<void>({
    url: `/sys/config/delete/${id}`,
    method: 'delete'
  })
}
