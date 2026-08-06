import request from '@/utils/request'

export interface ManagedServer {
  id: number
  name: string
  host: string
  port: number
  username: string
  description?: string
  enabled: number
  sort: number
  hasSavedPassword: boolean
  trustedFingerprint?: string
  fingerprintAlgorithm?: string
  fingerprintVerifiedTime?: string
  lastConnectTime?: string
  lastError?: string
  createTime?: string
  updateTime?: string
}

export interface ManagedServerForm {
  id?: number
  name: string
  host: string
  port: number
  username: string
  password?: string
  savePassword: boolean
  clearSavedPassword: boolean
  description?: string
  enabled: number
  sort: number
}

export interface ManagedServerQuery {
  pageNum: number
  pageSize: number
  name?: string
  enabled?: number
}

export interface ManagedServerPage {
  records: ManagedServer[]
  total: number
  current: number
  size: number
  pages: number
}

export type ServerConnectionTestStatus =
  | 'SUCCESS'
  | 'CONFIRM_REQUIRED'
  | 'FINGERPRINT_MISMATCH'

export interface ServerConnectionTest {
  status: ServerConnectionTestStatus
  fingerprint?: string
  trustedFingerprint?: string
  algorithm?: string
}

export interface TerminalTicket {
  ticket: string
  expiresAt: string
}

export function getManagedServersApi(params: ManagedServerQuery) {
  return request<ManagedServerPage>({
    url: '/monitor/server/list',
    method: 'get',
    params
  })
}

export function addManagedServerApi(data: ManagedServerForm) {
  return request<ManagedServer>({
    url: '/monitor/server',
    method: 'post',
    data
  })
}

export function updateManagedServerApi(data: ManagedServerForm) {
  return request<void>({
    url: '/monitor/server',
    method: 'put',
    data
  })
}

export function deleteManagedServerApi(id: number) {
  return request<void>({
    url: `/monitor/server/${id}`,
    method: 'delete'
  })
}

export function testManagedServerApi(id: number, password?: string) {
  return request<ServerConnectionTest>({
    url: `/monitor/server/${id}/test`,
    method: 'post',
    data: password ? { password } : {}
  })
}

export function confirmServerFingerprintApi(id: number, fingerprint: string) {
  return request<void>({
    url: `/monitor/server/${id}/fingerprint`,
    method: 'post',
    data: { fingerprint }
  })
}

export function resetServerFingerprintApi(id: number) {
  return request<void>({
    url: `/monitor/server/${id}/fingerprint`,
    method: 'delete'
  })
}

export function issueTerminalTicketApi(
  id: number,
  data: { password?: string; columns: number; rows: number }
) {
  return request<TerminalTicket>({
    url: `/monitor/server/${id}/terminal-ticket`,
    method: 'post',
    data
  })
}
