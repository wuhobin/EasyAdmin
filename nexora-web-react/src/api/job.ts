import request from '@/api/client'
import type { PageResult } from '@/types/api'

export interface JobQuery {
  pageNum: number
  pageSize: number
  jobName?: string
  jobGroup?: string
  status?: string
}

export interface JobRecord {
  jobId: number
  jobName: string
  jobGroup: string
  invokeTarget: string
  cronExpression: string
  misfirePolicy: string
  concurrent: string
  status: string
}

export type JobPayload = JobRecord | Omit<JobRecord, 'jobId'> & { jobId?: number }

export const getJobListApi = (params: JobQuery) =>
  request<PageResult<JobRecord>>({ url: '/monitor/job/list', method: 'get', params })

export const getJobDetailApi = (jobId: number) =>
  request<JobRecord>({ url: `/monitor/job/${jobId}`, method: 'get' })

export const addJobApi = (data: JobPayload) =>
  request<JobRecord>({ url: '/monitor/job', method: 'post', data })

export const updateJobApi = (data: JobPayload) =>
  request<void>({ url: '/monitor/job', method: 'put', data })

export const deleteJobApi = (ids: number | number[]) =>
  request<void>({ url: `/monitor/job/delete/${Array.isArray(ids) ? ids.join(',') : ids}`, method: 'delete' })

export const changeJobStatusApi = (jobId: number, status: string) =>
  request<void>({ url: '/monitor/job/changeStatus', method: 'put', data: { jobId, status } })

export const runJobApi = (job: Pick<JobRecord, 'jobId' | 'jobGroup'>) =>
  request<void>({ url: '/monitor/job/run', method: 'put', data: job })
