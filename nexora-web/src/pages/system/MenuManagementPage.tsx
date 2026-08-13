import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, RightOutlined } from '@ant-design/icons'
import { zodResolver } from '@hookform/resolvers/zod'
import AntApp from 'antd/es/app'
import Space from 'antd/es/space'
import Table from 'antd/es/table'
import Tag from 'antd/es/tag'
import TreeSelect from 'antd/es/tree-select'
import type { ColumnsType } from 'antd/es/table'
import { useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { IconPicker } from '@/components/IconPicker'
import { ManagementRowAction } from '@/components/management/ManagementUi'
import { MenuIcon } from '@/components/MenuIcon'
import { Button } from '@/components/ui/button'
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Switch } from '@/components/ui/switch'
import { createMenuApi, deleteMenuApi, getMenuTreeApi, updateMenuApi, type MenuType, type SysMenuRecord } from '@/api/menu'
import { useAuthStore } from '@/store/authStore'
import { emptyMenuForm, menuFormToPayload, menuRecordToForm, normalizeMenuRecords, type MenuFormValues } from '@/pages/system/menuForm'

function toTreeData(records: SysMenuRecord[]): { title: string; value: number; key: number; children?: ReturnType<typeof toTreeData> }[] {
  return records.map(record => ({ title: record.title, value: record.id, key: record.id, children: record.children?.length ? toTreeData(record.children) : undefined }))
}

const menuSchema = z.object({
  parentId: z.number(),
  title: z.string().trim().min(1, '请输入菜单名称').max(50, '菜单名称不能超过 50 个字符'),
  sort: z.number().int('排序必须是整数').min(1, '排序不能小于 1'),
  type: z.enum(['CATALOG', 'MENU', 'BUTTON']),
  icon: z.string(),
  path: z.string(),
  redirect: z.string(),
  component: z.string(),
  perm: z.string(),
  visible: z.boolean(),
  external: z.boolean()
}).superRefine((values, context) => {
  if (values.type !== 'BUTTON' && !values.path.trim()) context.addIssue({ code: 'custom', path: ['path'], message: '请输入路由地址' })
  if (values.type === 'BUTTON' && !values.perm.trim()) context.addIssue({ code: 'custom', path: ['perm'], message: '请输入权限标识' })
})

export function MenuManagementPage() {
  const form = useForm<MenuFormValues>({ resolver: zodResolver(menuSchema), defaultValues: emptyMenuForm })
  const queryClient = useQueryClient()
  const { message, modal } = AntApp.useApp()
  const [editingId, setEditingId] = useState<number>()
  const [dialogOpen, setDialogOpen] = useState(false)
  const user = useAuthStore(state => state.user)
  const canAdd = user.permissions.includes('sys:menu:add')
  const canUpdate = user.permissions.includes('sys:menu:update')
  const canDelete = user.permissions.includes('sys:menu:delete')
  const query = useQuery({ queryKey: ['menu-tree'], queryFn: async () => normalizeMenuRecords((await getMenuTreeApi()).data) })
  const saveMutation = useMutation({
    mutationFn: async ({ id, values }: { id?: number; values: MenuFormValues }) => {
      const payload = menuFormToPayload(values, id)
      return id ? updateMenuApi(payload) : createMenuApi(payload)
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['menu-tree'] })
      await queryClient.invalidateQueries({ queryKey: ['auth-bootstrap'] })
      setDialogOpen(false)
      message.success(editingId ? '菜单已更新' : '菜单已创建')
    },
    onError: () => message.error('菜单保存失败，请检查权限或表单内容')
  })
  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteMenuApi(id),
    onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: ['menu-tree'] }); message.success('菜单已删除') },
    onError: () => message.error('菜单删除失败，可能存在子菜单')
  })

  const records = query.data ?? []
  const treeData = useMemo(() => [{ title: '顶级菜单', value: 0, key: 0, children: toTreeData(records) }], [records])
  const menuType = form.watch('type')

  const openCreate = (parentId = 0) => {
    setEditingId(undefined)
    form.reset({ ...emptyMenuForm, parentId })
    setDialogOpen(true)
  }

  const openEdit = (record: SysMenuRecord) => {
    setEditingId(record.id)
    form.reset(menuRecordToForm(record))
    setDialogOpen(true)
  }

  const confirmDelete = (record: SysMenuRecord) => {
    modal.confirm({ title: `删除“${record.title}”？`, content: '存在子菜单时后端会拒绝删除。此操作不可撤销。', okText: '删除', okButtonProps: { danger: true }, cancelText: '取消', onOk: () => deleteMutation.mutateAsync(record.id) })
  }

  const columns: ColumnsType<SysMenuRecord> = [
    { title: '菜单名称', dataIndex: 'title', key: 'title', width: 250, render: (title: string, record) => <Space><span className="menu-table-icon"><MenuIcon value={record.icon} /></span><span>{title}</span></Space> },
    { title: '类型', dataIndex: 'type', key: 'type', width: 92, render: (type: MenuType) => <Tag color={type === 'CATALOG' ? 'purple' : type === 'MENU' ? 'blue' : 'default'}>{type === 'CATALOG' ? '目录' : type === 'MENU' ? '菜单' : '按钮'}</Tag> },
    { title: '路由地址', dataIndex: 'path', key: 'path', width: 170, ellipsis: true },
    { title: '组件标识', dataIndex: 'component', key: 'component', width: 190, ellipsis: true, render: value => value || '—' },
    { title: '权限标识', dataIndex: 'perm', key: 'perm', width: 180, ellipsis: true, render: value => value || '—' },
    { title: '排序', dataIndex: 'sort', key: 'sort', width: 70, align: 'center' },
    { title: '状态', dataIndex: 'hidden', key: 'hidden', width: 82, render: (hidden: number) => <Tag color={hidden === 1 ? 'default' : 'green'}>{hidden === 1 ? '隐藏' : '显示'}</Tag> },
    {
      title: '操作',
      key: 'action',
      width: 156,
      fixed: 'right',
      render: (_, record) => <div className="management-row-actions menu-row-actions">
        {canAdd ? <ManagementRowAction tone="add" icon={<PlusOutlined />} aria-label={`为${record.title}新增子菜单`} onClick={() => openCreate(record.id)} /> : null}
        {canUpdate ? <ManagementRowAction tone="edit" icon={<EditOutlined />} aria-label={`修改${record.title}`} onClick={() => openEdit(record)} /> : null}
        {canDelete ? <ManagementRowAction tone="delete" icon={<DeleteOutlined />} aria-label={`删除${record.title}`} loading={deleteMutation.isPending && deleteMutation.variables === record.id} onClick={() => confirmDelete(record)} /> : null}
      </div>
    }
  ]

  return <section className="menu-management-page">
    <div className="menu-data-card">
      <div className="menu-page-toolbar"><div className="menu-page-actions"><Button type="button" variant="outline" onClick={() => void query.refetch()}><ReloadOutlined />刷新</Button>{canAdd ? <Button type="button" onClick={() => openCreate()}><PlusOutlined />新增菜单</Button> : null}</div></div>
      <Table<SysMenuRecord> rowKey="id" loading={query.isLoading} columns={columns} dataSource={records} pagination={false} scroll={{ x: 1120 }} expandable={{ defaultExpandAllRows: true, rowExpandable: record => Boolean(record.children?.length), expandIcon: ({ expanded, expandable, onExpand, record }) => expandable ? <button type="button" className={`menu-expand-trigger ${expanded ? 'is-expanded' : ''}`} aria-label={expanded ? '收起子菜单' : '展开子菜单'} aria-expanded={expanded} onClick={event => onExpand(record, event)}><RightOutlined /></button> : <span className="menu-expand-placeholder" /> }} />
    </div>
    <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
      <DialogContent className="max-w-[720px]">
        <Form {...form}>
          <form onSubmit={form.handleSubmit(values => saveMutation.mutate({ id: editingId, values }))}>
            <DialogHeader><DialogTitle>{editingId ? '编辑菜单' : '新增菜单'}</DialogTitle><DialogDescription>组件标识使用稳定的逻辑键，例如 `/system/menu/index`，不要填写物理 React 文件路径。</DialogDescription></DialogHeader>
            <div className="management-dialog-body management-form-grid">
              <FormField control={form.control} name="parentId" render={({ field, fieldState }) => <FormItem className="management-form-span-2"><FormLabel>上级菜单</FormLabel><FormControl><TreeSelect value={field.value} onChange={field.onChange} status={fieldState.error ? 'error' : undefined} treeData={treeData} treeDefaultExpandAll placeholder="选择上级菜单" style={{ width: '100%' }} /></FormControl><FormMessage /></FormItem>} />
              <FormField control={form.control} name="type" render={({ field }) => <FormItem className="management-form-span-2"><FormLabel>菜单类型</FormLabel><FormControl><RadioGroup value={field.value} onValueChange={field.onChange} className="management-radio-options">{[{ label: '目录', value: 'CATALOG' }, { label: '菜单', value: 'MENU' }, { label: '按钮', value: 'BUTTON' }].map(option => <label className="management-radio-option" htmlFor={`menu-type-${option.value}`} key={option.value}><RadioGroupItem id={`menu-type-${option.value}`} value={option.value} />{option.label}</label>)}</RadioGroup></FormControl><FormMessage /></FormItem>} />
              <FormField control={form.control} name="title" render={({ field }) => <FormItem><FormLabel>菜单名称</FormLabel><FormControl><Input placeholder="例如：菜单管理" {...field} /></FormControl><FormMessage /></FormItem>} />
              <FormField control={form.control} name="sort" render={({ field }) => <FormItem><FormLabel>排序</FormLabel><FormControl><Input type="number" min={1} step={1} value={field.value} onBlur={field.onBlur} onChange={event => field.onChange(event.target.valueAsNumber)} /></FormControl><FormMessage /></FormItem>} />
              {menuType !== 'BUTTON' ? <FormField control={form.control} name="icon" render={({ field }) => <FormItem className="management-form-span-2"><FormLabel>图标</FormLabel><IconPicker value={field.value} onChange={field.onChange} /><FormMessage /></FormItem>} /> : null}
              {menuType !== 'BUTTON' ? <FormField control={form.control} name="path" render={({ field }) => <FormItem><FormLabel>路由地址</FormLabel><FormControl><Input placeholder="/system/menu" {...field} /></FormControl><FormMessage /></FormItem>} /> : null}
              <FormField control={form.control} name="redirect" render={({ field }) => <FormItem className={menuType === 'BUTTON' ? 'management-form-span-2' : undefined}><FormLabel>重定向地址</FormLabel><FormControl><Input placeholder="可选" {...field} /></FormControl><FormMessage /></FormItem>} />
              {menuType === 'MENU' ? <FormField control={form.control} name="component" render={({ field }) => <FormItem className="management-form-span-2"><FormLabel>组件标识</FormLabel><FormControl><Input placeholder="/system/menu/index" {...field} /></FormControl><FormMessage /></FormItem>} /> : null}
              {menuType === 'BUTTON' ? <FormField control={form.control} name="perm" render={({ field }) => <FormItem className="management-form-span-2"><FormLabel>权限标识</FormLabel><FormControl><Input placeholder="sys:menu:add" {...field} /></FormControl><FormMessage /></FormItem>} /> : null}
              <FormField control={form.control} name="visible" render={({ field }) => <FormItem><div className="management-switch-field"><div className="management-switch-copy"><span>显示状态</span><small>{field.value ? '当前在导航中显示' : '当前从导航中隐藏'}</small></div><FormControl><Switch checked={field.value} onCheckedChange={field.onChange} /></FormControl></div><FormMessage /></FormItem>} />
              <FormField control={form.control} name="external" render={({ field }) => <FormItem><div className="management-switch-field"><div className="management-switch-copy"><span>外链</span><small>{field.value ? '使用外部地址打开' : '使用站内路由打开'}</small></div><FormControl><Switch checked={field.value} onCheckedChange={field.onChange} /></FormControl></div><FormMessage /></FormItem>} />
            </div>
            <DialogFooter><DialogClose asChild><Button type="button" variant="outline">取消</Button></DialogClose><Button type="submit" loading={saveMutation.isPending}>保存</Button></DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  </section>
}
