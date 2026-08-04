import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

async function readSource(relativePath) {
  return readFile(new URL(`../${relativePath}`, import.meta.url), 'utf8')
}

const apiSource = await readSource('src/api/monitor/online.ts')
const pageSource = await readSource('src/views/monitor/online/index.vue')

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
