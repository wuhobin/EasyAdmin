import { DeleteOutlined, DownloadOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, SettingOutlined, UndoOutlined } from '@ant-design/icons'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import AntApp from 'antd/es/app'
import Table from 'antd/es/table'
import Tree from 'antd/es/tree'
import type { ColumnsType } from 'antd/es/table'
import type { DataNode } from 'antd/es/tree'
import { useForm } from 'react-hook-form'
import { useEffect, useState, type Key } from 'react'
import { z } from 'zod'
import { getMenuTreeApi, type SysMenuRecord } from '@/api/menu'
import { createRoleApi, deleteRoleApi, exportRoleApi, getRoleListApi, getRoleMenusApi, updateRoleApi, updateRoleMenusApi, type RoleQuery, type SysRoleRecord } from '@/api/role'
import { EmptyValue, ManagementCard, ManagementPagination, ManagementRowAction } from '@/components/management/ManagementUi'
import { Button } from '@/components/ui/button'
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { emptyRoleForm, roleFormToPayload, roleRecordToForm, splitRoleMenuSelection, type RoleFormValues } from '@/pages/system/managementForms'
import { useAuthStore } from '@/store/authStore'
import { formatDateTime } from '@/utils/format'
import { useUrlQueryState, type UrlQuerySchema } from '@/utils/urlQueryState'

interface RoleFilterValues {
  name?: string
}

const initialQuery: RoleQuery = { pageNum: 1, pageSize: 10 }
const querySchema: UrlQuerySchema<RoleQuery> = { pageNum: 'number', pageSize: 'number', name: 'string' }
const roleSchema = z.object({
  id: z.number().optional(),
  name: z.string().trim().min(1, '请输入角色名称').max(50, '角色名称不能超过 50 个字符'),
  code: z.string().trim().min(1, '请输入角色编码').max(50, '角色编码不能超过 50 个字符'),
  remarks: z.string().trim().max(255, '备注不能超过 255 个字符')
})

function toTreeData(records: SysMenuRecord[]): DataNode[] {
  return records.map(record => ({ key: record.id, title: record.title, children: record.children?.length ? toTreeData(record.children) : undefined }))
}

export function RoleManagementPage() {
  const filterForm = useForm<RoleFilterValues>({ defaultValues: { name: '' } })
  const roleForm = useForm<RoleFormValues>({ resolver: zodResolver(roleSchema), defaultValues: emptyRoleForm })
  const { message, modal } = AntApp.useApp()
  const queryClient = useQueryClient()
  const permissions = useAuthStore(state => state.user.permissions)
  const [queryParams, setQueryParams] = useUrlQueryState(initialQuery, querySchema)
  const [selectedIds, setSelectedIds] = useState<number[]>([])
  const [editingId, setEditingId] = useState<number>()
  const [roleDialogOpen, setRoleDialogOpen] = useState(false)
  const [permissionRole, setPermissionRole] = useState<SysRoleRecord>()
  const [checkedMenuIds, setCheckedMenuIds] = useState<number[]>([])
  const [halfCheckedMenuIds, setHalfCheckedMenuIds] = useState<number[]>([])

  useEffect(() => filterForm.reset({ name: queryParams.name ?? '' }), [filterForm, queryParams.name])

  const canAdd = permissions.includes('sys:role:add')
  const canUpdate = permissions.includes('sys:role:update')
  const canDelete = permissions.includes('sys:role:delete')
  const canAssign = permissions.includes('sys:role:menus')
  const rolesQuery = useQuery({ queryKey: ['roles', queryParams], queryFn: async () => (await getRoleListApi(queryParams)).data })
  const menusQuery = useQuery({ queryKey: ['menu-tree'], queryFn: async () => (await getMenuTreeApi()).data, staleTime: 60_000 })

  const saveMutation = useMutation({
    mutationFn: async (values: RoleFormValues) => values.id === undefined ? createRoleApi(roleFormToPayload(values)) : updateRoleApi(roleFormToPayload(values)),
    onSuccess: async (_, values) => { await queryClient.invalidateQueries({ queryKey: ['roles'] }); setRoleDialogOpen(false); message.success(values.id === undefined ? '角色已创建' : '角色已更新') },
    onError: () => message.error('角色保存失败，请检查角色编码是否重复')
  })
  const deleteMutation = useMutation({
    mutationFn: (ids: number | number[]) => deleteRoleApi(ids),
    onSuccess: async (_, ids) => { await queryClient.invalidateQueries({ queryKey: ['roles'] }); setSelectedIds([]); message.success(Array.isArray(ids) ? '选中角色已删除' : '角色已删除') },
    onError: () => message.error('角色删除失败，可能仍有关联用户')
  })
  const permissionMutation = useMutation({
    mutationFn: ({ roleId, menuIds }: { roleId: number; menuIds: number[] }) => updateRoleMenusApi(roleId, menuIds),
    onSuccess: () => { setPermissionRole(undefined); message.success('角色权限已更新') },
    onError: () => message.error('角色权限保存失败')
  })

  const openCreate = () => {
    setEditingId(undefined)
    roleForm.reset(emptyRoleForm)
    setRoleDialogOpen(true)
  }

  const openEdit = (record: SysRoleRecord) => {
    setEditingId(record.id)
    roleForm.reset(roleRecordToForm(record))
    setRoleDialogOpen(true)
  }

  const openPermission = async (record: SysRoleRecord) => {
    setPermissionRole(record)
    setCheckedMenuIds([])
    setHalfCheckedMenuIds([])
    try {
      const [response, menus] = await Promise.all([
        getRoleMenusApi(record.id),
        menusQuery.data ? Promise.resolve(menusQuery.data) : getMenuTreeApi().then(result => result.data)
      ])
      const selection = splitRoleMenuSelection(menus, response.data)
      setCheckedMenuIds(selection.checkedIds)
      setHalfCheckedMenuIds(selection.halfCheckedIds)
    } catch {
      setPermissionRole(undefined)
      message.error('角色权限加载失败')
    }
  }

  const confirmDelete = (record: SysRoleRecord) => {
    modal.confirm({ title: `删除角色“${record.name}”？`, content: '仍被用户使用的角色可能无法删除。', okText: '删除', okButtonProps: { danger: true }, cancelText: '取消', onOk: () => deleteMutation.mutateAsync(record.id) })
  }

  const confirmBatchDelete = () => {
    modal.confirm({ title: `删除选中的 ${selectedIds.length} 个角色？`, content: '此操作不可撤销。', okText: '批量删除', okButtonProps: { danger: true }, cancelText: '取消', onOk: () => deleteMutation.mutateAsync(selectedIds) })
  }

  const handleExport = async () => {
    try {
      const blob = await exportRoleApi()
      const url = URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = '角色数据.xlsx'
      anchor.click()
      window.setTimeout(() => URL.revokeObjectURL(url), 0)
      message.success('角色数据已导出')
    } catch {
      message.error('角色数据导出失败')
    }
  }

  const columns: ColumnsType<SysRoleRecord> = [
    { title: 'ID', dataIndex: 'id', width: 80, align: 'center' },
    { title: '角色名称', dataIndex: 'name', width: 180, ellipsis: true, render: value => <span className="management-primary-text">{value}</span> },
    { title: '角色编码', dataIndex: 'code', width: 180, ellipsis: true, render: value => <code className="management-code">{value}</code> },
    { title: '备注', dataIndex: 'remarks', ellipsis: true, render: value => <EmptyValue value={value} /> },
    { title: '创建时间', dataIndex: 'createTime', width: 180, render: value => <EmptyValue value={formatDateTime(value)} /> },
    { title: '操作', key: 'action', width: 156, fixed: 'right', align: 'center', render: (_, record) => <div className="management-row-actions">{canUpdate ? <ManagementRowAction tone="edit" icon={<EditOutlined />} aria-label={`修改${record.name}`} onClick={() => openEdit(record)} /> : null}{canAssign ? <ManagementRowAction tone="settings" icon={<SettingOutlined />} aria-label={`分配${record.name}的权限`} onClick={() => void openPermission(record)} /> : null}{canDelete ? <ManagementRowAction tone="delete" icon={<DeleteOutlined />} aria-label={`删除${record.name}`} loading={deleteMutation.isPending && deleteMutation.variables === record.id} onClick={() => confirmDelete(record)} /> : null}</div> }
  ]

  return (
    <section className="management-page">
      <ManagementCard
        filters={<Form {...filterForm}><form className="management-filter-form" onSubmit={filterForm.handleSubmit(values => setQueryParams(previous => ({ ...previous, pageNum: 1, name: values.name?.trim() || undefined })))}><FormField control={filterForm.control} name="name" render={({ field }) => <FormItem className="management-filter-field"><FormLabel>角色名称</FormLabel><FormControl><Input placeholder="请输入角色名称" {...field} /></FormControl><FormMessage /></FormItem>} /><div className="management-filter-actions"><Button type="submit"><SearchOutlined />搜索</Button><Button type="button" variant="outline" onClick={() => { filterForm.reset(); setQueryParams(previous => ({ pageNum: 1, pageSize: previous.pageSize })) }}><UndoOutlined />重置</Button></div></form></Form>}
        toolbar={<><div className="management-toolbar-selection">{selectedIds.length ? `已选择 ${selectedIds.length} 项` : '角色列表'}</div><div className="management-actions">{canDelete ? <Button type="button" variant="destructive" disabled={!selectedIds.length} onClick={confirmBatchDelete}><DeleteOutlined />批量删除</Button> : null}{canDelete ? <Button type="button" variant="outline" onClick={() => void handleExport()}><DownloadOutlined />导出</Button> : null}<Button type="button" variant="outline" onClick={() => void rolesQuery.refetch()}><ReloadOutlined />刷新</Button>{canAdd ? <Button type="button" onClick={openCreate}><PlusOutlined />新增角色</Button> : null}</div></>}
        pagination={<ManagementPagination current={queryParams.pageNum} pageSize={queryParams.pageSize} total={rolesQuery.data?.total ?? 0} onChange={(pageNum, pageSize) => setQueryParams(previous => ({ ...previous, pageNum, pageSize }))} />}
      >
        <Table<SysRoleRecord> rowKey="id" loading={rolesQuery.isLoading} columns={columns} dataSource={rolesQuery.data?.records ?? []} pagination={false} scroll={{ x: 900 }} rowSelection={canDelete ? { selectedRowKeys: selectedIds, preserveSelectedRowKeys: true, onChange: keys => setSelectedIds(keys.map(Number)) } : undefined} />
      </ManagementCard>

      <Dialog open={roleDialogOpen} onOpenChange={setRoleDialogOpen}>
        <DialogContent className="max-w-[580px]">
          <Form {...roleForm}>
            <form onSubmit={roleForm.handleSubmit(values => saveMutation.mutate({ ...values, id: editingId }))}>
              <DialogHeader><DialogTitle>{editingId ? '修改角色' : '新增角色'}</DialogTitle><DialogDescription>维护角色名称、稳定编码和用途说明。</DialogDescription></DialogHeader>
              <div className="management-dialog-body management-form-stack">
                <FormField control={roleForm.control} name="name" render={({ field }) => <FormItem><FormLabel>角色名称</FormLabel><FormControl><Input placeholder="请输入角色名称" {...field} /></FormControl><FormMessage /></FormItem>} />
                <FormField control={roleForm.control} name="code" render={({ field }) => <FormItem><FormLabel>角色编码</FormLabel><FormControl><Input placeholder="例如：admin" {...field} /></FormControl><FormMessage /></FormItem>} />
                <FormField control={roleForm.control} name="remarks" render={({ field }) => <FormItem><FormLabel>备注</FormLabel><FormControl><Textarea rows={4} maxLength={255} placeholder="请输入备注信息" {...field} /></FormControl><div className="management-character-count">{field.value.length}/255</div><FormMessage /></FormItem>} />
              </div>
              <DialogFooter><DialogClose asChild><Button type="button" variant="outline">取消</Button></DialogClose><Button type="submit" loading={saveMutation.isPending}>保存</Button></DialogFooter>
            </form>
          </Form>
        </DialogContent>
      </Dialog>

      <Dialog open={Boolean(permissionRole)} onOpenChange={open => { if (!open) setPermissionRole(undefined) }}>
        <DialogContent className="max-w-[620px]">
          <DialogHeader><DialogTitle>分配权限</DialogTitle><DialogDescription>为“{permissionRole?.name}”选择可访问的菜单和操作权限。</DialogDescription></DialogHeader>
          <div className="management-dialog-body"><div className="management-permission-tree" aria-busy={menusQuery.isLoading}>
            <Tree checkable blockNode defaultExpandAll checkedKeys={checkedMenuIds} treeData={toTreeData(menusQuery.data ?? [])} onCheck={(keys, info) => { const checked = Array.isArray(keys) ? keys : keys.checked; setCheckedMenuIds(checked.map(Number)); setHalfCheckedMenuIds(((info.halfCheckedKeys ?? []) as Key[]).map(Number)) }} />
          </div></div>
          <DialogFooter><DialogClose asChild><Button type="button" variant="outline">取消</Button></DialogClose><Button type="button" loading={permissionMutation.isPending} onClick={() => permissionRole && permissionMutation.mutate({ roleId: permissionRole.id, menuIds: [...new Set([...checkedMenuIds, ...halfCheckedMenuIds])] })}>保存权限</Button></DialogFooter>
        </DialogContent>
      </Dialog>
    </section>
  )
}
