import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import AntApp from 'antd/es/app'
import Tag from 'antd/es/tag'
import {
  Bell,
  BriefcaseBusiness,
  CalendarDays,
  CheckCircle2,
  ChevronRight,
  CircleAlert,
  Clock3,
  Files,
  Mail,
  Menu,
  Server,
  ShieldCheck,
  Sparkles,
  TimerReset,
  Users
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getFileListApi } from '@/api/file'
import { getJobListApi } from '@/api/job'
import { getMailAccountsApi } from '@/api/mail'
import {
  getMyNoticeDetailApi,
  getMyNoticeListApi,
  getUnreadNoticeCountApi,
  markNoticeReadApi,
  type NoticeRecord
} from '@/api/notice'
import { getManagedServersApi } from '@/api/server'
import { getWorkbenchSummaryApi } from '@/api/workbench'
import { MenuIcon } from '@/components/MenuIcon'
import { NoticeContent } from '@/components/notifications/NoticeContent'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle
} from '@/components/ui/dialog'
import {
  formatRelativeTime,
  greetingForHour,
  noticePreview,
  selectWorkbenchRuntimePaths,
  selectWorkbenchShortcuts
} from '@/pages/workbench/workbenchUtils'
import { useAuthStore } from '@/store/authStore'
import { useRouteStore } from '@/store/routeStore'

interface StatCard {
  label: string
  value?: number
  icon: LucideIcon
  tone: 'violet' | 'blue' | 'green' | 'amber'
}

interface OverviewItem {
  key: string
  title: string
  path: string
  value: number | undefined
  description: string
  icon: LucideIcon
  loading: boolean
  error: boolean
}

const noticeId = (notice: NoticeRecord) => notice.noticeId ?? notice.id

async function getRecentNotices() {
  const [unreadResponse, allResponse] = await Promise.all([
    getMyNoticeListApi({ unreadOnly: true, pageNum: 1, pageSize: 5 }),
    getMyNoticeListApi({ unreadOnly: false, pageNum: 1, pageSize: 5 })
  ])
  const seen = new Set<number>()
  return [...(unreadResponse.data.records ?? []), ...(allResponse.data.records ?? [])]
    .filter(item => {
      const id = noticeId(item)
      if (seen.has(id)) return false
      seen.add(id)
      return true
    })
    .slice(0, 5)
}

function WorkbenchClock({ name, avatar }: { name: string; avatar: string | null }) {
  const [now, setNow] = useState(() => new Date())

  useEffect(() => {
    const timer = window.setInterval(() => setNow(new Date()), 1000)
    return () => window.clearInterval(timer)
  }, [])

  const time = now.toLocaleTimeString('zh-CN', { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' })
  const date = now.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
  const initial = name.trim().charAt(0).toUpperCase() || 'N'

  return (
    <section className="workbench-welcome" aria-labelledby="workbench-title">
      <div className="workbench-welcome-copy">
        <div className="workbench-avatar" aria-hidden="true">
          {avatar ? <img src={avatar} alt="" /> : <span>{initial}</span>}
        </div>
        <div>
          <h1 id="workbench-title">{greetingForHour(now.getHours())}，{name}</h1>
          <p>欢迎回来，这里是你的工作概览。</p>
        </div>
      </div>
      <div className="workbench-clock" aria-label={`${date} ${time}`}>
        <time>{time}</time>
        <span><CalendarDays aria-hidden="true" />{date}</span>
      </div>
    </section>
  )
}

function StatCards({ cards, loading, error }: { cards: StatCard[]; loading: boolean; error: boolean }) {
  return (
    <section className="workbench-stats" aria-label="关键统计" aria-busy={loading}>
      {cards.map(card => {
        const Icon = card.icon
        return (
          <article className={`workbench-stat-card tone-${card.tone}`} key={card.label}>
            <div className="workbench-stat-icon"><Icon aria-hidden="true" /></div>
            <div>
              <strong>{loading ? <span className="workbench-number-skeleton" /> : error ? '—' : (card.value ?? 0).toLocaleString('zh-CN')}</strong>
              <span>{card.label}</span>
            </div>
          </article>
        )
      })}
    </section>
  )
}

function SectionHeading({ title, description }: { title: string; description: string }) {
  return <header className="workbench-section-heading"><div><h2>{title}</h2><p>{description}</p></div></header>
}

export function WorkbenchPage() {
  const { message } = AntApp.useApp()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const user = useAuthStore(state => state.user)
  const routes = useRouteStore(state => state.routes)
  const [noticeDetail, setNoticeDetail] = useState<NoticeRecord>()
  const [emailReminderDismissed, setEmailReminderDismissed] = useState(false)
  const shortcuts = useMemo(() => selectWorkbenchShortcuts(routes), [routes])

  const summaryQuery = useQuery({
    queryKey: ['workbench-summary', user.id],
    queryFn: async () => (await getWorkbenchSummaryApi()).data,
    enabled: user.id !== null
  })
  const unreadQuery = useQuery({
    queryKey: ['notice-unread-count', user.id],
    queryFn: async () => (await getUnreadNoticeCountApi()).data.unreadCount ?? 0,
    enabled: user.id !== null,
    refetchInterval: 60_000
  })
  const recentNoticesQuery = useQuery({
    queryKey: ['my-notices', user.id, 'workbench'],
    queryFn: getRecentNotices,
    enabled: user.id !== null
  })

  const routePaths = useMemo(() => selectWorkbenchRuntimePaths(routes), [routes])

  const jobsQuery = useQuery({
    queryKey: ['workbench-runtime', user.id, 'jobs'],
    enabled: Boolean(routePaths.jobs),
    queryFn: async () => {
      const [all, enabled] = await Promise.all([
        getJobListApi({ pageNum: 1, pageSize: 1 }),
        getJobListApi({ pageNum: 1, pageSize: 1, status: '0' })
      ])
      return { total: all.data.total ?? 0, healthy: enabled.data.total ?? 0 }
    }
  })
  const serversQuery = useQuery({
    queryKey: ['workbench-runtime', user.id, 'servers'],
    enabled: Boolean(routePaths.servers),
    queryFn: async () => {
      const response = await getManagedServersApi({ pageNum: 1, pageSize: 100 })
      const records = response.data.records ?? []
      return { total: response.data.total ?? records.length, attention: records.filter(item => Boolean(item.lastError)).length }
    }
  })
  const filesQuery = useQuery({
    queryKey: ['workbench-runtime', user.id, 'files'],
    enabled: Boolean(routePaths.files),
    queryFn: async () => (await getFileListApi({ pageNum: 1, pageSize: 1 })).data.total ?? 0
  })
  const mailQuery = useQuery({
    queryKey: ['workbench-runtime', user.id, 'mail'],
    enabled: Boolean(routePaths.mail),
    queryFn: async () => {
      const accounts = (await getMailAccountsApi()).data ?? []
      return { total: accounts.length, attention: accounts.filter(item => Boolean(item.lastError)).length }
    }
  })

  const detailMutation = useMutation({
    mutationFn: async (item: NoticeRecord) => {
      const response = await getMyNoticeDetailApi(noticeId(item))
      return { item, detail: response.data }
    },
    onSuccess: async ({ item, detail }) => {
      setNoticeDetail(detail)
      if (item.isRead === 1) return
      try {
        await markNoticeReadApi(noticeId(item))
        await Promise.all([
          queryClient.invalidateQueries({ queryKey: ['notice-unread-count'] }),
          queryClient.invalidateQueries({ queryKey: ['my-notices'] })
        ])
        window.dispatchEvent(new CustomEvent('nexora:notice-read-changed'))
      } catch {
        message.warning('通知已打开，但未读状态更新失败')
      }
    },
    onError: () => message.error('通知详情加载失败')
  })

  const summary = summaryQuery.data
  const statCards: StatCard[] = summary?.administrator
    ? [
        { label: '用户总数', value: summary.userCount, icon: Users, tone: 'violet' },
        { label: '角色数量', value: summary.roleCount, icon: BriefcaseBusiness, tone: 'green' },
        { label: '菜单数量', value: summary.menuCount, icon: Menu, tone: 'blue' },
        { label: '权限数量', value: summary.permissionCount, icon: ShieldCheck, tone: 'amber' }
      ]
    : [
        { label: '可访问功能', value: summary?.accessibleFeatureCount, icon: Sparkles, tone: 'violet' },
        { label: '我的角色', value: summary?.roleCount, icon: BriefcaseBusiness, tone: 'green' },
        { label: '我的权限', value: summary?.permissionCount, icon: ShieldCheck, tone: 'blue' },
        { label: '未读通知', value: unreadQuery.data, icon: Bell, tone: 'amber' }
      ]

  const overview: OverviewItem[] = [
    routePaths.jobs ? {
      key: 'jobs', title: '定时任务', path: routePaths.jobs, value: jobsQuery.data?.total,
      description: jobsQuery.isError ? '状态暂时无法获取' : `${jobsQuery.data?.healthy ?? 0} 项正在运行`,
      icon: TimerReset, loading: jobsQuery.isLoading, error: jobsQuery.isError
    } : undefined,
    routePaths.servers ? {
      key: 'servers', title: '个人服务器', path: routePaths.servers, value: serversQuery.data?.total,
      description: serversQuery.isError ? '状态暂时无法获取' : serversQuery.data?.attention ? `${serversQuery.data.attention} 台需要关注` : '当前无连接异常',
      icon: Server, loading: serversQuery.isLoading, error: serversQuery.isError
    } : undefined,
    routePaths.files ? {
      key: 'files', title: '文件', path: routePaths.files, value: filesQuery.data,
      description: filesQuery.isError ? '状态暂时无法获取' : '可管理文件总数',
      icon: Files, loading: filesQuery.isLoading, error: filesQuery.isError
    } : undefined,
    routePaths.mail ? {
      key: 'mail', title: '邮箱账户', path: routePaths.mail, value: mailQuery.data?.total,
      description: mailQuery.isError ? '状态暂时无法获取' : mailQuery.data?.attention ? `${mailQuery.data.attention} 个连接异常` : '账户连接状态正常',
      icon: Mail, loading: mailQuery.isLoading, error: mailQuery.isError
    } : undefined
  ].filter((item): item is OverviewItem => Boolean(item))

  const displayName = user.nickname?.trim() || user.email.split('@')[0] || 'Nexora 用户'
  const notices = recentNoticesQuery.data ?? []

  return (
    <div className="workbench-page">
      <WorkbenchClock name={displayName} avatar={user.avatar} />
      {!user.email && !emailReminderDismissed ? <section className="workbench-email-reminder" role="status">
        <CircleAlert aria-hidden="true" />
        <div><strong>绑定邮箱，完善账号登录方式</strong><span>绑定并设置密码后，你仍可使用微信登录，也可以使用邮箱密码登录。</span></div>
        <Button type="button" onClick={() => navigate('/profile')}>去绑定</Button>
        <Button type="button" variant="ghost" onClick={() => setEmailReminderDismissed(true)}>稍后提醒</Button>
      </section> : null}
      <StatCards cards={statCards} loading={summaryQuery.isLoading || (!summary?.administrator && unreadQuery.isLoading)} error={summaryQuery.isError} />

      <div className="workbench-main-grid">
        <section className="workbench-panel workbench-shortcuts">
          <SectionHeading title="快捷入口" description="根据你的访问权限智能排列" />
          {shortcuts.length ? (
            <div className="workbench-shortcut-grid">
              {shortcuts.map(shortcut => (
                <button type="button" key={shortcut.path} onClick={() => navigate(shortcut.path)}>
                  <span className="workbench-shortcut-icon"><MenuIcon value={shortcut.icon} /></span>
                  <span>{shortcut.title}</span>
                  <ChevronRight aria-hidden="true" />
                </button>
              ))}
            </div>
          ) : <div className="workbench-empty"><Sparkles aria-hidden="true" /><span>暂无其他可访问功能</span></div>}
        </section>

        <section className="workbench-panel workbench-notices">
          <SectionHeading title="最近通知" description="未读消息优先显示" />
          <div className="workbench-notice-list" aria-busy={recentNoticesQuery.isLoading}>
            {recentNoticesQuery.isLoading ? Array.from({ length: 4 }, (_, index) => <div className="workbench-notice-skeleton" key={index} />)
              : recentNoticesQuery.isError ? <div className="workbench-empty"><CircleAlert aria-hidden="true" /><span>通知暂时无法加载</span><Button type="button" variant="link" size="sm" onClick={() => void recentNoticesQuery.refetch()}>重新加载</Button></div>
                : notices.length ? notices.map(notice => (
                  <button type="button" className={notice.isRead === 1 ? '' : 'is-unread'} key={noticeId(notice)} onClick={() => detailMutation.mutate(notice)}>
                    <span className="workbench-notice-mark" aria-hidden="true" />
                    <span className="workbench-notice-copy">
                      <span><strong>{notice.title}</strong><time>{formatRelativeTime(notice.publishTime)}</time></span>
                      <small>{noticePreview(notice.contentFormat, notice.contentPreview, notice.content)}</small>
                    </span>
                  </button>
                )) : <div className="workbench-empty"><CheckCircle2 aria-hidden="true" /><span>暂无通知，所有事项都已处理</span></div>}
          </div>
        </section>
      </div>

      <section className="workbench-panel workbench-runtime">
        <SectionHeading title="运行概览" description="只展示你有权访问的业务状态" />
        {overview.length ? <div className="workbench-runtime-grid">
          {overview.map(item => {
            const Icon = item.icon
            return (
              <button type="button" key={item.key} onClick={() => navigate(item.path)}>
                <span className="workbench-runtime-icon"><Icon aria-hidden="true" /></span>
                <span className="workbench-runtime-copy">
                  <span>{item.title}</span>
                  <strong>{item.loading ? <span className="workbench-number-skeleton small" /> : item.error ? '—' : (item.value ?? 0).toLocaleString('zh-CN')}</strong>
                  <small className={item.error ? 'is-error' : ''}>{item.description}</small>
                </span>
                <ChevronRight aria-hidden="true" />
              </button>
            )
          })}
        </div> : <div className="workbench-runtime-fallback"><Clock3 aria-hidden="true" /><div><strong>工作台已就绪</strong><span>你的个人状态与通知会持续在这里更新。</span></div></div>}
      </section>

      <Dialog open={Boolean(noticeDetail)} onOpenChange={open => { if (!open) setNoticeDetail(undefined) }}>
        <DialogContent className="max-w-[680px]">
          <DialogHeader><DialogTitle>通知详情</DialogTitle><DialogDescription>查看系统发送的通知或公告内容。</DialogDescription></DialogHeader>
          {noticeDetail ? <div className="notice-detail-body"><div className="notice-detail-meta"><Tag color={noticeDetail.noticeType === 2 ? 'gold' : 'blue'}>{noticeDetail.noticeType === 2 ? '公告' : '通知'}</Tag><span>{noticeDetail.publishTime || ''}</span></div><h2>{noticeDetail.title}</h2><NoticeContent notice={noticeDetail} /></div> : null}
          <DialogFooter><DialogClose asChild><Button type="button" variant="outline">关闭</Button></DialogClose></DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
