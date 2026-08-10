import request from '@/utils/request'

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

export interface OnlineSessionPage {
  records: OnlineSessionRecord[]
  total: number
  current: number
  size: number
}

export type ForceLogoutOutcome = 'LOGGED_OUT' | 'ALREADY_OFFLINE'

export interface ForceLogoutResult {
  outcome: ForceLogoutOutcome
  currentSession: boolean
}

export function getOnlineSessionListApi(params: OnlineSessionQuery) {
  return request<OnlineSessionPage>({
    url: '/monitor/online/list',
    method: 'get',
    params
  })
}

export function forceLogoutOnlineSessionApi(sessionId: string) {
  return request<ForceLogoutResult>({
    url: `/monitor/online/${sessionId}`,
    method: 'delete'
  })
}
