import { ApiOutlined, DeleteOutlined, DesktopOutlined, EditOutlined, KeyOutlined, LockOutlined, MoreOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, UndoOutlined, UnlockOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import AntApp from 'antd/es/app'
import Spin from 'antd/es/spin'
import Tag from 'antd/es/tag'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { addManagedServerApi, confirmServerFingerprintApi, deleteManagedServerApi, getManagedServersApi, resetServerFingerprintApi, testManagedServerApi, updateManagedServerApi, type ManagedServer, type ManagedServerQuery, type ServerConnectionTest } from '@/api/server'
import { ManagementCard, ManagementPagination } from '@/components/management/ManagementUi'
import { ManagedServerDialog } from '@/components/monitor/ManagedServerDialog'
import { SshTerminalDialog } from '@/components/monitor/SshTerminalDialog'
import { TemporaryPasswordDialog } from '@/components/monitor/TemporaryPasswordDialog'
import { Button } from '@/components/ui/button'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { managedServerFormToPayload, type ManagedServerFormValues } from '@/pages/monitor/monitorForms'
import { useAuthStore } from '@/store/authStore'

interface ServerFilterValues { name: string; enabled?: number }
type PasswordAction = 'test' | 'terminal'

const initialQuery: ManagedServerQuery = { pageNum: 1, pageSize: 12 }

function compactFingerprint(fingerprint: string) {
  if (fingerprint.length <= 38) return fingerprint
  return `${fingerprint.slice(0, 24)}...${fingerprint.slice(-12)}`
}

function serverState(server: ManagedServer) {
  if (server.enabled !== 1) return { label: '已停用', className: 'disabled' }
  if (server.lastError) return { label: '连接异常', className: 'error' }
  if (server.lastConnectTime) return { label: '连接正常', className: 'online' }
  return { label: '尚未测试', className: 'pending' }
}

export function ServerManagementPage() {
  const { message, modal } = AntApp.useApp()
  const queryClient = useQueryClient()
  const permissions = useAuthStore(state => state.user.permissions)
  const [queryParams, setQueryParams] = useState<ManagedServerQuery>(initialQuery)
  const [serverDialogOpen, setServerDialogOpen] = useState(false)
  const [editingServer, setEditingServer] = useState<ManagedServer>()
  const [passwordRequest, setPasswordRequest] = useState<{ server: ManagedServer; action: PasswordAction }>()
  const [terminal, setTerminal] = useState<{ server: ManagedServer; password?: string }>()
  const [testingId, setTestingId] = useState<number>()
  const filterForm = useForm<ServerFilterValues>({ defaultValues: { name: '', enabled: undefined } })
  const canAdd = permissions.includes('monitor:server:add')
  const canUpdate = permissions.includes('monitor:server:update')
  const canDelete = permissions.includes('monitor:server:delete')
  const canTest = permissions.includes('monitor:server:test')
  const canFingerprint = permissions.includes('monitor:server:fingerprint')
  const canTerminal = permissions.includes('monitor:server:terminal')

  const serversQuery = useQuery({ queryKey: ['managed-servers', queryParams], queryFn: async () => (await getManagedServersApi(queryParams)).data })
  const refreshServers = async () => { await queryClient.invalidateQueries({ queryKey: ['managed-servers'] }) }

  const saveMutation = useMutation({
    mutationFn: async (values: ManagedServerFormValues) => values.id === undefined ? addManagedServerApi(managedServerFormToPayload(values)) : updateManagedServerApi(managedServerFormToPayload(values)),
    onSuccess: async (_, values) => {
      setServerDialogOpen(false)
      await refreshServers()
      message.success(values.id === undefined ? '服务器已添加' : '服务器配置已更新')
    },
    onError: () => message.error('服务器配置保存失败')
  })

  const deleteMutation = useMutation({
    mutationFn: deleteManagedServerApi,
    onSuccess: async () => {
      const shouldMoveBack = (serversQuery.data?.records.length ?? 0) <= 1 && queryParams.pageNum > 1
      if (shouldMoveBack) setQueryParams(previous => ({ ...previous, pageNum: previous.pageNum - 1 }))
      else await refreshServers()
      message.success('服务器已删除')
    },
    onError: () => message.error('服务器删除失败')
  })

  const approveFingerprint = async (server: ManagedServer, result: ServerConnectionTest) => {
    if (!result.fingerprint) return false
    return new Promise<boolean>(resolve => {
      modal.confirm({
        title: '确认 SSH 主机指纹',
        width: 590,
        content: <div className="fingerprint-confirm-content"><p>服务器：{server.name}（{server.host}:{server.port}）</p><p>算法：{result.algorithm || '未知'}</p><code>{result.fingerprint}</code><small>请与服务器管理员提供的指纹核对一致后再确认。</small></div>,
        okText: '指纹一致，确认信任',
        cancelText: '取消',
        onCancel: () => resolve(false),
        onOk: async () => { await confirmServerFingerprintApi(server.id, result.fingerprint!); resolve(true) }
      })
    })
  }

  const showFingerprintMismatch = (server: ManagedServer, result: ServerConnectionTest) => {
    modal.error({ title: '主机指纹发生变化', width: 590, content: <div className="fingerprint-confirm-content"><p>服务器“{server.name}”返回的指纹与已信任值不同，连接已阻止。</p><span>已信任</span><code>{result.trustedFingerprint || '无'}</code><span>当前返回</span><code>{result.fingerprint || '未知'}</code><small>请先核实服务器是否更换过主机密钥，确认安全后再重置指纹并重新测试。</small></div>, okText: '知道了' })
  }

  const executeConnectionTest = async (server: ManagedServer, password?: string, allowConfirmation = true): Promise<boolean> => {
    const result = (await testManagedServerApi(server.id, password)).data
    if (result.status === 'SUCCESS') return true
    if (result.status === 'FINGERPRINT_MISMATCH') { showFingerprintMismatch(server, result); return false }
    if (result.status === 'CONFIRM_REQUIRED' && allowConfirmation) {
      if (!await approveFingerprint(server, result)) return false
      return executeConnectionTest(server, password, false)
    }
    return false
  }

  const runTest = async (server: ManagedServer, password?: string) => {
    setTestingId(server.id)
    try {
      if (await executeConnectionTest(server, password)) message.success(`${server.name} SSH 连接成功`)
      await refreshServers()
    } catch { message.error(`${server.name} SSH 连接失败`) } finally { setTestingId(undefined) }
  }

  const openTerminal = async (server: ManagedServer, password?: string) => {
    if (server.enabled !== 1) { message.warning('请先启用服务器'); return }
    if (!server.trustedFingerprint) {
      setTestingId(server.id)
      try {
        if (!await executeConnectionTest(server, password)) return
        await refreshServers()
      } catch { message.error(`${server.name} SSH 连接失败`); return } finally { setTestingId(undefined) }
    }
    setTerminal({ server, password })
  }

  const requestCredential = (server: ManagedServer, action: PasswordAction) => {
    if (!server.hasSavedPassword) { setPasswordRequest({ server, action }); return }
    if (action === 'test') void runTest(server)
    else void openTerminal(server)
  }

  const confirmResetFingerprint = (server: ManagedServer) => modal.confirm({ title: `重置“${server.name}”的主机指纹？`, content: '重置后必须重新测试并确认指纹才能连接。', okText: '确认重置', cancelText: '取消', onOk: async () => { await resetServerFingerprintApi(server.id); await refreshServers(); message.success('主机指纹已重置') } })
  const confirmDelete = (server: ManagedServer) => modal.confirm({ title: `删除服务器“${server.name}”？`, content: '正在使用的终端会立即断开，此操作不可撤销。', okText: '删除', okButtonProps: { danger: true }, cancelText: '取消', onOk: () => deleteMutation.mutateAsync(server.id) })

  const applyFilters = (values: ServerFilterValues) => setQueryParams(previous => ({ ...previous, pageNum: 1, name: values.name.trim() || undefined, enabled: values.enabled }))
  const resetFilters = () => { filterForm.reset(); setQueryParams(previous => ({ pageNum: 1, pageSize: previous.pageSize })) }

  return (
    <section className="management-page monitor-server-page">
      <ManagementCard
        filters={<Form {...filterForm}><form className="management-filter-form" onSubmit={filterForm.handleSubmit(applyFilters)}><FormField control={filterForm.control} name="name" render={({ field }) => <FormItem className="management-filter-field"><FormLabel>服务器</FormLabel><FormControl><Input placeholder="搜索名称" {...field} /></FormControl><FormMessage /></FormItem>} /><FormField control={filterForm.control} name="enabled" render={({ field }) => <FormItem className="management-filter-field management-filter-field-select"><FormLabel>状态</FormLabel><Select value={field.value === undefined ? 'all' : String(field.value)} onValueChange={value => field.onChange(value === 'all' ? undefined : Number(value))}><FormControl><SelectTrigger><SelectValue placeholder="全部状态" /></SelectTrigger></FormControl><SelectContent><SelectItem value="all">全部状态</SelectItem><SelectItem value="1">已启用</SelectItem><SelectItem value="0">已停用</SelectItem></SelectContent></Select><FormMessage /></FormItem>} /><div className="management-filter-actions"><Button type="submit"><SearchOutlined />搜索</Button><Button type="button" variant="outline" onClick={resetFilters}><UndoOutlined />重置</Button></div></form></Form>}
        toolbar={<><div className="management-toolbar-selection">服务器列表</div><div className="management-actions"><Button type="button" variant="outline" loading={serversQuery.isFetching} onClick={() => void serversQuery.refetch()}><ReloadOutlined />刷新</Button>{canAdd ? <Button type="button" onClick={() => { setEditingServer(undefined); setServerDialogOpen(true) }}><PlusOutlined />添加服务器</Button> : null}</div></>}
        pagination={<ManagementPagination current={queryParams.pageNum} pageSize={queryParams.pageSize} pageSizeOptions={[6, 12, 24, 48]} total={serversQuery.data?.total ?? 0} onChange={(pageNum, pageSize) => setQueryParams(previous => ({ ...previous, pageNum: pageSize === previous.pageSize ? pageNum : 1, pageSize }))} />}
      >
        <Spin spinning={serversQuery.isLoading}>
          <div className="server-grid">
            {(serversQuery.data?.records ?? []).map(server => {
              const state = serverState(server)
              return <article key={server.id} className={`server-card ${state.className}`} aria-label={`${server.name}，${state.label}`}><header className="server-card-header"><span className="server-mark"><DesktopOutlined /></span><div className="server-title"><strong>{server.name}</strong><code>{server.username}@{server.host}:{server.port}</code></div><span className="server-state"><i />{state.label}</span></header><p className="server-description">{server.description || '未填写用途说明'}</p><dl className="server-meta"><div><dt>凭据</dt><dd>{server.hasSavedPassword ? <LockOutlined /> : <UnlockOutlined />}{server.hasSavedPassword ? '已加密保存' : '连接时输入'}</dd></div><div><dt>最后连接</dt><dd>{server.lastConnectTime || '尚未连接'}</dd></div></dl><section className={`fingerprint-strip ${server.trustedFingerprint ? 'trusted' : ''}`}><div className="fingerprint-label"><span><KeyOutlined />主机指纹</span><Tag color={server.trustedFingerprint ? 'green' : undefined}>{server.trustedFingerprint ? '已确认' : '待确认'}</Tag></div>{server.trustedFingerprint ? <><code title={server.trustedFingerprint}>{compactFingerprint(server.trustedFingerprint)}</code><small>{server.fingerprintAlgorithm || '未知算法'} · {server.fingerprintVerifiedTime || '确认时间未知'}</small></> : <p>首次测试连接后，请核对并确认服务器返回的指纹。</p>}</section>{server.lastError ? <div className="server-error" role="alert">{server.lastError}</div> : null}<footer className="server-actions">{canTerminal ? <Button type="button" disabled={server.enabled !== 1} loading={testingId === server.id} onClick={() => requestCredential(server, 'terminal')}><DesktopOutlined />SSH 终端</Button> : null}{canTest ? <Button type="button" variant="outline" disabled={server.enabled !== 1} loading={testingId === server.id} onClick={() => requestCredential(server, 'test')}><ApiOutlined />测试</Button> : null}{canUpdate || canDelete || (canFingerprint && server.trustedFingerprint) ? <Popover><PopoverTrigger asChild><Button type="button" variant="outline" size="icon" aria-label={`${server.name}更多操作`}><MoreOutlined /></Button></PopoverTrigger><PopoverContent align="end" className="server-action-menu">{canFingerprint && server.trustedFingerprint ? <button type="button" onClick={() => confirmResetFingerprint(server)}><ReloadOutlined />重置主机指纹</button> : null}{canUpdate ? <button type="button" onClick={() => { setEditingServer(server); setServerDialogOpen(true) }}><EditOutlined />编辑配置</button> : null}{canDelete ? <button type="button" className="is-danger" onClick={() => confirmDelete(server)}><DeleteOutlined />删除服务器</button> : null}</PopoverContent></Popover> : null}</footer></article>
            })}
            {!serversQuery.isLoading && !serversQuery.data?.records.length ? <div className="server-empty"><DesktopOutlined /><span>暂无符合条件的服务器</span>{canAdd ? <Button type="button" onClick={() => setServerDialogOpen(true)}><PlusOutlined />添加第一台服务器</Button> : null}</div> : null}
          </div>
        </Spin>
      </ManagementCard>
      <ManagedServerDialog open={serverDialogOpen} server={editingServer} saving={saveMutation.isPending} onOpenChange={setServerDialogOpen} onSubmit={values => saveMutation.mutate(values)} />
      <TemporaryPasswordDialog server={passwordRequest?.server} action={passwordRequest?.action === 'terminal' ? '打开终端' : '连接测试'} onCancel={() => setPasswordRequest(undefined)} onSubmit={password => { const request = passwordRequest; setPasswordRequest(undefined); if (!request) return; if (request.action === 'test') void runTest(request.server, password); else void openTerminal(request.server, password) }} />
      <SshTerminalDialog server={terminal?.server} password={terminal?.password} onClose={() => setTerminal(undefined)} />
    </section>
  )
}
