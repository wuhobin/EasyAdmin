import { BellOutlined } from '@ant-design/icons'
import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import AntApp from 'antd/es/app'
import Tag from 'antd/es/tag'
import { useEffect, useMemo, useState } from 'react'
import { getMyNoticeDetailApi, getMyNoticeListApi, getUnreadNoticeCountApi, issueNotificationWsTicketApi, markAllNoticeReadApi, markNoticeReadApi, type NoticeRecord } from '@/api/notice'
import { NoticeContent } from '@/components/notifications/NoticeContent'
import { Button } from '@/components/ui/button'
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { useAuthStore } from '@/store/authStore'
import { getToken } from '@/utils/token'

type NoticeTab = 'unread' | 'all'

interface NotificationEvent {
  event?: string
  noticeType?: number
  title?: string
  unreadCount?: number
}

export function buildNotificationWebSocketUrl(ticket: string) {
  const baseApi = String(import.meta.env.VITE_APP_BASE_API || '/api')
  const base = /^https?:\/\//i.test(baseApi) ? new URL(baseApi) : new URL(baseApi.startsWith('/') ? baseApi : `/${baseApi}`, window.location.origin)
  base.protocol = base.protocol === 'https:' ? 'wss:' : 'ws:'
  base.pathname = `${base.pathname.replace(/\/$/, '')}/ws/notification`
  base.search = new URLSearchParams({ ticket }).toString()
  return base.toString()
}

function noticeId(item: NoticeRecord) {
  return item.noticeId ?? item.id
}

function noticeSummary(item: NoticeRecord) {
  return item.contentFormat === 'text' ? (item.contentPreview || item.content || '').replace(/\s+/g, ' ').slice(0, 100) : 'HTML 内容，点击查看详情'
}

function formatRelativeTime(value?: string) {
  if (!value) return ''
  const timestamp = new Date(value).getTime()
  if (Number.isNaN(timestamp)) return value
  const seconds = Math.round((timestamp - Date.now()) / 1000)
  const formatter = new Intl.RelativeTimeFormat('zh-CN', { numeric: 'auto' })
  if (Math.abs(seconds) < 60) return formatter.format(seconds, 'second')
  const minutes = Math.round(seconds / 60)
  if (Math.abs(minutes) < 60) return formatter.format(minutes, 'minute')
  const hours = Math.round(minutes / 60)
  if (Math.abs(hours) < 24) return formatter.format(hours, 'hour')
  const days = Math.round(hours / 24)
  if (Math.abs(days) < 30) return formatter.format(days, 'day')
  return value
}

export function NotificationCenter() {
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const userId = useAuthStore(state => state.user.id)
  const [open, setOpen] = useState(false)
  const [activeTab, setActiveTab] = useState<NoticeTab>('unread')
  const [detail, setDetail] = useState<NoticeRecord>()

  const countQuery = useQuery({ queryKey: ['notice-unread-count', userId], queryFn: async () => (await getUnreadNoticeCountApi()).data.unreadCount ?? 0, enabled: userId !== null, refetchInterval: 60_000 })
  const noticesQuery = useInfiniteQuery({
    queryKey: ['my-notices', userId, activeTab],
    initialPageParam: 1,
    enabled: open,
    queryFn: async ({ pageParam }) => (await getMyNoticeListApi({ unreadOnly: activeTab === 'unread', pageNum: pageParam, pageSize: 20 })).data,
    getNextPageParam: page => page.current < page.pages ? page.current + 1 : undefined
  })
  const items = useMemo(() => noticesQuery.data?.pages.flatMap(page => page.records ?? []) ?? [], [noticesQuery.data])

  const detailMutation = useMutation({
    mutationFn: async (item: NoticeRecord) => {
      const id = noticeId(item)
      const response = await getMyNoticeDetailApi(id)
      return { detail: response.data, item }
    },
    onSuccess: async ({ detail: notice, item }) => {
      setDetail(notice)
      setOpen(false)
      if (item.isRead !== 1) {
        try {
          await markNoticeReadApi(noticeId(item))
        } catch {
          message.warning('通知已打开，但未读状态更新失败')
          return
        }
        await Promise.all([
          queryClient.invalidateQueries({ queryKey: ['notice-unread-count'] }),
          queryClient.invalidateQueries({ queryKey: ['my-notices'] })
        ])
      }
    },
    onError: () => message.error('通知详情加载失败')
  })
  const markAllMutation = useMutation({
    mutationFn: markAllNoticeReadApi,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['notice-unread-count'] }),
        queryClient.invalidateQueries({ queryKey: ['my-notices'] })
      ])
      message.success('全部通知已标记为已读')
    },
    onError: () => message.error('全部已读操作失败')
  })

  useEffect(() => {
    let disposed = false
    let socket: WebSocket | undefined
    let pingTimer: number | undefined
    let reconnectTimer: number | undefined
    let reconnectAttempt = 0
    let lastPongAt = 0

    const delays = [1000, 2000, 5000, 10_000, 30_000]
    const refresh = async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['notice-unread-count'] }),
        queryClient.invalidateQueries({ queryKey: ['my-notices'] })
      ])
    }
    const scheduleReconnect = () => {
      if (disposed || reconnectTimer || !getToken()) return
      reconnectTimer = window.setTimeout(() => {
        reconnectTimer = undefined
        reconnectAttempt = Math.min(reconnectAttempt + 1, delays.length - 1)
        void connect()
      }, delays[reconnectAttempt])
    }
    const connect = async () => {
      if (disposed || !getToken()) return
      try {
        const response = await issueNotificationWsTicketApi()
        if (disposed) return
        socket = new WebSocket(buildNotificationWebSocketUrl(response.data.ticket))
        socket.onmessage = event => {
          try {
            const payload = JSON.parse(event.data) as NotificationEvent
            if (payload.event === 'pong') lastPongAt = Date.now()
            if (payload.event === 'notice-published') {
              if (payload.unreadCount !== undefined) queryClient.setQueryData(['notice-unread-count', userId], payload.unreadCount)
              else void queryClient.invalidateQueries({ queryKey: ['notice-unread-count'] })
              void queryClient.invalidateQueries({ queryKey: ['my-notices'] })
              if (payload.noticeType === 1 && payload.title) message.info(payload.title)
            }
          } catch {
            // Ignore malformed push messages and keep the channel alive.
          }
        }
        socket.onopen = () => {
          reconnectAttempt = 0
          lastPongAt = Date.now()
          window.clearInterval(pingTimer)
          pingTimer = window.setInterval(() => {
            if (!socket || socket.readyState !== WebSocket.OPEN) return
            if (Date.now() - lastPongAt >= 90_000) { socket.close(4000, 'pong timeout'); return }
            socket.send(JSON.stringify({ type: 'ping' }))
          }, 30_000)
          void refresh()
        }
        socket.onclose = () => {
          window.clearInterval(pingTimer)
          pingTimer = undefined
          socket = undefined
          scheduleReconnect()
        }
        socket.onerror = () => socket?.close()
      } catch {
        scheduleReconnect()
      }
    }
    const handleReadChange = () => { void refresh() }
    window.addEventListener('nexora:notice-read-changed', handleReadChange)
    void connect()
    return () => {
      disposed = true
      window.removeEventListener('nexora:notice-read-changed', handleReadChange)
      window.clearInterval(pingTimer)
      window.clearTimeout(reconnectTimer)
      socket?.close()
    }
  }, [message, queryClient])

  const unreadCount = countQuery.data ?? 0

  return (
    <>
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger asChild>
          <button className="topbar-icon-button notification-trigger" type="button" aria-label="打开通知中心" aria-expanded={open}>
            <BellOutlined />
            {unreadCount ? <span className="notification-badge" aria-hidden="true">{unreadCount > 99 ? '99+' : unreadCount}</span> : null}
            <span className="sr-only" aria-live="polite">{unreadCount} 条未读消息</span>
          </button>
        </PopoverTrigger>
        <PopoverContent align="end" sideOffset={10} className="notification-popover">
          <div className="notification-popover-header"><strong>通知中心</strong><Button type="button" variant="link" size="sm" disabled={!unreadCount} loading={markAllMutation.isPending} onClick={() => markAllMutation.mutate()}>全部已读</Button></div>
          <div className="notification-tabs" role="tablist" aria-label="通知分类">
            <button type="button" role="tab" aria-selected={activeTab === 'unread'} className={activeTab === 'unread' ? 'is-active' : ''} onClick={() => setActiveTab('unread')}>未读消息{unreadCount ? ` (${unreadCount})` : ''}</button>
            <button type="button" role="tab" aria-selected={activeTab === 'all'} className={activeTab === 'all' ? 'is-active' : ''} onClick={() => setActiveTab('all')}>全部消息</button>
          </div>
          <div className="notification-list" onScroll={event => { const target = event.currentTarget; if (target.scrollTop + target.clientHeight >= target.scrollHeight - 48 && noticesQuery.hasNextPage && !noticesQuery.isFetchingNextPage) void noticesQuery.fetchNextPage() }}>
            {noticesQuery.isLoading ? <div className="notification-empty">正在加载消息</div> : items.length ? items.map(item => <button key={`${activeTab}-${noticeId(item)}`} type="button" className={`notification-item ${item.isRead !== 1 ? 'is-unread' : ''}`} onClick={() => detailMutation.mutate(item)}><span className={`notification-item-mark ${item.noticeType === 2 ? 'is-announcement' : ''}`}>{item.noticeType === 2 ? '公' : '通'}</span><span className="notification-item-copy"><strong>{item.title}</strong><small>{noticeSummary(item)}</small><time>{formatRelativeTime(item.publishTime)}</time></span></button>) : <div className="notification-empty">{activeTab === 'unread' ? '暂无未读消息' : '暂无消息'}</div>}
            {noticesQuery.isFetchingNextPage ? <div className="notification-loading-more">正在加载更多</div> : null}
          </div>
        </PopoverContent>
      </Popover>

      <Dialog open={Boolean(detail)} onOpenChange={next => { if (!next) setDetail(undefined) }}>
        <DialogContent className="max-w-[680px]">
          <DialogHeader><DialogTitle>通知详情</DialogTitle><DialogDescription>查看系统发送的通知或公告内容。</DialogDescription></DialogHeader>
          {detail ? <div className="notice-detail-body"><div className="notice-detail-meta"><Tag color={detail.noticeType === 2 ? 'gold' : 'blue'}>{detail.noticeType === 2 ? '公告' : '通知'}</Tag><span>{detail.publishTime || ''}</span></div><h2>{detail.title}</h2><NoticeContent notice={detail} /></div> : null}
          <DialogFooter><DialogClose asChild><Button type="button" variant="outline">关闭</Button></DialogClose></DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  )
}
