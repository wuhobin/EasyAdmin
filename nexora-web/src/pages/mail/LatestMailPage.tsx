import { DeleteOutlined, DownloadOutlined, EditOutlined, InboxOutlined, MailOutlined, MoreOutlined, PaperClipOutlined, PlusOutlined, ReloadOutlined, WifiOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import AntApp from 'antd/es/app'
import Empty from 'antd/es/empty'
import Skeleton from 'antd/es/skeleton'
import Spin from 'antd/es/spin'
import axios from 'axios'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { addMailAccountApi, deleteMailAccountApi, downloadMailAttachmentApi, getLatestMailsApi, getMailAccountsApi, getMailDetailApi, getMailProvidersApi, markMailReadApi, openMailApi, testMailAccountApi, updateMailAccountApi, type MailAccount, type MailAttachment, type MailMessageDetail, type MailMessageSummary } from '@/api/mail'
import { MailAccountDialog } from '@/components/mail/MailAccountDialog'
import { Button } from '@/components/ui/button'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { Switch } from '@/components/ui/switch'
import { mailAccountFormToPayload, providerClass, providerLabel, providerMark, type MailAccountFormValues } from '@/pages/mail/mailForms'
import { useAuthStore } from '@/store/authStore'

const MAIL_LIMITS = [20, 30, 50]
const REFRESH_INTERVAL = 30_000

interface MailboxSnapshot {
  items: MailMessageSummary[]
  nextCursor?: string
  hasMore: boolean
}

function mailboxCacheKey(accountId: number | undefined, limit: number) {
  return `${accountId ?? 'all'}:${limit}`
}

function messageKey(message: MailMessageSummary) {
  return `${message.accountId}:${message.uidValidity}:${message.uid}`
}

function senderMark(message: MailMessageSummary) {
  return (message.fromName || message.fromAddress || '@').slice(0, 1).toUpperCase()
}

const messageTimeFormatter = new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false })
const messageDateFormatter = new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit' })
const detailTimeFormatter = new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false })
const fileSizeFormatter = new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 1 })

function formatMessageTime(value?: string) {
  if (!value) return '-'
  const time = new Date(value)
  if (Number.isNaN(time.getTime())) return '-'
  return time.toDateString() === new Date().toDateString() ? messageTimeFormatter.format(time) : messageDateFormatter.format(time)
}

function formatDetailTime(value?: string) {
  if (!value) return '-'
  const time = new Date(value)
  return Number.isNaN(time.getTime()) ? '-' : detailTimeFormatter.format(time)
}

function formatFileSize(size: number) {
  if (!size) return '未知大小'
  const units = ['B', 'KB', 'MB', 'GB']
  const index = Math.min(Math.floor(Math.log(size) / Math.log(1024)), units.length - 1)
  return `${fileSizeFormatter.format(size / Math.pow(1024, index))} ${units[index]}`
}

export function LatestMailPage() {
  const { message, modal, notification } = AntApp.useApp()
  const queryClient = useQueryClient()
  const permissions = useAuthStore(state => state.user.permissions)
  const [selectedAccountId, setSelectedAccountId] = useState<number>()
  const [messages, setMessages] = useState<MailMessageSummary[]>([])
  const [selectedMessageKey, setSelectedMessageKey] = useState('')
  const [mailDetail, setMailDetail] = useState<MailMessageDetail>()
  const [mailLimit, setMailLimit] = useState(30)
  const [mailCursor, setMailCursor] = useState<string>()
  const [hasMore, setHasMore] = useState(false)
  const [mailLoading, setMailLoading] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [autoRefresh, setAutoRefresh] = useState(true)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingAccount, setEditingAccount] = useState<MailAccount>()
  const [testingId, setTestingId] = useState<number>()
  const listController = useRef<AbortController>()
  const detailController = useRef<AbortController>()
  const listRequestId = useRef(0)
  const detailRequestId = useRef(0)
  const messagesRef = useRef<MailMessageSummary[]>([])
  const mailboxCache = useRef(new Map<string, MailboxSnapshot>())
  const detailCache = useRef(new Map<string, MailMessageDetail>())
  const detailRequests = useRef(new Map<string, Promise<MailMessageDetail>>())
  const prefetchTimer = useRef<number>()
  const knownMessageKeys = useRef(new Set<string>())
  const initializedMessages = useRef(false)
  const selectedMessageKeyRef = useRef('')
  const canAdd = permissions.includes('mail:account:add')
  const canUpdate = permissions.includes('mail:account:update')
  const canDelete = permissions.includes('mail:account:delete')
  const canTest = permissions.includes('mail:account:test')
  const canDownload = permissions.includes('mail:inbox:download')

  const accountsQuery = useQuery({ queryKey: ['mail-accounts'], queryFn: async () => (await getMailAccountsApi()).data })
  const providersQuery = useQuery({ queryKey: ['mail-providers'], queryFn: async () => (await getMailProvidersApi()).data })
  const accounts = accountsQuery.data ?? []
  const providers = providersQuery.data ?? []
  const currentAccount = accounts.find(account => account.id === selectedAccountId)
  const selectedMessage = messages.find(item => messageKey(item) === selectedMessageKey)
  const detailProvider = selectedMessage?.provider || 'QQ'
  const mailDocument = useMemo(() => `<!doctype html><html><head><meta charset="utf-8"><meta name="color-scheme" content="light"><style>html{background:#fff;color:#222}body{margin:20px;font:14px/1.65 Arial,sans-serif;overflow-wrap:anywhere}img{max-width:100%;height:auto}table{max-width:100%}</style></head><body>${mailDetail?.bodyHtml || ''}</body></html>`, [mailDetail?.bodyHtml])

  const selectMessage = (key: string) => { selectedMessageKeyRef.current = key; setSelectedMessageKey(key) }
  const showMailbox = useCallback((snapshot?: MailboxSnapshot) => {
    selectMessage('')
    setMailDetail(undefined)
    const nextMessages = snapshot?.items ?? []
    messagesRef.current = nextMessages
    setMessages(nextMessages)
    setMailCursor(snapshot?.nextCursor)
    setHasMore(snapshot?.hasMore ?? false)
    knownMessageKeys.current.clear()
    nextMessages.forEach(item => knownMessageKeys.current.add(messageKey(item)))
    initializedMessages.current = Boolean(snapshot)
  }, [])

  const loadMessages = useCallback(async ({ accountId, limit, cursor, silent = false, append = false, refresh = false }: { accountId?: number; limit: number; cursor?: string; silent?: boolean; append?: boolean; refresh?: boolean }) => {
    if (append && listController.current) return
    if (silent && listController.current) return
    if (!append) listController.current?.abort()
    const controller = new AbortController()
    listController.current = controller
    const requestId = ++listRequestId.current
    if (append) setLoadingMore(true)
    else if (!silent) setMailLoading(true)
    try {
      const data = (await getLatestMailsApi(accountId, limit, append ? cursor : undefined, controller.signal, refresh)).data
      if (requestId !== listRequestId.current) return
      if (!append && initializedMessages.current) {
        const newMessages = data.items.filter(item => !knownMessageKeys.current.has(messageKey(item)))
        if (newMessages.length) notification.success({ message: `收到 ${newMessages.length} 封新邮件`, description: newMessages[0].subject, placement: 'bottomRight' })
      }
      if (!append) knownMessageKeys.current.clear()
      data.items.forEach(item => knownMessageKeys.current.add(messageKey(item)))
      initializedMessages.current = true
      const nextMessages = append
        ? [...messagesRef.current, ...data.items.filter(item => !messagesRef.current.some(existing => messageKey(existing) === messageKey(item)))]
        : data.items
      messagesRef.current = nextMessages
      setMessages(nextMessages)
      setMailCursor(data.nextCursor)
      setHasMore(data.hasMore)
      mailboxCache.current.set(mailboxCacheKey(accountId, limit), {
        items: nextMessages,
        nextCursor: data.nextCursor,
        hasMore: data.hasMore
      })
      if (accountId === undefined && !append) {
        const accountItems = new Map<number, MailMessageSummary[]>()
        data.items.forEach(item => accountItems.set(item.accountId, [...(accountItems.get(item.accountId) ?? []), item]))
        accountItems.forEach((items, itemAccountId) => {
          const key = mailboxCacheKey(itemAccountId, limit)
          if (!mailboxCache.current.has(key)) mailboxCache.current.set(key, { items, hasMore: false })
        })
      }
      if (!append && selectedMessageKeyRef.current && !data.items.some(item => messageKey(item) === selectedMessageKeyRef.current)) {
        selectMessage('')
        setMailDetail(undefined)
      }
    } catch (error) {
      if (!axios.isCancel(error) && !silent) message.error('邮件列表读取失败')
    } finally {
      if (listController.current === controller) {
        listController.current = undefined
        if (append) setLoadingMore(false)
        else if (!silent) setMailLoading(false)
      }
    }
  }, [message, notification])

  useEffect(() => {
    const pendingController = listController.current
    pendingController?.abort()
    if (listController.current === pendingController) listController.current = undefined
    const cached = mailboxCache.current.get(mailboxCacheKey(selectedAccountId, mailLimit))
    showMailbox(cached)
    void loadMessages({ accountId: selectedAccountId, limit: mailLimit, silent: Boolean(cached) })
    return () => listController.current?.abort()
  }, [loadMessages, mailLimit, selectedAccountId, showMailbox])

  useEffect(() => {
    if (!autoRefresh) return
    const timer = window.setInterval(() => {
      if (document.visibilityState === 'visible') void loadMessages({ accountId: selectedAccountId, limit: mailLimit, silent: true, refresh: true })
    }, REFRESH_INTERVAL)
    return () => window.clearInterval(timer)
  }, [autoRefresh, loadMessages, mailLimit, selectedAccountId])

  useEffect(() => () => {
    listController.current?.abort()
    detailController.current?.abort()
    if (prefetchTimer.current !== undefined) window.clearTimeout(prefetchTimer.current)
  }, [])

  const markMessageRead = useCallback((key: string) => {
    const updateItems = (items: MailMessageSummary[]) => items.map(item =>
      messageKey(item) === key && !item.read ? { ...item, read: true } : item)
    const nextMessages = updateItems(messagesRef.current)
    messagesRef.current = nextMessages
    setMessages(nextMessages)
    mailboxCache.current.forEach((snapshot, cacheKey) => {
      const nextItems = updateItems(snapshot.items)
      if (nextItems.some((item, index) => item !== snapshot.items[index])) {
        mailboxCache.current.set(cacheKey, { ...snapshot, items: nextItems })
      }
    })
  }, [])

  const prefetchMessage = useCallback((mail: MailMessageSummary) => {
    const key = messageKey(mail)
    if (detailCache.current.has(key) || detailRequests.current.has(key) || detailRequests.current.size > 0) return
    const request = getMailDetailApi(mail)
      .then(response => {
        detailCache.current.set(key, response.data)
        return response.data
      })
      .finally(() => detailRequests.current.delete(key))
    detailRequests.current.set(key, request)
    void request.catch(() => undefined)
  }, [])

  const schedulePrefetch = useCallback((mail: MailMessageSummary) => {
    if (prefetchTimer.current !== undefined) window.clearTimeout(prefetchTimer.current)
    prefetchTimer.current = window.setTimeout(() => {
      prefetchTimer.current = undefined
      prefetchMessage(mail)
    }, 200)
  }, [prefetchMessage])

  const cancelPrefetch = useCallback(() => {
    if (prefetchTimer.current === undefined) return
    window.clearTimeout(prefetchTimer.current)
    prefetchTimer.current = undefined
  }, [])

  const openMessage = async (mail: MailMessageSummary) => {
    cancelPrefetch()
    detailController.current?.abort()
    const controller = new AbortController()
    detailController.current = controller
    const requestId = ++detailRequestId.current
    const key = messageKey(mail)
    selectMessage(key)
    const cachedDetail = detailCache.current.get(key)
    const pendingDetail = detailRequests.current.get(key)
    if (cachedDetail) setMailDetail(cachedDetail)
    setDetailLoading(!cachedDetail)
    try {
      let detail = cachedDetail
      if (!detail) {
        detail = pendingDetail
          ? await pendingDetail
          : (await (mail.read ? getMailDetailApi(mail, controller.signal) : openMailApi(mail, controller.signal))).data
      }
      detailCache.current.set(key, detail)
      if (!mail.read && (cachedDetail || pendingDetail)) await markMailReadApi(mail, controller.signal)
      if (!mail.read) markMessageRead(key)
      if (requestId === detailRequestId.current && selectedMessageKeyRef.current === key) setMailDetail(detail)
    } catch (error) {
      if (!axios.isCancel(error)) message.error('邮件正文读取失败')
    } finally {
      if (detailController.current === controller) { detailController.current = undefined; setDetailLoading(false) }
    }
  }

  const refreshAccounts = async () => { await queryClient.invalidateQueries({ queryKey: ['mail-accounts'] }) }
  const saveMutation = useMutation({
    mutationFn: async (values: MailAccountFormValues) => values.id === undefined ? addMailAccountApi(mailAccountFormToPayload(values)) : updateMailAccountApi(mailAccountFormToPayload(values)),
    onSuccess: async (_, values) => { setDialogOpen(false); await refreshAccounts(); await loadMessages({ accountId: selectedAccountId, limit: mailLimit, refresh: true }); message.success(values.id === undefined ? '邮箱账户已添加' : '邮箱账户已更新') },
    onError: () => message.error('邮箱账户保存失败')
  })
  const testAccount = async (account: MailAccount) => {
    setTestingId(account.id)
    try { await testMailAccountApi(account.id); await refreshAccounts(); message.success(`${account.accountName} 连接成功`) } catch { message.error(`${account.accountName} 连接失败`) } finally { setTestingId(undefined) }
  }
  const confirmDelete = (account: MailAccount) => modal.confirm({
    title: `删除邮箱“${account.accountName}”？`,
    content: '删除后不会影响邮箱服务器中的邮件，此操作不可撤销。',
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: async () => { await deleteMailAccountApi(account.id); mailboxCache.current.clear(); detailCache.current.clear(); setSelectedAccountId(undefined); showMailbox(); await refreshAccounts(); message.success('邮箱账户已删除') }
  })
  const downloadAttachment = async (attachment: MailAttachment) => {
    if (!mailDetail) return
    try {
      const blob = await downloadMailAttachmentApi(mailDetail, attachment.partId)
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = attachment.fileName
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
    } catch { message.error('附件下载失败') }
  }

  return (
    <section className="management-page mail-inbox-page">
      <div className="mail-inbox-canvas">
        <header className="mail-inbox-toolbar">
          <div><span className={`mail-refresh-dot ${autoRefresh ? 'active' : ''}`} /><span>{autoRefresh ? '每 30 秒自动刷新' : '自动刷新已暂停'}</span><Switch checked={autoRefresh} aria-label="自动刷新" onCheckedChange={setAutoRefresh} /></div>
          <div className="management-actions"><Button type="button" variant="outline" loading={mailLoading} onClick={() => void loadMessages({ accountId: selectedAccountId, limit: mailLimit, refresh: true })}><ReloadOutlined />刷新</Button>{canAdd ? <Button type="button" onClick={() => { setEditingAccount(undefined); setDialogOpen(true) }}><PlusOutlined />添加邮箱</Button> : null}</div>
        </header>

        <div className="mail-workspace">
          <aside className="mail-account-panel">
            <div className="mail-panel-label">邮箱账户</div>
            <button className={`mail-account-item ${selectedAccountId === undefined ? 'active' : ''}`} type="button" onClick={() => setSelectedAccountId(undefined)}><span className="mail-provider-avatar all"><InboxOutlined /></span><span className="mail-account-copy"><span>全部收件箱</span><small>{accounts.filter(item => item.enabled === 1).length} 个启用账户</small></span><span className="mail-count">{messages.length}</span></button>
            <div className="mail-account-list">{accounts.map(account => <button key={account.id} className={`mail-account-item ${selectedAccountId === account.id ? 'active' : ''}`} type="button" onClick={() => setSelectedAccountId(account.id)}><span className={`mail-provider-avatar ${providerClass(account.provider)}`}>{providerMark(account.provider)}</span><span className="mail-account-copy"><span>{account.accountName}</span><small>{account.email}</small></span><i className={`mail-connection-dot ${account.lastError ? 'error' : ''} ${account.enabled !== 1 ? 'disabled' : ''}`} /></button>)}{!accountsQuery.isLoading && !accounts.length ? <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="还没有邮箱账户" /> : null}</div>
            {currentAccount && (canTest || canUpdate || canDelete) ? <Popover><PopoverTrigger asChild><Button type="button" variant="outline" className="mail-account-manage"><MoreOutlined />管理当前邮箱</Button></PopoverTrigger><PopoverContent align="start" className="mail-account-menu">{canTest ? <button type="button" disabled={testingId === currentAccount.id} onClick={() => void testAccount(currentAccount)}><WifiOutlined />{testingId === currentAccount.id ? '正在测试' : '测试连接'}</button> : null}{canUpdate ? <button type="button" onClick={() => { setEditingAccount(currentAccount); setDialogOpen(true) }}><EditOutlined />编辑账户</button> : null}{canDelete ? <button type="button" className="is-danger" onClick={() => confirmDelete(currentAccount)}><DeleteOutlined />删除账户</button> : null}</PopoverContent></Popover> : null}
          </aside>

          <section className="mail-message-panel">
            <div className="mail-message-toolbar"><div><span>{currentAccount?.accountName || '全部收件箱'}</span><small>{messages.length} 封最新邮件</small></div><div className="mail-limit-control" aria-label="邮件数量">{MAIL_LIMITS.map(limit => <button key={limit} type="button" className={mailLimit === limit ? 'active' : ''} onClick={() => setMailLimit(limit)}>{limit}</button>)}</div></div>
            <Spin spinning={mailLoading}><div className="mail-message-list">{messages.map(mail => <button key={messageKey(mail)} aria-label={`${mail.read ? '已读' : '未读'}邮件：${mail.subject || '无主题'}`} className={`mail-message-item ${selectedMessageKey === messageKey(mail) ? 'active' : ''} ${mail.read ? '' : 'unread'}`} type="button" onMouseEnter={() => schedulePrefetch(mail)} onMouseLeave={cancelPrefetch} onClick={() => void openMessage(mail)}>{mail.read ? null : <span className="mail-unread-dot" aria-hidden="true" />}<span className={`mail-sender-avatar ${providerClass(mail.provider)}`}>{senderMark(mail)}</span><span className="mail-message-copy"><span className="mail-message-meta"><span>{mail.fromName || mail.fromAddress}</span><time dateTime={mail.receivedTime}>{formatMessageTime(mail.receivedTime)}</time></span><span className="mail-subject"><span>{mail.subject || '（无主题）'}</span>{mail.hasAttachment ? <PaperClipOutlined /> : null}</span><small>{mail.accountName} · {mail.fromAddress}</small></span></button>)}{!mailLoading && !messages.length ? <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="没有读取到邮件" /> : null}{hasMore ? <div className="mail-load-more"><Button type="button" variant="outline" loading={loadingMore} onClick={() => void loadMessages({ accountId: selectedAccountId, limit: mailLimit, cursor: mailCursor, append: true })}>加载更多</Button></div> : null}</div></Spin>
          </section>

          <article className="mail-reader-panel">
            {detailLoading ? <div className="mail-reader-loading"><Skeleton active paragraph={{ rows: 8 }} /></div> : mailDetail ? <><header className="mail-reader-header"><div className="mail-reader-meta"><span>{providerLabel(providers, detailProvider)}</span><time dateTime={mailDetail.receivedTime}>{formatDetailTime(mailDetail.receivedTime)}</time></div><h2>{mailDetail.subject || '（无主题）'}</h2><div className="mail-sender-line"><span className={`mail-sender-avatar large ${providerClass(detailProvider)}`}>{(mailDetail.fromName || mailDetail.fromAddress || '@').slice(0, 1).toUpperCase()}</span><div><span>{mailDetail.fromName || mailDetail.fromAddress}</span><p>{mailDetail.fromAddress}</p><small>发送至 {mailDetail.recipients?.join(', ') || '-'}</small></div></div></header>{mailDetail.attachments?.length ? <div className="mail-attachment-strip">{mailDetail.attachments.map(attachment => <button key={attachment.partId} type="button" disabled={!canDownload} onClick={() => void downloadAttachment(attachment)}><PaperClipOutlined /><span><span>{attachment.fileName}</span><small>{formatFileSize(attachment.size)}</small></span><DownloadOutlined /></button>)}</div> : null}<div className="mail-reader-body">{mailDetail.bodyHtml ? <iframe className="mail-frame" sandbox="allow-popups allow-popups-to-escape-sandbox" referrerPolicy="no-referrer" title="邮件正文" srcDoc={mailDocument} /> : <pre>{mailDetail.bodyText || '（邮件没有可显示的正文）'}</pre>}</div></> : <div className="mail-reader-empty"><span><MailOutlined /></span><h2>选择一封邮件开始阅读</h2><p>正文和附件会在点击后直接从对应邮箱读取。</p></div>}
          </article>
        </div>
      </div>
      <MailAccountDialog open={dialogOpen} account={editingAccount} providers={providers} providersLoading={providersQuery.isLoading} saving={saveMutation.isPending} onOpenChange={setDialogOpen} onSubmit={values => saveMutation.mutate(values)} />
    </section>
  )
}
