<template>
  <el-dialog
    v-model="visible"
    class="ssh-terminal-dialog"
    :fullscreen="fullscreen"
    width="min(1180px, 94vw)"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    destroy-on-close
    @opened="openTerminal"
    @closed="disposeTerminal"
  >
    <template #header>
      <div class="terminal-heading">
        <div>
          <strong>{{ server?.name || 'SSH 终端' }}</strong>
          <span>{{ server ? `${server.username}@${server.host}:${server.port}` : '' }}</span>
        </div>
        <div class="terminal-heading-actions">
          <span :class="['terminal-state', connectionState]"><i />{{ connectionLabel }}</span>
          <el-button text :icon="fullscreen ? ScaleToOriginal : FullScreen" @click="toggleFullscreen">
            {{ fullscreen ? '退出全屏' : '全屏' }}
          </el-button>
        </div>
      </div>
    </template>

    <div class="terminal-shell">
      <div class="terminal-notice">
        <span>一次性票据 · 最多 3 个终端</span>
        <span>30 分钟无键盘输入将自动断开</span>
      </div>
      <div ref="terminalElement" class="terminal-canvas" aria-label="SSH 交互终端" />
    </div>

    <template #footer>
      <el-button type="danger" plain @click="visible = false">断开并关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { FullScreen, ScaleToOriginal } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { FitAddon } from '@xterm/addon-fit'
import { Terminal } from '@xterm/xterm'
import '@xterm/xterm/css/xterm.css'
import { issueTerminalTicketApi, type ManagedServer } from '@/api/monitor/server'

type ConnectionState = 'connecting' | 'connected' | 'closed' | 'error'

const props = defineProps<{
  modelValue: boolean
  server?: ManagedServer
  password?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const terminalElement = ref<HTMLElement>()
const fullscreen = ref(false)
const connectionState = ref<ConnectionState>('connecting')
let terminal: Terminal | undefined
let fitAddon: FitAddon | undefined
let socket: WebSocket | undefined
let resizeObserver: ResizeObserver | undefined
let opening = false

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})
const connectionLabel = computed(() => ({
  connecting: '正在连接',
  connected: '已连接',
  closed: '已断开',
  error: '连接异常'
})[connectionState.value])

function buildWebSocketUrl(ticket: string) {
  const baseApi = String(import.meta.env.VITE_APP_BASE_API || '/api')
  const baseUrl = /^https?:\/\//i.test(baseApi)
    ? new URL(baseApi)
    : new URL(baseApi.startsWith('/') ? baseApi : `/${baseApi}`, window.location.origin)
  baseUrl.protocol = baseUrl.protocol === 'https:' ? 'wss:' : 'ws:'
  baseUrl.pathname = `${baseUrl.pathname.replace(/\/$/, '')}/ws/ssh`
  baseUrl.search = new URLSearchParams({ ticket }).toString()
  return baseUrl.toString()
}

function decodeBase64(data: string) {
  const binary = window.atob(data)
  const bytes = new Uint8Array(binary.length)
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index)
  }
  return bytes
}

async function openTerminal() {
  if (opening || !terminalElement.value || !props.server) return
  opening = true
  connectionState.value = 'connecting'
  terminal = new Terminal({
    cursorBlink: true,
    cursorStyle: 'bar',
    convertEol: false,
    fontFamily: '"JetBrains Mono", "Cascadia Code", Consolas, monospace',
    fontSize: 14,
    lineHeight: 1.22,
    scrollback: 5000,
    theme: {
      background: '#0b1117',
      foreground: '#d9e3ec',
      cursor: '#67e8c4',
      cursorAccent: '#0b1117',
      selectionBackground: '#28475f',
      black: '#17212b',
      red: '#ff6b6b',
      green: '#67e8a5',
      yellow: '#ffd166',
      blue: '#72a7ff',
      magenta: '#c792ea',
      cyan: '#63d7e6',
      white: '#edf5fb',
      brightBlack: '#556575'
    }
  })
  fitAddon = new FitAddon()
  terminal.loadAddon(fitAddon)
  terminal.open(terminalElement.value)
  fitAddon.fit()
  terminal.writeln('\x1b[38;2;103;232;196mNEXORA SSH\x1b[0m  正在建立安全连接…')

  terminal.onData((data) => {
    if (socket?.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({ type: 'data', data }))
    }
  })
  terminal.onResize(({ cols, rows }) => {
    if (socket?.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({ type: 'resize', columns: cols, rows }))
    }
  })
  resizeObserver = new ResizeObserver(() => fitTerminal())
  resizeObserver.observe(terminalElement.value)

  try {
    const { data } = await issueTerminalTicketApi(props.server.id, {
      password: props.password || undefined,
      columns: terminal.cols,
      rows: terminal.rows
    })
    if (!visible.value) return
    socket = new WebSocket(buildWebSocketUrl(data.ticket))
    socket.onmessage = handleSocketMessage
    socket.onerror = () => {
      connectionState.value = 'error'
      terminal?.writeln('\r\n\x1b[31mWebSocket 连接异常\x1b[0m')
    }
    socket.onclose = () => {
      if (connectionState.value !== 'error') connectionState.value = 'closed'
      terminal?.writeln('\r\n\x1b[33m连接已关闭\x1b[0m')
    }
  } catch {
    connectionState.value = 'error'
    terminal?.writeln('\r\n\x1b[31m无法创建 SSH 终端\x1b[0m')
    ElMessage.error('SSH 终端连接失败')
  } finally {
    opening = false
  }
}

function handleSocketMessage(event: MessageEvent<string>) {
  try {
    const message = JSON.parse(event.data) as {
      type: string
      data?: string
      encoding?: string
      message?: string
    }
    if (message.type === 'connected') {
      connectionState.value = 'connected'
      terminal?.writeln('\r\n\x1b[32m已连接\x1b[0m\r\n')
      terminal?.focus()
    } else if (message.type === 'data' && message.data) {
      terminal?.write(message.encoding === 'base64' ? decodeBase64(message.data) : message.data)
    } else if (message.type === 'error') {
      connectionState.value = 'error'
      terminal?.writeln(`\r\n\x1b[31m${message.message || '连接失败'}\x1b[0m`)
    } else if (message.type === 'disconnected') {
      connectionState.value = 'closed'
      terminal?.writeln(`\r\n\x1b[33m${message.message || '连接已断开'}\x1b[0m`)
    }
  } catch {
    connectionState.value = 'error'
    terminal?.writeln('\r\n\x1b[31m终端消息解析失败\x1b[0m')
  }
}

function fitTerminal() {
  nextTick(() => {
    window.setTimeout(() => {
      try {
        fitAddon?.fit()
      } catch {
        // The dialog can finish closing while a resize callback is queued.
      }
    }, 50)
  })
}

function toggleFullscreen() {
  fullscreen.value = !fullscreen.value
  fitTerminal()
}

function disposeTerminal() {
  opening = false
  resizeObserver?.disconnect()
  resizeObserver = undefined
  if (socket) {
    socket.onclose = null
    socket.close()
    socket = undefined
  }
  terminal?.dispose()
  terminal = undefined
  fitAddon = undefined
  fullscreen.value = false
  connectionState.value = 'connecting'
}

onBeforeUnmount(disposeTerminal)
</script>

<style scoped>
.terminal-heading {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding-right: 28px;
}
.terminal-heading > div:first-child {
  min-width: 0;
}
.terminal-heading strong,
.terminal-heading span {
  display: block;
}
.terminal-heading strong {
  color: var(--el-text-color-primary);
  font-size: 16px;
}
.terminal-heading > div:first-child span {
  overflow: hidden;
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-family: "JetBrains Mono", "Cascadia Code", Consolas, monospace;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.terminal-heading-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
}
.terminal-state {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.terminal-state i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--el-color-info);
}
.terminal-state.connected { color: var(--el-color-success); }
.terminal-state.connected i { background: var(--el-color-success); box-shadow: 0 0 0 4px color-mix(in srgb, var(--el-color-success) 15%, transparent); }
.terminal-state.error { color: var(--el-color-danger); }
.terminal-state.error i { background: var(--el-color-danger); }
.terminal-shell {
  overflow: hidden;
  border: 1px solid #263745;
  border-radius: 10px;
  background: #0b1117;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}
.terminal-notice {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 36px;
  padding: 0 14px;
  color: #8195a6;
  border-bottom: 1px solid #263745;
  background: #101923;
  font-size: 11px;
  letter-spacing: .02em;
}
.terminal-canvas {
  height: min(64vh, 650px);
  padding: 10px 8px;
}
:global(.ssh-terminal-dialog.is-fullscreen) .terminal-canvas {
  height: calc(100vh - 190px);
}
.terminal-canvas :deep(.xterm) {
  height: 100%;
}
@media (max-width: 640px) {
  .terminal-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }
  .terminal-notice span:last-child {
    display: none;
  }
}
</style>
