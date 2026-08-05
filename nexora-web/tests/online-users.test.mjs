import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import { transform } from 'esbuild'

async function readSource(relativePath) {
  return readFile(new URL(`../${relativePath}`, import.meta.url), 'utf8')
}

const apiSource = await readSource('src/api/monitor/online.ts')
const pageSource = await readSource('src/views/monitor/online/index.vue')
const flowSource = await readSource('src/views/monitor/online/force-logout.ts')

async function importTypeScriptModule(relativePath) {
  const source = await readSource(relativePath)
  const result = await transform(source, { loader: 'ts', format: 'esm', target: 'es2022' })
  const encoded = Buffer.from(result.code).toString('base64')
  return import(`data:text/javascript;base64,${encoded}`)
}

test('online session API defines a typed paginated query contract', () => {
  assert.match(apiSource, /export interface OnlineSessionQuery/)
  assert.match(apiSource, /pageNum:\s*number/)
  assert.match(apiSource, /pageSize:\s*number/)
  assert.match(apiSource, /keyword\?:\s*string/)
  assert.match(apiSource, /ip\?:\s*string/)
  assert.match(apiSource, /export interface OnlineSessionRecord/)
  assert.match(apiSource, /currentSession:\s*boolean/)
  assert.match(apiSource, /request<OnlineSessionPage>/)
  assert.match(apiSource, /url:\s*['"]\/monitor\/online\/list['"]/)
  assert.match(apiSource, /method:\s*['"]get['"]/)
  assert.match(apiSource, /export interface ForceLogoutResult/)
  assert.match(apiSource, /outcome:\s*ForceLogoutOutcome/)
  assert.match(apiSource, /forceLogoutOnlineSessionApi\(sessionId:\s*string\)/)
  assert.match(apiSource, /url:\s*`\/monitor\/online\/\$\{sessionId\}`/)
  assert.match(apiSource, /method:\s*['"]delete['"]/)
})

test('online user page renders the required session fields and neutral current-session marker', () => {
  for (const label of ['会话编号', '用户', 'IP / 地点', '浏览器', '操作系统', '登录时间', '最后访问时间']) {
    assert.match(pageSource, new RegExp(`label=["']${label.replace('/', '\\/')}["']`))
  }
  assert.match(pageSource, /abbreviateSessionId\(row\.sessionId\)/)
  assert.match(pageSource, /v-if=["']row\.currentSession["']/)
  assert.match(pageSource, /type=["']info["']/)
  assert.match(pageSource, />\s*当前会话\s*</)
  assert.match(pageSource, /v-if=["']normalize\(row\.nickname\)["']/)
  assert.match(pageSource, /displayValue\(row\.email\)/)
  assert.match(pageSource, /const displayValue[\s\S]*?['"]--['"]/)
  assert.match(
    pageSource,
    /<el-table-column\s+label=["']操作系统["'][^>]*show-overflow-tooltip/
  )
  assert.match(
    pageSource,
    /\.online-session-page\s+:deep\(\.data-list-table td\.el-table__cell\)\s*\{[^}]*height:\s*72px/
  )
  assert.doesNotMatch(pageSource, /label=["']状态["']/)
})

test('online user page supports filters, reset, manual refresh, pagination, loading, and empty state', () => {
  assert.match(pageSource, /queryParams\.keyword/)
  assert.match(pageSource, /queryParams\.ip/)
  assert.match(pageSource, /@click=["']handleQuery["']/)
  assert.match(pageSource, /@click=["']resetQuery["']/)
  assert.match(pageSource, /@click=["']handleRefresh["']/)
  assert.match(pageSource, /v-loading=["']loading["']/)
  assert.match(pageSource, /empty-text=["']暂无在线用户["']/)
  assert.match(pageSource, /<el-pagination/)
  assert.match(pageSource, /const handleRefresh = \(\) => \{\s*getList\(\)\s*\}/)
  assert.doesNotMatch(pageSource, /setInterval|setTimeout|WebSocket|autoRefresh|自动刷新/)
})

test('programmatic empty-page fallback does not duplicate the pagination refresh', () => {
  assert.match(pageSource, /@current-change=["']handleCurrentChange["']/)
  assert.match(pageSource, /let suppressPaginationChange = false/)
  assert.match(pageSource, /forcingSessionId\.value = session\.sessionId\s*suppressPaginationChange = true/)
  assert.match(pageSource, /finally \{\s*await nextTick\(\)\s*suppressPaginationChange = false/)
})

test('force logout confirmation shows the server row identity and warns for the current session', async () => {
  const { getForceLogoutConfirmation } = await importTypeScriptModule(
    'src/views/monitor/online/force-logout.ts'
  )
  const message = getForceLogoutConfirmation({
    sessionId: '550e8400-e29b-41d4-a716-446655440000',
    email: 'admin@example.com',
    nickname: 'Admin',
    currentSession: true
  })

  assert.match(message, /Admin/)
  assert.match(message, /550e8400…0000/)
  assert.match(message, /当前会话/)
  assert.match(message, /返回登录页/)
})

test('cancelling confirmation does not call the force logout API', async () => {
  const { runForceLogoutFlow } = await importTypeScriptModule(
    'src/views/monitor/online/force-logout.ts'
  )
  let apiCalls = 0
  const result = await runForceLogoutFlow(
    { sessionId: 'session', email: 'admin@example.com', currentSession: false },
    {
      confirm: async () => { throw new Error('cancelled') },
      forceLogout: async () => { apiCalls += 1 },
      refresh: async () => ({ pageNum: 1, pageSize: 10, total: 0, recordCount: 0 }),
      clearSession: () => {},
      redirectToLogin: async () => {},
      notify: () => {}
    }
  )

  assert.equal(result, 'cancelled')
  assert.equal(apiCalls, 0)
})

test('forcing another session refreshes once for both force logout outcomes', async () => {
  const { runForceLogoutFlow } = await importTypeScriptModule(
    'src/views/monitor/online/force-logout.ts'
  )
  for (const outcome of ['LOGGED_OUT', 'ALREADY_OFFLINE']) {
    let refreshCalls = 0
    const notifications = []
    const result = await runForceLogoutFlow(
      { sessionId: 'session', email: 'user@example.com', currentSession: false },
      {
        confirm: async () => {},
        forceLogout: async () => ({ outcome, currentSession: false }),
        refresh: async () => {
          refreshCalls += 1
          return { pageNum: 1, pageSize: 10, total: 1, recordCount: 1 }
        },
        clearSession: () => {},
        redirectToLogin: async () => {},
        notify: (...args) => notifications.push(args)
      }
    )

    assert.equal(result, 'refreshed')
    assert.equal(refreshCalls, 1)
    assert.deepEqual(notifications, [[outcome, false]])
  }
})

test('forcing the current session clears local state and redirects without refreshing', async () => {
  const { runForceLogoutFlow } = await importTypeScriptModule(
    'src/views/monitor/online/force-logout.ts'
  )
  let clearCalls = 0
  let redirectCalls = 0
  let refreshCalls = 0
  const result = await runForceLogoutFlow(
    { sessionId: 'session', email: 'admin@example.com', currentSession: true },
    {
      confirm: async () => {},
      forceLogout: async () => ({ outcome: 'LOGGED_OUT', currentSession: true }),
      refresh: async () => {
        refreshCalls += 1
        return { pageNum: 1, pageSize: 10, total: 0, recordCount: 0 }
      },
      clearSession: () => { clearCalls += 1 },
      redirectToLogin: async () => { redirectCalls += 1 },
      notify: () => {}
    }
  )

  assert.equal(result, 'redirected')
  assert.equal(clearCalls, 1)
  assert.equal(redirectCalls, 1)
  assert.equal(refreshCalls, 0)
  assert.doesNotMatch(pageSource, /userStore\.logout\(/)
  assert.match(pageSource, /userStore\.forceLogout\(\)/)
  assert.match(pageSource, /router\.replace\(['"]\/login['"]\)/)
})

test('an empty later page reloads the new last page', async () => {
  const { runForceLogoutFlow, resolveFallbackPage } = await importTypeScriptModule(
    'src/views/monitor/online/force-logout.ts'
  )
  assert.equal(resolveFallbackPage({ pageNum: 3, pageSize: 10, total: 20, recordCount: 0 }), 2)
  assert.equal(resolveFallbackPage({ pageNum: 1, pageSize: 10, total: 0, recordCount: 0 }), undefined)

  const refreshedPages = []
  await runForceLogoutFlow(
    { sessionId: 'session', email: 'user@example.com', currentSession: false },
    {
      confirm: async () => {},
      forceLogout: async () => ({ outcome: 'LOGGED_OUT', currentSession: false }),
      refresh: async (pageNum) => {
        refreshedPages.push(pageNum)
        return pageNum === undefined
          ? { pageNum: 2, pageSize: 10, total: 10, recordCount: 0 }
          : { pageNum, pageSize: 10, total: 10, recordCount: 10 }
      },
      clearSession: () => {},
      redirectToLogin: async () => {},
      notify: () => {}
    }
  )

  assert.deepEqual(refreshedPages, [undefined, 1])
})

test('force logout action is permission controlled in the table', () => {
  assert.match(pageSource, /v-permission=["']\['sys:online:forceLogout'\]["']/)
  assert.match(pageSource, /@click=["']handleForceLogout\(row\)["']/)
  assert.match(pageSource, />\s*强退\s*</)
  assert.doesNotMatch(flowSource, /logoutApi|userStore\.logout/)
})
