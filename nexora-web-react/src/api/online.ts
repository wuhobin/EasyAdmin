import request from '@/api/client'
import type { PageResult } from '@/types/api'

export interface OnlineSessionQuery {
  pageNum: number
  pageSize: number
  keyword?: string
  ip?: string
}

export interface OnlineSessionRecord {
  sessionId: string
  email?: string
  nickname?: string
  ip?: string
  location?: string
  browser?: string
  os?: string
  loginTime?: string
  lastAccessTime?: string
  currentSession: boolean
}

export type ForceLogoutOutcome = 'LOGGED_OUT' | 'ALREADY_OFFLINE'

export interface ForceLogoutResult {
  outcome: ForceLogoutOutcome
  currentSession: boolean
}

export const getOnlineSessionListApi = (params: OnlineSessionQuery) =>
  request<PageResult<OnlineSessionRecord>>({ url: '/monitor/online/list', method: 'get', params })

export const forceLogoutOnlineSessionApi = (sessionId: string) =>
  request<ForceLogoutResult>({ url: `/monitor/online/${encodeURIComponent(sessionId)}`, method: 'delete' })
