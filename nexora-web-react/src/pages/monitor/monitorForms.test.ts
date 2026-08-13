import { describe, expect, it } from 'vitest'
import { buildCronExpression, createCronBuilderState, parseCronExpression } from '@/pages/monitor/cronBuilder'
import { abbreviateSessionId, getForceLogoutConfirmation, resolveFallbackPage } from '@/pages/monitor/forceLogoutFlow'
import { jobFormToPayload, managedServerFormToPayload } from '@/pages/monitor/monitorForms'

describe('monitor form helpers', () => {
  it('normalizes server credentials without accidentally persisting an empty password', () => {
    expect(managedServerFormToPayload({ id: 7, name: ' 生产节点 ', host: ' 10.0.0.8 ', port: 22, username: ' root ', password: '', savePassword: true, clearSavedPassword: true, description: ' 应用服务器 ', enabled: 1, sort: 2 })).toEqual({
      id: 7, name: '生产节点', host: '10.0.0.8', port: 22, username: 'root', password: undefined, savePassword: false, clearSavedPassword: true, description: '应用服务器', enabled: 1, sort: 2
    })
  })

  it('trims scheduled job payloads', () => {
    expect(jobFormToPayload({ jobName: ' 邮件检查 ', jobGroup: 'SYSTEM', invokeTarget: ' mailTask.run() ', cronExpression: ' 0 0/5 * * * ? ', misfirePolicy: '1', concurrent: '1', status: '1' })).toMatchObject({ jobName: '邮件检查', invokeTarget: 'mailTask.run()', cronExpression: '0 0/5 * * * ?' })
  })

  it('builds and parses Quartz cron fields', () => {
    const state = createCronBuilderState()
    state.minute = { mode: 'cycle', start: 0, interval: 5, values: [] }
    expect(buildCronExpression(state)).toBe('0 0/5 * * * ? *')
    expect(buildCronExpression(parseCronExpression('0 30 2 * * ? *'))).toBe('0 30 2 * * ? *')
  })

  it('preserves current-session warnings and page fallback behavior', () => {
    const session = { sessionId: '1234567890abcdef', email: 'admin@example.com', currentSession: true }
    expect(abbreviateSessionId(session.sessionId)).toBe('12345678...cdef')
    expect(getForceLogoutConfirmation(session)).toContain('当前会话')
    expect(resolveFallbackPage({ pageNum: 3, pageSize: 10, total: 20, recordCount: 0 })).toBe(2)
  })
})
