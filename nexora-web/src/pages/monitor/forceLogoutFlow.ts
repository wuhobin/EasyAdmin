import type { ForceLogoutOutcome, ForceLogoutResult, OnlineSessionRecord } from '@/api/online'

export interface OnlineSessionPageState {
  pageNum: number
  pageSize: number
  total: number
  recordCount: number
}

export interface ForceLogoutFlowDependencies {
  confirm: (message: string) => Promise<unknown>
  forceLogout: (sessionId: string) => Promise<ForceLogoutResult>
  refresh: (pageNum?: number) => Promise<OnlineSessionPageState>
  clearSession: () => void
  redirectToLogin: () => Promise<unknown>
  notify: (outcome: ForceLogoutOutcome, currentSession: boolean) => void
}

export type ForceLogoutFlowResult = 'cancelled' | 'refreshed' | 'redirected'

export function abbreviateSessionId(sessionId: string) {
  const value = sessionId.trim()
  if (!value) return '--'
  if (value.length <= 13) return value
  return `${value.slice(0, 8)}...${value.slice(-4)}`
}

export function getSessionUserLabel(session: OnlineSessionRecord) {
  return session.nickname?.trim() || session.email?.trim() || '--'
}

export function getForceLogoutConfirmation(session: OnlineSessionRecord) {
  const redirectNotice = session.currentSession ? ' 这是当前会话，操作成功后将返回登录页。' : ''
  return `确定强退用户“${getSessionUserLabel(session)}”的会话 ${abbreviateSessionId(session.sessionId)} 吗？${redirectNotice}`
}

export function resolveFallbackPage(state: OnlineSessionPageState) {
  if (state.recordCount > 0 || state.pageNum <= 1) return undefined
  const lastPage = Math.max(1, Math.ceil(state.total / state.pageSize))
  return Math.min(state.pageNum - 1, lastPage)
}

export async function runForceLogoutFlow(session: OnlineSessionRecord, dependencies: ForceLogoutFlowDependencies): Promise<ForceLogoutFlowResult> {
  try {
    await dependencies.confirm(getForceLogoutConfirmation(session))
  } catch {
    return 'cancelled'
  }
  const result = await dependencies.forceLogout(session.sessionId)
  dependencies.notify(result.outcome, result.currentSession)
  if (result.currentSession) {
    dependencies.clearSession()
    await dependencies.redirectToLogin()
    return 'redirected'
  }
  const state = await dependencies.refresh()
  const fallbackPage = resolveFallbackPage(state)
  if (fallbackPage !== undefined && fallbackPage !== state.pageNum) await dependencies.refresh(fallbackPage)
  return 'refreshed'
}
