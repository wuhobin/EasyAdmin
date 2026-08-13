import { ExpandOutlined, ShrinkOutlined } from '@ant-design/icons'
import { FitAddon } from '@xterm/addon-fit'
import { Terminal } from '@xterm/xterm'
import { useEffect, useRef, useState } from 'react'
import type { ManagedServer } from '@/api/server'
import { issueTerminalTicketApi } from '@/api/server'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import '@xterm/xterm/css/xterm.css'

type ConnectionState = 'connecting' | 'connected' | 'closed' | 'error'

export function buildSshWebSocketUrl(ticket: string) {
  const baseApi = String(import.meta.env.VITE_APP_BASE_API || '/api')
  const base = /^https?:\/\//i.test(baseApi) ? new URL(baseApi) : new URL(baseApi.startsWith('/') ? baseApi : `/${baseApi}`, window.location.origin)
  base.protocol = base.protocol === 'https:' ? 'wss:' : 'ws:'
  base.pathname = `${base.pathname.replace(/\/$/, '')}/ws/ssh`
  base.search = new URLSearchParams({ ticket }).toString()
  return base.toString()
}

function decodeBase64(data: string) {
  const binary = window.atob(data)
  const bytes = new Uint8Array(binary.length)
  for (let index = 0; index < binary.length; index += 1) bytes[index] = binary.charCodeAt(index)
  return bytes
}

export function SshTerminalDialog({ server, password, onClose }: { server?: ManagedServer; password?: string; onClose: () => void }) {
  const elementRef = useRef<HTMLDivElement>(null)
  const [fullscreen, setFullscreen] = useState(false)
  const [state, setState] = useState<ConnectionState>('connecting')

  useEffect(() => {
    if (!server || !elementRef.current) return
    let disposed = false
    let socket: WebSocket | undefined
    const terminal = new Terminal({ cursorBlink: true, cursorStyle: 'bar', fontFamily: '"JetBrains Mono", "Cascadia Code", Consolas, monospace', fontSize: 14, lineHeight: 1.22, scrollback: 5000, theme: { background: '#0b1117', foreground: '#d9e3ec', cursor: '#67e8c4', cursorAccent: '#0b1117', selectionBackground: '#28475f', black: '#17212b', red: '#ff6b6b', green: '#67e8a5', yellow: '#ffd166', blue: '#72a7ff', magenta: '#c792ea', cyan: '#63d7e6', white: '#edf5fb', brightBlack: '#556575' } })
    const fitAddon = new FitAddon()
    terminal.loadAddon(fitAddon)
    terminal.open(elementRef.current)
    fitAddon.fit()
    terminal.writeln('\x1b[38;2;103;232;196mNEXORA SSH\x1b[0m  正在建立安全连接...')
    const resizeObserver = new ResizeObserver(() => window.setTimeout(() => { if (!disposed) { try { fitAddon.fit() } catch { /* Terminal may be closing. */ } } }, 40))
    resizeObserver.observe(elementRef.current)
    const dataSubscription = terminal.onData(data => { if (socket?.readyState === WebSocket.OPEN) socket.send(JSON.stringify({ type: 'data', data })) })
    const resizeSubscription = terminal.onResize(({ cols, rows }) => { if (socket?.readyState === WebSocket.OPEN) socket.send(JSON.stringify({ type: 'resize', columns: cols, rows })) })

    void (async () => {
      try {
        const response = await issueTerminalTicketApi(server.id, { password: password || undefined, columns: terminal.cols, rows: terminal.rows })
        if (disposed) return
        socket = new WebSocket(buildSshWebSocketUrl(response.data.ticket))
        socket.onmessage = event => {
          try {
            const message = JSON.parse(event.data) as { type: string; data?: string; encoding?: string; message?: string }
            if (message.type === 'connected') { setState('connected'); terminal.writeln('\r\n\x1b[32m已连接\x1b[0m\r\n'); terminal.focus() }
            else if (message.type === 'data' && message.data) terminal.write(message.encoding === 'base64' ? decodeBase64(message.data) : message.data)
            else if (message.type === 'error') { setState('error'); terminal.writeln(`\r\n\x1b[31m${message.message || '连接失败'}\x1b[0m`) }
            else if (message.type === 'disconnected') { setState('closed'); terminal.writeln(`\r\n\x1b[33m${message.message || '连接已断开'}\x1b[0m`) }
          } catch { setState('error'); terminal.writeln('\r\n\x1b[31m终端消息解析失败\x1b[0m') }
        }
        socket.onerror = () => { setState('error'); terminal.writeln('\r\n\x1b[31mWebSocket 连接异常\x1b[0m') }
        socket.onclose = () => { setState(current => current === 'error' ? current : 'closed'); terminal.writeln('\r\n\x1b[33m连接已关闭\x1b[0m') }
      } catch { setState('error'); terminal.writeln('\r\n\x1b[31m无法创建 SSH 终端\x1b[0m') }
    })()

    return () => {
      disposed = true
      resizeObserver.disconnect()
      dataSubscription.dispose()
      resizeSubscription.dispose()
      if (socket) { socket.onclose = null; socket.close() }
      terminal.dispose()
      setState('connecting')
    }
  }, [password, server])

  const stateLabel = { connecting: '正在连接', connected: '已连接', closed: '已断开', error: '连接异常' }[state]
  return <Dialog open={Boolean(server)} onOpenChange={open => { if (!open) onClose() }}><DialogContent className={fullscreen ? 'ssh-terminal-dialog is-fullscreen' : 'ssh-terminal-dialog max-w-[1180px]'} onEscapeKeyDown={event => event.preventDefault()} onPointerDownOutside={event => event.preventDefault()}><DialogHeader className="terminal-heading"><div><DialogTitle>{server?.name || 'SSH 终端'}</DialogTitle><DialogDescription>{server ? `${server.username}@${server.host}:${server.port}` : ''}</DialogDescription></div><div className="terminal-heading-actions"><span className={`terminal-state ${state}`}><i />{stateLabel}</span><Button type="button" variant="ghost" onClick={() => setFullscreen(value => !value)}>{fullscreen ? <ShrinkOutlined /> : <ExpandOutlined />}{fullscreen ? '退出全屏' : '全屏'}</Button></div></DialogHeader><div className="terminal-shell"><div className="terminal-notice"><span>一次性票据 · 最多 3 个终端</span><span>30 分钟无键盘输入将自动断开</span></div><div ref={elementRef} className="terminal-canvas" aria-label="SSH 交互终端" /></div><DialogFooter><Button type="button" variant="destructive" onClick={onClose}>断开并关闭</Button></DialogFooter></DialogContent></Dialog>
}
