import request from '@/api/client'
import type { PageResult } from '@/types/api'

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

export interface ManagedServerPayload {
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

export type ServerConnectionTestStatus = 'SUCCESS' | 'CONFIRM_REQUIRED' | 'FINGERPRINT_MISMATCH'

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

export const getManagedServersApi = (params: ManagedServerQuery) =>
  request<PageResult<ManagedServer>>({ url: '/monitor/server/list', method: 'get', params })

export const addManagedServerApi = (data: ManagedServerPayload) =>
  request<ManagedServer>({ url: '/monitor/server', method: 'post', data })

export const updateManagedServerApi = (data: ManagedServerPayload) =>
  request<void>({ url: '/monitor/server', method: 'put', data })

export const deleteManagedServerApi = (id: number) =>
  request<void>({ url: `/monitor/server/${id}`, method: 'delete' })

export const testManagedServerApi = (id: number, password?: string) =>
  request<ServerConnectionTest>({ url: `/monitor/server/${id}/test`, method: 'post', data: password ? { password } : {} })

export const confirmServerFingerprintApi = (id: number, fingerprint: string) =>
  request<void>({ url: `/monitor/server/${id}/fingerprint`, method: 'post', data: { fingerprint } })

export const resetServerFingerprintApi = (id: number) =>
  request<void>({ url: `/monitor/server/${id}/fingerprint`, method: 'delete' })

export const issueTerminalTicketApi = (id: number, data: { password?: string; columns: number; rows: number }) =>
  request<TerminalTicket>({ url: `/monitor/server/${id}/terminal-ticket`, method: 'post', data })
