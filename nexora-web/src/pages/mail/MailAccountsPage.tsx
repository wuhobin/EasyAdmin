import { ApiOutlined, DeleteOutlined, EditOutlined, EyeOutlined, InboxOutlined, LockOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, UndoOutlined, WarningOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import AntApp from 'antd/es/app'
import Table, { type ColumnsType } from 'antd/es/table'
import Tag from 'antd/es/tag'
import { useEffect, useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { addMailAccountApi, deleteMailAccountApi, getMailAccountsApi, getMailProvidersApi, testMailAccountApi, updateMailAccountApi, type MailAccount, type MailProvider } from '@/api/mail'
import { MailAccountDialog } from '@/components/mail/MailAccountDialog'
import { ManagementCard, ManagementRowAction } from '@/components/management/ManagementUi'
import { Button } from '@/components/ui/button'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Sheet, SheetBody, SheetContent, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { mailAccountFormToPayload, providerClass, providerLabel, providerMark, type MailAccountFormValues } from '@/pages/mail/mailForms'
import { useAuthStore } from '@/store/authStore'
import { formatDateTime } from '@/utils/format'
import { useUrlQueryState, type UrlQuerySchema } from '@/utils/urlQueryState'

interface AccountFilterValues {
  keyword: string
  provider: MailProvider | 'all'
  status: 'all' | '0' | '1'
}

const initialFilters: AccountFilterValues = { keyword: '', provider: 'all', status: 'all' }
const filterSchema: UrlQuerySchema<AccountFilterValues> = { keyword: 'string', provider: 'string', status: 'string' }

function connectionState(account: MailAccount) {
  if (account.lastError) return <span className="mail-connection-state error"><WarningOutlined />连接异常</span>
  if (account.lastConnectTime) return <span className="mail-connection-state success"><i />连接正常</span>
  return <span className="mail-connection-state pending"><i />尚未测试</span>
}

export function MailAccountsPage() {
  const { message, modal } = AntApp.useApp()
  const queryClient = useQueryClient()
  const permissions = useAuthStore(state => state.user.permissions)
  const [filters, setFilters] = useUrlQueryState(initialFilters, filterSchema)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingAccount, setEditingAccount] = useState<MailAccount>()
  const [detailAccount, setDetailAccount] = useState<MailAccount>()
  const [testingId, setTestingId] = useState<number>()
  const filterForm = useForm<AccountFilterValues>({ defaultValues: filters })
  useEffect(() => filterForm.reset(filters), [filterForm, filters])
  const canAdd = permissions.includes('mail:account:add')
  const canUpdate = permissions.includes('mail:account:update')
  const canDelete = permissions.includes('mail:account:delete')
  const canTest = permissions.includes('mail:account:test')

  const accountsQuery = useQuery({ queryKey: ['mail-accounts'], queryFn: async () => (await getMailAccountsApi()).data })
  const providersQuery = useQuery({ queryKey: ['mail-providers'], queryFn: async () => (await getMailProvidersApi()).data })
  const accounts = accountsQuery.data ?? []
  const providers = providersQuery.data ?? []
  const filteredAccounts = useMemo(() => {
    const keyword = filters.keyword.trim().toLowerCase()
    return accounts.filter(account => {
      const matchesKeyword = !keyword || account.accountName.toLowerCase().includes(keyword) || account.email.toLowerCase().includes(keyword)
      const matchesProvider = filters.provider === 'all' || account.provider === filters.provider
      const matchesStatus = filters.status === 'all' || account.enabled === Number(filters.status)
      return matchesKeyword && matchesProvider && matchesStatus
    })
  }, [accounts, filters])

  const refreshAccounts = async () => { await queryClient.invalidateQueries({ queryKey: ['mail-accounts'] }) }
  const saveMutation = useMutation({
    mutationFn: async (values: MailAccountFormValues) => values.id === undefined ? addMailAccountApi(mailAccountFormToPayload(values)) : updateMailAccountApi(mailAccountFormToPayload(values)),
    onSuccess: async (_, values) => {
      setDialogOpen(false)
      await refreshAccounts()
      message.success(values.id === undefined ? '邮箱账户已添加' : '邮箱账户已更新')
    },
    onError: () => message.error('邮箱账户保存失败')
  })
  const testMutation = useMutation({
    mutationFn: async (account: MailAccount) => { setTestingId(account.id); return testMailAccountApi(account.id) },
    onSuccess: async (_, account) => { await refreshAccounts(); message.success(`${account.accountName} 连接成功`) },
    onError: (_, account) => message.error(`${account.accountName} 连接失败`),
    onSettled: () => setTestingId(undefined)
  })
  const deleteMutation = useMutation({
    mutationFn: deleteMailAccountApi,
    onSuccess: async () => { setDetailAccount(undefined); await refreshAccounts(); message.success('邮箱账户已删除') },
    onError: () => message.error('邮箱账户删除失败')
  })

  const openCreate = () => { setEditingAccount(undefined); setDialogOpen(true) }
  const openEdit = (account: MailAccount) => { setEditingAccount(account); setDialogOpen(true) }
  const confirmDelete = (account: MailAccount) => modal.confirm({
    title: `删除邮箱“${account.accountName}”？`,
    content: '删除后不会影响邮箱服务器中的邮件，此操作不可撤销。',
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: () => deleteMutation.mutateAsync(account.id)
  })
  const applyFilters = (values: AccountFilterValues) => setFilters({ ...values, keyword: values.keyword.trim() })
  const resetFilters = () => { const values: AccountFilterValues = { keyword: '', provider: 'all', status: 'all' }; filterForm.reset(values); setFilters(values) }

  const columns: ColumnsType<MailAccount> = [
    { title: '邮箱账户', key: 'account', width: 240, render: (_, account) => <div className="mail-account-cell"><span className={`mail-provider-avatar ${providerClass(account.provider)}`}>{providerMark(account.provider)}</span><span><span>{account.accountName}</span><small>{account.email}</small></span></div> },
    { title: '邮箱类型', dataIndex: 'provider', width: 130, align: 'center', render: provider => <Tag>{providerLabel(providers, provider)}</Tag> },
    { title: '状态', dataIndex: 'enabled', width: 100, align: 'center', render: enabled => enabled === 1 ? <Tag color="green">已启用</Tag> : <Tag>已停用</Tag> },
    { title: '连接状态', key: 'connection', width: 132, align: 'center', render: (_, account) => connectionState(account) },
    { title: '最后连接', dataIndex: 'lastConnectTime', width: 176, align: 'center', render: value => formatDateTime(value) },
    { title: '排序', dataIndex: 'sort', width: 80, align: 'center' },
    { title: '操作', key: 'actions', width: 192, fixed: 'right', align: 'center', render: (_, account) => <div className="management-row-actions"><ManagementRowAction tone="data" icon={<EyeOutlined />} aria-label={`查看${account.accountName}`} onClick={() => setDetailAccount(account)} />{canTest ? <ManagementRowAction tone="approve" icon={<ApiOutlined />} aria-label={`测试${account.accountName}`} loading={testingId === account.id} onClick={() => testMutation.mutate(account)} /> : null}{canUpdate ? <ManagementRowAction tone="edit" icon={<EditOutlined />} aria-label={`编辑${account.accountName}`} onClick={() => openEdit(account)} /> : null}{canDelete ? <ManagementRowAction tone="delete" icon={<DeleteOutlined />} aria-label={`删除${account.accountName}`} loading={deleteMutation.isPending && deleteMutation.variables === account.id} onClick={() => confirmDelete(account)} /> : null}</div> }
  ]

  return (
    <section className="management-page mail-accounts-page">
      <ManagementCard
        filters={<Form {...filterForm}><form className="management-filter-form" onSubmit={filterForm.handleSubmit(applyFilters)}><FormField control={filterForm.control} name="keyword" render={({ field }) => <FormItem className="management-filter-field"><FormLabel>邮箱</FormLabel><FormControl><Input placeholder="账户名称或邮箱地址" {...field} /></FormControl><FormMessage /></FormItem>} /><FormField control={filterForm.control} name="provider" render={({ field }) => <FormItem className="management-filter-field management-filter-field-select"><FormLabel>类型</FormLabel><Select value={field.value} onValueChange={field.onChange}><FormControl><SelectTrigger><SelectValue /></SelectTrigger></FormControl><SelectContent><SelectItem value="all">全部类型</SelectItem>{providers.map(provider => <SelectItem key={provider.value} value={provider.value}>{provider.label}</SelectItem>)}</SelectContent></Select><FormMessage /></FormItem>} /><FormField control={filterForm.control} name="status" render={({ field }) => <FormItem className="management-filter-field management-filter-field-select"><FormLabel>状态</FormLabel><Select value={field.value} onValueChange={field.onChange}><FormControl><SelectTrigger><SelectValue /></SelectTrigger></FormControl><SelectContent><SelectItem value="all">全部状态</SelectItem><SelectItem value="1">已启用</SelectItem><SelectItem value="0">已停用</SelectItem></SelectContent></Select><FormMessage /></FormItem>} /><div className="management-filter-actions"><Button type="submit"><SearchOutlined />搜索</Button><Button type="button" variant="outline" onClick={resetFilters}><UndoOutlined />重置</Button></div></form></Form>}
        toolbar={<><div className="management-toolbar-selection">邮箱列表 · {filteredAccounts.length} 个账户</div><div className="management-actions"><Button type="button" variant="outline" loading={accountsQuery.isFetching} onClick={() => void accountsQuery.refetch()}><ReloadOutlined />刷新</Button>{canAdd ? <Button type="button" onClick={openCreate}><PlusOutlined />添加邮箱</Button> : null}</div></>}
      >
        <div className="mail-account-stats"><div><span className="total"><InboxOutlined /></span><p><strong>{accounts.length}</strong><small>邮箱总数</small></p></div><div><span className="enabled"><i /></span><p><strong>{accounts.filter(account => account.enabled === 1).length}</strong><small>已启用</small></p></div><div><span className="error"><WarningOutlined /></span><p><strong>{accounts.filter(account => Boolean(account.lastError)).length}</strong><small>连接异常</small></p></div></div>
        <Table<MailAccount> rowKey="id" columns={columns} dataSource={filteredAccounts} loading={accountsQuery.isLoading} pagination={false} scroll={{ x: 1050 }} locale={{ emptyText: '没有符合条件的邮箱账户' }} />
      </ManagementCard>

      <Sheet open={Boolean(detailAccount)} onOpenChange={open => { if (!open) setDetailAccount(undefined) }}>
        <SheetContent className="w-[min(430px,96vw)]">
          <SheetHeader><SheetTitle>邮箱账户详情</SheetTitle></SheetHeader>
          <SheetBody>{detailAccount ? <div className="mail-account-detail"><header><span className={`mail-provider-avatar large ${providerClass(detailAccount.provider)}`}>{providerMark(detailAccount.provider)}</span><div><h3>{detailAccount.accountName}</h3><p>{detailAccount.email}</p></div></header><dl><div><dt>邮箱类型</dt><dd>{providerLabel(providers, detailAccount.provider)}</dd></div><div><dt>账户状态</dt><dd>{detailAccount.enabled === 1 ? '已启用' : '已停用'}</dd></div><div><dt>排序</dt><dd>{detailAccount.sort}</dd></div><div><dt>最后连接</dt><dd>{detailAccount.lastConnectTime ? formatDateTime(detailAccount.lastConnectTime) : '尚未连接'}</dd></div><div><dt>连接结果</dt><dd className={detailAccount.lastError ? 'error' : 'success'}>{detailAccount.lastError || (detailAccount.lastConnectTime ? '连接正常' : '尚未测试')}</dd></div></dl><p className="mail-account-security"><LockOutlined />授权码已经加密保存，不会在详情或编辑接口中返回。</p></div> : null}</SheetBody>
        </SheetContent>
      </Sheet>
      <MailAccountDialog open={dialogOpen} account={editingAccount} providers={providers} providersLoading={providersQuery.isLoading} saving={saveMutation.isPending} onOpenChange={setDialogOpen} onSubmit={values => saveMutation.mutate(values)} />
    </section>
  )
}
