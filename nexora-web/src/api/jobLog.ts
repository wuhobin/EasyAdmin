import request from '@/api/client'
import type { PageResult } from '@/types/api'

export interface JobLogQuery {
  pageNum: number
  pageSize: number
  jobName?: string
  jobGroup?: string
  status?: string
}

export interface JobLogRecord {
  logId: number
  jobId?: number
  jobName: string
  jobGroup?: string
  invokeTarget?: string
  startTime?: string
  stopTime?: string
  costMillis?: number
  jobMessage?: string
  status?: string
  exceptionInfo?: string
}

export const getJobLogListApi = (params: JobLogQuery) =>
  request<PageResult<JobLogRecord>>({ url: '/monitor/jobLog/list', method: 'get', params })

export const deleteJobLogApi = (ids: number | number[]) =>
  request<void>({ url: `/monitor/jobLog/delete/${ids}`, method: 'delete' })

export const cleanJobLogApi = () =>
  request<void>({ url: '/monitor/jobLog/clean', method: 'delete' })
