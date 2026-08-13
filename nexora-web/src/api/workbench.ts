import request from '@/api/client'

export interface WorkbenchSummary {
  administrator: boolean
  userCount?: number
  roleCount: number
  menuCount?: number
  permissionCount: number
  accessibleFeatureCount?: number
}

export const getWorkbenchSummaryApi = () =>
  request<WorkbenchSummary>({ url: '/workbench/summary', method: 'get' })
