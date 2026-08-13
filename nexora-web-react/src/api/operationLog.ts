import request from '@/api/client'
import type { PageResult } from '@/types/api'

export interface OperationLogQuery {
  pageNum: number
  pageSize: number
  userId?: number
}

export interface OperationLogRecord {
  id: number
  userId: number
  requestUrl?: string
  type?: string
  operationName?: string
  ip?: string
  source?: string
  spendTime?: number
  paramsJson?: string
  classPath?: string
  methodName?: string
  createTime?: string
  updateTime?: string
  params?: Record<string, unknown>
}

export const getOperationLogListApi = (params: OperationLogQuery) =>
  request<PageResult<OperationLogRecord>>({ url: '/sys/operateLog', method: 'get', params })

export const deleteOperationLogApi = (ids: number | number[]) =>
  request<void>({ url: `/sys/operateLog/delete/${ids}`, method: 'delete' })
