import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const page = readFileSync(new URL('../src/views/monitor/server/index.vue', import.meta.url), 'utf8')
const terminal = readFileSync(
  new URL('../src/components/SshTerminalDialog/index.vue', import.meta.url),
  'utf8'
)
const api = readFileSync(new URL('../src/api/monitor/server.ts', import.meta.url), 'utf8')
const viteConfig = readFileSync(new URL('../vite.config.ts', import.meta.url), 'utf8')

test('server management uses an isolated card list and permission-gated operations', () => {
  assert.match(page, /class="server-grid"/)
  assert.match(page, /class="server-card"/)
  assert.match(page, /仅展示你添加的服务器/)
  assert.match(page, /monitor:server:terminal/)
  assert.match(page, /monitor:server:fingerprint/)
  assert.match(page, /主机指纹发生变化/)
})

test('terminal uses a single-use ticket and the base64 websocket protocol', () => {
  assert.match(api, /terminal-ticket/)
  assert.match(terminal, /new WebSocket\(buildWebSocketUrl\(data\.ticket\)\)/)
  assert.match(terminal, /message\.encoding === 'base64'/)
  assert.match(terminal, /columns: cols/)
  assert.match(terminal, /30 分钟无键盘输入将自动断开/)
})

test('vite proxies websocket upgrades without rewriting the browser origin', () => {
  assert.match(viteConfig, /'\/api\/ws'/)
  assert.match(viteConfig, /ws: true/)
  assert.match(viteConfig, /changeOrigin: false/)
})
