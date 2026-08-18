import { LogoutOutlined, ReloadOutlined, SearchOutlined, UndoOutlined } from '@ant-design/icons'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import AntApp from 'antd/es/app'
import Table from 'antd/es/table'
import Tag from 'antd/es/tag'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { forceLogoutOnlineSessionApi, getOnlineSessionListApi, type ForceLogoutOutcome, type OnlineSessionQuery, type OnlineSessionRecord } from '@/api/online'
import { ManagementCard, ManagementPagination, ManagementRowAction } from '@/components/management/ManagementUi'
import { Button } from '@/components/ui/button'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { abbreviateSessionId, runForceLogoutFlow, type OnlineSessionPageState } from '@/pages/monitor/forceLogoutFlow'
import { useAuthStore } from '@/store/authStore'
import { usePageTabsStore } from '@/store/pageTabsStore'
import { useRouteStore } from '@/store/routeStore'
import { formatDateTime } from '@/utils/format'
import { useUrlQueryState, type UrlQuerySchema } from '@/utils/urlQueryState'

interface OnlineFilterValues { keyword: string; ip: string }
const initialQuery: OnlineSessionQuery = { pageNum: 1, pageSize: 10 }
const querySchema: UrlQuerySchema<OnlineSessionQuery> = { pageNum: 'number', pageSize: 'number', keyword: 'string', ip: 'string' }

function displayValue(value?: string) {
  return value?.trim() || '-'
}

export function OnlineUsersPage() {
  const { message, modal } = AntApp.useApp()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const permissions = useAuthStore(state => state.user.permissions)
  const [queryParams, setQueryParams] = useUrlQueryState(initialQuery, querySchema)
  const [forcingSessionId, setForcingSessionId] = useState('')
  const filterForm = useForm<OnlineFilterValues>({ defaultValues: { keyword: '', ip: '' } })
  useEffect(() => filterForm.reset({ keyword: queryParams.keyword ?? '', ip: queryParams.ip ?? '' }), [filterForm, queryParams.ip, queryParams.keyword])
  const canForceLogout = permissions.includes('sys:online:forceLogout')
  const sessionsQuery = useQuery({ queryKey: ['online-sessions', queryParams], queryFn: async () => (await getOnlineSessionListApi(queryParams)).data })

  const notifyForceLogout = (outcome: ForceLogoutOutcome, currentSession: boolean) => {
    if (currentSession) message.success('当前会话已退出，请重新登录')
    else if (outcome === 'LOGGED_OUT') message.success('强退成功')
    else message.warning('该会话已离线，列表已刷新')
  }

  const refresh = async (pageNum?: number): Promise<OnlineSessionPageState> => {
    const nextQuery = { ...queryParams, pageNum: pageNum ?? queryParams.pageNum }
    const data = (await getOnlineSessionListApi(nextQuery)).data
    queryClient.setQueryData(['online-sessions', nextQuery], data)
    if (nextQuery.pageNum !== queryParams.pageNum) setQueryParams(nextQuery)
    return { pageNum: nextQuery.pageNum, pageSize: nextQuery.pageSize, total: data.total, recordCount: data.records.length }
  }

  const confirmForceLogout = (content: string) => new Promise<void>((resolve, reject) => {
    modal.confirm({ title: '强退确认', content, okText: '强退', okButtonProps: { danger: true }, cancelText: '取消', onOk: () => resolve(), onCancel: () => reject(new Error('cancelled')) })
  })

  const handleForceLogout = async (session: OnlineSessionRecord) => {
    if (forcingSessionId) return
    setForcingSessionId(session.sessionId)
    try {
      await runForceLogoutFlow(session, {
        confirm: confirmForceLogout,
        forceLogout: async sessionId => (await forceLogoutOnlineSessionApi(sessionId)).data,
        refresh,
        notify: notifyForceLogout,
        clearSession: () => {
          useAuthStore.getState().clearSession()
          useRouteStore.getState().clearRoutes()
          usePageTabsStore.getState().resetTabs()
          queryClient.clear()
        },
        redirectToLogin: async () => { navigate('/login', { replace: true }) }
      })
    } catch {
      message.error('强退会话失败')
    } finally {
      setForcingSessionId('')
    }
  }

  const columns: ColumnsType<OnlineSessionRecord> = [
    { title: '会话编号', dataIndex: 'sessionId', width: 170, align: 'center', render: (value, record) => <div className="online-session-cell"><code title={value}>{abbreviateSessionId(value)}</code>{record.currentSession ? <Tag>当前会话</Tag> : null}</div> },
    { title: '用户', key: 'user', width: 210, render: (_, record) => <div className="online-detail-cell"><span>{displayValue(record.nickname)}</span><small>{displayValue(record.email)}</small></div> },
    { title: 'IP / 地点', key: 'location', width: 200, render: (_, record) => <div className="online-detail-cell"><span>{displayValue(record.ip)}</span><small>{displayValue(record.location)}</small></div> },
    { title: '浏览器', dataIndex: 'browser', width: 140, align: 'center', render: value => displayValue(value) },
    { title: '操作系统', dataIndex: 'os', width: 160, align: 'center', ellipsis: true, render: value => displayValue(value) },
    { title: '登录时间', dataIndex: 'loginTime', width: 180, render: value => <time dateTime={value}>{formatDateTime(value)}</time> },
    { title: '最后访问时间', dataIndex: 'lastAccessTime', width: 180, render: value => <time dateTime={value}>{formatDateTime(value)}</time> },
    { title: '操作', key: 'action', width: 84, fixed: 'right', align: 'center', render: (_, record) => canForceLogout ? <div className="management-row-actions"><ManagementRowAction tone="delete" icon={<LogoutOutlined />} aria-label={`强退会话 ${abbreviateSessionId(record.sessionId)}`} loading={forcingSessionId === record.sessionId} disabled={Boolean(forcingSessionId) && forcingSessionId !== record.sessionId} onClick={() => void handleForceLogout(record)} /></div> : null }
  ]

  const applyFilters = (values: OnlineFilterValues) => setQueryParams(previous => ({ ...previous, pageNum: 1, keyword: values.keyword.trim() || undefined, ip: values.ip.trim() || undefined }))
  const resetFilters = () => { filterForm.reset(); setQueryParams(previous => ({ pageNum: 1, pageSize: previous.pageSize })) }

  return (
    <section className="management-page online-users-page">
      <ManagementCard
        filters={<Form {...filterForm}><form className="management-filter-form" onSubmit={filterForm.handleSubmit(applyFilters)}><FormField control={filterForm.control} name="keyword" render={({ field }) => <FormItem className="management-filter-field"><FormLabel>用户</FormLabel><FormControl><Input placeholder="请输入邮箱或昵称" {...field} /></FormControl><FormMessage /></FormItem>} /><FormField control={filterForm.control} name="ip" render={({ field }) => <FormItem className="management-filter-field"><FormLabel>IP</FormLabel><FormControl><Input placeholder="请输入 IP" {...field} /></FormControl><FormMessage /></FormItem>} /><div className="management-filter-actions"><Button type="submit"><SearchOutlined />搜索</Button><Button type="button" variant="outline" onClick={resetFilters}><UndoOutlined />重置</Button></div></form></Form>}
        toolbar={<><div className="management-toolbar-selection">共 {sessionsQuery.data?.total ?? 0} 个有效会话</div><div className="management-actions"><Button type="button" variant="outline" loading={sessionsQuery.isFetching} onClick={() => void sessionsQuery.refetch()}><ReloadOutlined />刷新</Button></div></>}
        pagination={<ManagementPagination current={queryParams.pageNum} pageSize={queryParams.pageSize} total={sessionsQuery.data?.total ?? 0} onChange={(pageNum, pageSize) => setQueryParams(previous => ({ ...previous, pageNum: pageSize === previous.pageSize ? pageNum : 1, pageSize }))} />}
      >
        <Table<OnlineSessionRecord> rowKey="sessionId" loading={sessionsQuery.isLoading} columns={columns} dataSource={sessionsQuery.data?.records ?? []} pagination={false} scroll={{ x: 1240 }} locale={{ emptyText: '暂无在线用户' }} />
      </ManagementCard>
    </section>
  )
}
