import { CheckOutlined, DeleteOutlined, EditOutlined, KeyOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, UndoOutlined } from '@ant-design/icons'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import AntApp from 'antd/es/app'
import Avatar from 'antd/es/avatar'
import Table from 'antd/es/table'
import type { ColumnsType } from 'antd/es/table'
import { useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { auditUserApi, createUserApi, deleteUserApi, getUserListApi, resetPasswordApi, updateUserApi, type SysUserRecord, type UserQuery } from '@/api/user'
import type { PasswordConfig } from '@/api/config'
import { getAllRoleListApi } from '@/api/role'
import { EmptyValue, ManagementCard, ManagementPagination, ManagementRowAction, StatusTag } from '@/components/management/ManagementUi'
import { Button } from '@/components/ui/button'
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { MultiSelect } from '@/components/ui/multi-select'
import { PasswordInput } from '@/components/ui/password-input'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { emptyUserForm, userFormToPayload, userRecordToForm, type UserFormValues } from '@/pages/system/managementForms'
import { useAuthStore } from '@/store/authStore'
import { usePublicConfigStore } from '@/store/publicConfigStore'
import { passwordPolicyDescription, validatePasswordByPolicy } from '@/utils/password-policy'

interface UserFilterValues {
  nickname?: string
  email?: string
  status?: number
}

interface ResetPasswordValues {
  password: string
  confirmPassword: string
}

const initialQuery: UserQuery = { pageNum: 1, pageSize: 10 }

function createUserSchema(passwordPolicy: PasswordConfig, editing: boolean) {
  return z.object({
    id: z.number().optional(),
    nickname: z.string().trim().min(1, '请输入昵称').max(30, '昵称不能超过 30 个字符'),
    email: z.string().trim().min(1, '请输入邮箱').email('请输入正确的邮箱地址'),
    password: z.string(),
    mobile: z.string().trim().refine(value => !value || /^1[3-9]\d{9}$/.test(value), '请输入正确的手机号'),
    sex: z.number(),
    status: z.number(),
    roleIds: z.array(z.number()).min(1, '请选择角色')
  }).superRefine((values, context) => {
    if (editing) return
    const error = validatePasswordByPolicy(values.password, passwordPolicy)
    if (error) context.addIssue({ code: 'custom', path: ['password'], message: error })
  })
}

function createResetPasswordSchema(passwordPolicy: PasswordConfig) {
  return z.object({ password: z.string(), confirmPassword: z.string().min(1, '请再次输入新密码') }).superRefine((values, context) => {
    const error = validatePasswordByPolicy(values.password, passwordPolicy)
    if (error) context.addIssue({ code: 'custom', path: ['password'], message: error })
    if (values.password !== values.confirmPassword) context.addIssue({ code: 'custom', path: ['confirmPassword'], message: '两次输入的密码不一致' })
  })
}

export function UserManagementPage() {
  const { message, modal } = AntApp.useApp()
  const queryClient = useQueryClient()
  const permissions = useAuthStore(state => state.user.permissions)
  const passwordPolicy = usePublicConfigStore(state => state.config.password)
  const [queryParams, setQueryParams] = useState<UserQuery>(initialQuery)
  const [selectedIds, setSelectedIds] = useState<number[]>([])
  const [editingId, setEditingId] = useState<number>()
  const [userDialogOpen, setUserDialogOpen] = useState(false)
  const [resetUser, setResetUser] = useState<SysUserRecord>()
  const userSchema = useMemo(() => createUserSchema(passwordPolicy, editingId !== undefined), [editingId, passwordPolicy])
  const resetPasswordSchema = useMemo(() => createResetPasswordSchema(passwordPolicy), [passwordPolicy])
  const filterForm = useForm<UserFilterValues>({ defaultValues: { nickname: '', email: '', status: undefined } })
  const userForm = useForm<UserFormValues>({ resolver: zodResolver(userSchema), defaultValues: emptyUserForm })
  const passwordForm = useForm<ResetPasswordValues>({ resolver: zodResolver(resetPasswordSchema), defaultValues: { password: '', confirmPassword: '' } })

  const canAdd = permissions.includes('sys:user:add')
  const canUpdate = permissions.includes('sys:user:update')
  const canDelete = permissions.includes('sys:user:delete')
  const canReset = permissions.includes('sys:user:reset')
  const usersQuery = useQuery({ queryKey: ['users', queryParams], queryFn: async () => (await getUserListApi(queryParams)).data })
  const rolesQuery = useQuery({ queryKey: ['roles-all'], queryFn: async () => (await getAllRoleListApi()).data, staleTime: 60_000 })

  const saveMutation = useMutation({
    mutationFn: async ({ values, editing }: { values: UserFormValues; editing: boolean }) => {
      const payload = userFormToPayload(values, editing)
      return editing ? updateUserApi(payload) : createUserApi(payload)
    },
    onSuccess: async (_, variables) => {
      await queryClient.invalidateQueries({ queryKey: ['users'] })
      setUserDialogOpen(false)
      message.success(variables.editing ? '用户已更新' : '用户已创建')
    },
    onError: () => message.error('用户保存失败，请检查表单内容或权限')
  })
  const deleteMutation = useMutation({
    mutationFn: (ids: number | number[]) => deleteUserApi(ids),
    onSuccess: async (_, ids) => {
      await queryClient.invalidateQueries({ queryKey: ['users'] })
      setSelectedIds([])
      message.success(Array.isArray(ids) ? '选中用户已删除' : '用户已删除')
    },
    onError: () => message.error('用户删除失败')
  })
  const auditMutation = useMutation({
    mutationFn: (id: number) => auditUserApi(id),
    onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: ['users'] }); message.success('用户已审核通过') },
    onError: () => message.error('用户审核失败')
  })
  const resetMutation = useMutation({
    mutationFn: ({ id, password }: { id: number; password: string }) => resetPasswordApi(id, password),
    onSuccess: () => { setResetUser(undefined); passwordForm.reset(); message.success('密码已重置') },
    onError: () => message.error('密码重置失败')
  })

  const roleNames = (roleIds: number[]) => {
    const names = (rolesQuery.data ?? []).filter(role => roleIds.includes(role.id)).map(role => role.name)
    return names.join('、')
  }

  const openCreate = () => {
    setEditingId(undefined)
    userForm.reset(emptyUserForm)
    setUserDialogOpen(true)
  }

  const openEdit = (record: SysUserRecord) => {
    setEditingId(record.id)
    userForm.reset(userRecordToForm(record))
    setUserDialogOpen(true)
  }

  const confirmDelete = (record: SysUserRecord) => {
    modal.confirm({ title: `删除用户“${record.nickname || record.email}”？`, content: '删除后无法恢复，关联登录信息也会一并失效。', okText: '删除', okButtonProps: { danger: true }, cancelText: '取消', onOk: () => deleteMutation.mutateAsync(record.id) })
  }

  const confirmBatchDelete = () => {
    modal.confirm({ title: `删除选中的 ${selectedIds.length} 个用户？`, content: '此操作不可撤销。', okText: '批量删除', okButtonProps: { danger: true }, cancelText: '取消', onOk: () => deleteMutation.mutateAsync(selectedIds) })
  }

  const confirmAudit = (record: SysUserRecord) => {
    modal.confirm({ title: `审核通过“${record.nickname || record.email}”？`, content: '通过后该用户可按已分配角色访问系统。', okText: '审核通过', cancelText: '取消', onOk: () => auditMutation.mutateAsync(record.id) })
  }

  const columns: ColumnsType<SysUserRecord> = [
    { title: '用户', key: 'user', width: 200, render: (_, record) => <div className="management-user-cell"><Avatar shape="square" src={record.avatar}>{(record.nickname || record.email).slice(0, 1).toUpperCase()}</Avatar><span><span className="management-user-name">{record.nickname || '-'}</span><small>{record.email}</small></span></div> },
    { title: '手机号', dataIndex: 'mobile', width: 130, render: value => <EmptyValue value={value} /> },
    { title: '角色', dataIndex: 'roleIds', width: 170, ellipsis: true, render: (roleIds: number[]) => <EmptyValue value={roleNames(roleIds)} /> },
    { title: '登录 IP', dataIndex: 'ip', width: 130, render: value => <EmptyValue value={value} /> },
    { title: '登录地址', dataIndex: 'ipLocation', width: 150, ellipsis: true, render: value => <EmptyValue value={value} /> },
    { title: '状态', dataIndex: 'status', width: 88, align: 'center', render: status => <StatusTag status={status} pending /> },
    { title: '创建时间', dataIndex: 'createTime', width: 168, render: value => <EmptyValue value={value} /> },
    {
      title: '操作', key: 'action', width: 190, fixed: 'right', align: 'center', render: (_, record) => <div className="management-row-actions">
        {record.status === 2 && canUpdate ? <ManagementRowAction tone="approve" icon={<CheckOutlined />} aria-label={`审核通过${record.nickname}`} loading={auditMutation.isPending && auditMutation.variables === record.id} onClick={() => confirmAudit(record)} /> : null}
        {canUpdate ? <ManagementRowAction tone="edit" icon={<EditOutlined />} aria-label={`修改${record.nickname}`} onClick={() => openEdit(record)} /> : null}
        {canReset ? <ManagementRowAction tone="settings" icon={<KeyOutlined />} aria-label={`重置${record.nickname}的密码`} onClick={() => { setResetUser(record); passwordForm.reset() }} /> : null}
        {canDelete ? <ManagementRowAction tone="delete" icon={<DeleteOutlined />} aria-label={`删除${record.nickname}`} disabled={record.id === 1} loading={deleteMutation.isPending && deleteMutation.variables === record.id} onClick={() => confirmDelete(record)} /> : null}
      </div>
    }
  ]

  const applyFilters = (values: UserFilterValues) => setQueryParams(previous => ({
    ...previous,
    pageNum: 1,
    nickname: values.nickname?.trim() || undefined,
    email: values.email?.trim() || undefined,
    status: values.status
  }))

  const resetFilters = () => {
    filterForm.reset()
    setQueryParams(previous => ({ pageNum: 1, pageSize: previous.pageSize }))
  }

  return (
    <section className="management-page">
      <ManagementCard
        filters={<Form {...filterForm}><form className="management-filter-form" onSubmit={filterForm.handleSubmit(applyFilters)}>
          <FormField control={filterForm.control} name="nickname" render={({ field }) => <FormItem className="management-filter-field"><FormLabel>昵称</FormLabel><FormControl><Input placeholder="请输入昵称" {...field} /></FormControl><FormMessage /></FormItem>} />
          <FormField control={filterForm.control} name="email" render={({ field }) => <FormItem className="management-filter-field"><FormLabel>邮箱</FormLabel><FormControl><Input type="email" placeholder="请输入邮箱" {...field} /></FormControl><FormMessage /></FormItem>} />
          <FormField control={filterForm.control} name="status" render={({ field }) => <FormItem className="management-filter-field management-filter-field-select"><FormLabel>状态</FormLabel><Select value={field.value === undefined ? 'all' : String(field.value)} onValueChange={value => field.onChange(value === 'all' ? undefined : Number(value))}><FormControl><SelectTrigger><SelectValue placeholder="全部状态" /></SelectTrigger></FormControl><SelectContent><SelectItem value="all">全部状态</SelectItem><SelectItem value="1">启用</SelectItem><SelectItem value="0">禁用</SelectItem><SelectItem value="2">待审核</SelectItem></SelectContent></Select><FormMessage /></FormItem>} />
          <div className="management-filter-actions"><Button type="submit"><SearchOutlined />搜索</Button><Button type="button" variant="outline" onClick={resetFilters}><UndoOutlined />重置</Button></div>
        </form></Form>}
        toolbar={<><div className="management-toolbar-selection">{selectedIds.length ? `已选择 ${selectedIds.length} 项` : '用户列表'}</div><div className="management-actions">{canDelete ? <Button type="button" variant="destructive" disabled={!selectedIds.length} onClick={confirmBatchDelete}><DeleteOutlined />批量删除</Button> : null}<Button type="button" variant="outline" onClick={() => void usersQuery.refetch()}><ReloadOutlined />刷新</Button>{canAdd ? <Button type="button" onClick={openCreate}><PlusOutlined />新增用户</Button> : null}</div></>}
        pagination={<ManagementPagination current={queryParams.pageNum} pageSize={queryParams.pageSize} total={usersQuery.data?.total ?? 0} onChange={(pageNum, pageSize) => setQueryParams(previous => ({ ...previous, pageNum, pageSize }))} />}
      >
        <Table<SysUserRecord> rowKey="id" loading={usersQuery.isLoading} columns={columns} dataSource={usersQuery.data?.records ?? []} pagination={false} scroll={{ x: 1220 }} rowSelection={canDelete ? { selectedRowKeys: selectedIds, preserveSelectedRowKeys: true, getCheckboxProps: record => ({ disabled: record.id === 1 }), onChange: keys => setSelectedIds(keys.map(Number)) } : undefined} />
      </ManagementCard>

      <Dialog open={userDialogOpen} onOpenChange={setUserDialogOpen}>
        <DialogContent className="max-w-[680px]">
          <Form {...userForm}>
            <form onSubmit={userForm.handleSubmit(values => saveMutation.mutate({ values: { ...values, id: editingId }, editing: editingId !== undefined }))}>
              <DialogHeader><DialogTitle>{editingId ? '修改用户' : '新增用户'}</DialogTitle><DialogDescription>{editingId ? '更新用户资料、状态和角色权限。' : '填写登录资料并为新用户分配角色。'}</DialogDescription></DialogHeader>
              <div className="management-dialog-body management-form-grid">
                <FormField control={userForm.control} name="nickname" render={({ field }) => <FormItem><FormLabel>昵称</FormLabel><FormControl><Input placeholder="请输入昵称" {...field} /></FormControl><FormMessage /></FormItem>} />
                <FormField control={userForm.control} name="email" render={({ field }) => <FormItem><FormLabel>邮箱</FormLabel><FormControl><Input type="email" disabled={editingId !== undefined} placeholder="请输入邮箱" autoComplete="email" {...field} /></FormControl><FormMessage /></FormItem>} />
                <FormField control={userForm.control} name="mobile" render={({ field }) => <FormItem><FormLabel>手机号</FormLabel><FormControl><Input inputMode="tel" placeholder="请输入手机号" {...field} /></FormControl><FormMessage /></FormItem>} />
                {editingId === undefined ? <FormField control={userForm.control} name="password" render={({ field }) => <FormItem><FormLabel>密码</FormLabel><FormControl><PasswordInput autoComplete="new-password" placeholder={passwordPolicyDescription(passwordPolicy)} {...field} /></FormControl><FormMessage /></FormItem>} /> : <div />}
                <FormField control={userForm.control} name="sex" render={({ field }) => <FormItem><FormLabel>性别</FormLabel><FormControl><RadioGroup value={String(field.value)} onValueChange={value => field.onChange(Number(value))} className="management-radio-options">{[{ label: '保密', value: 0 }, { label: '男', value: 1 }, { label: '女', value: 2 }].map(option => <label className="management-radio-option" htmlFor={`user-sex-${option.value}`} key={option.value}><RadioGroupItem id={`user-sex-${option.value}`} value={String(option.value)} />{option.label}</label>)}</RadioGroup></FormControl><FormMessage /></FormItem>} />
                <FormField control={userForm.control} name="status" render={({ field }) => <FormItem><FormLabel>状态</FormLabel><FormControl><RadioGroup value={String(field.value)} onValueChange={value => field.onChange(Number(value))} disabled={editingId === 1} className="management-radio-options">{[{ label: '启用', value: 1 }, { label: '禁用', value: 0 }, ...(editingId === undefined ? [] : [{ label: '待审核', value: 2 }])].map(option => <label className="management-radio-option" htmlFor={`user-status-${option.value}`} key={option.value}><RadioGroupItem id={`user-status-${option.value}`} value={String(option.value)} />{option.label}</label>)}</RadioGroup></FormControl><FormMessage /></FormItem>} />
                <FormField control={userForm.control} name="roleIds" render={({ field, fieldState }) => <FormItem><FormLabel>角色</FormLabel><MultiSelect options={(rolesQuery.data ?? []).map(role => ({ label: role.name, value: role.id }))} value={field.value} onValueChange={field.onChange} disabled={editingId === 1} loading={rolesQuery.isLoading} invalid={Boolean(fieldState.error)} placeholder="请选择角色" /><FormMessage /></FormItem>} />
              </div>
              <DialogFooter><DialogClose asChild><Button type="button" variant="outline">取消</Button></DialogClose><Button type="submit" loading={saveMutation.isPending}>{editingId ? '保存修改' : '创建用户'}</Button></DialogFooter>
            </form>
          </Form>
        </DialogContent>
      </Dialog>

      <Dialog open={Boolean(resetUser)} onOpenChange={open => { if (!open) setResetUser(undefined) }}>
        <DialogContent className="max-w-[500px]">
          <Form {...passwordForm}>
            <form onSubmit={passwordForm.handleSubmit(values => resetUser && resetMutation.mutate({ id: resetUser.id, password: values.password }))}>
              <DialogHeader><DialogTitle>重置密码</DialogTitle><DialogDescription>为“{resetUser?.nickname || resetUser?.email}”设置新的登录密码。</DialogDescription></DialogHeader>
              <div className="management-dialog-body management-form-stack">
                <FormField control={passwordForm.control} name="password" render={({ field }) => <FormItem><FormLabel>新密码</FormLabel><FormControl><PasswordInput autoComplete="new-password" placeholder={passwordPolicyDescription(passwordPolicy)} {...field} /></FormControl><FormMessage /></FormItem>} />
                <FormField control={passwordForm.control} name="confirmPassword" render={({ field }) => <FormItem><FormLabel>确认密码</FormLabel><FormControl><PasswordInput autoComplete="new-password" placeholder="请再次输入新密码" {...field} /></FormControl><FormMessage /></FormItem>} />
              </div>
              <DialogFooter><DialogClose asChild><Button type="button" variant="outline">取消</Button></DialogClose><Button type="submit" loading={resetMutation.isPending}>确认重置</Button></DialogFooter>
            </form>
          </Form>
        </DialogContent>
      </Dialog>
    </section>
  )
}
