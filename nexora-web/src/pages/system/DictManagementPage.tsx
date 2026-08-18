import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, UnorderedListOutlined, UndoOutlined } from '@ant-design/icons'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import AntApp from 'antd/es/app'
import Table from 'antd/es/table'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { addDictApi, addDictDataApi, deleteDictApi, deleteDictDataApi, getDictDataListApi, getDictListApi, updateDictApi, updateDictDataApi, type DictDataQuery, type DictQuery, type SysDictDataRecord, type SysDictRecord } from '@/api/dict'
import { EmptyValue, ManagementCard, ManagementPagination, ManagementRowAction, StatusTag } from '@/components/management/ManagementUi'
import { Button } from '@/components/ui/button'
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Sheet, SheetBody, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Textarea } from '@/components/ui/textarea'
import { dictDataFormToPayload, dictDataRecordToForm, dictFormToPayload, dictRecordToForm, emptyDictDataForm, emptyDictForm, type DictDataFormValues, type DictFormValues } from '@/pages/system/managementForms'
import { useAuthStore } from '@/store/authStore'
import { formatDateTime } from '@/utils/format'
import { useUrlQueryState, type UrlQuerySchema } from '@/utils/urlQueryState'

interface DictFilterValues {
  name?: string
  status?: number
}

const initialQuery: DictQuery = { pageNum: 1, pageSize: 10 }
const querySchema: UrlQuerySchema<DictQuery> = { pageNum: 'number', pageSize: 'number', name: 'string', status: 'number' }
const dictSchema = z.object({
  id: z.number().optional(),
  name: z.string().trim().min(1, '请输入字典名称').max(50, '字典名称不能超过 50 个字符'),
  type: z.string().trim().min(1, '请输入字典类型').max(100, '字典类型不能超过 100 个字符'),
  status: z.number(),
  remark: z.string().trim().max(255, '备注不能超过 255 个字符')
})
const dictDataSchema = z.object({
  id: z.number().optional(),
  dictId: z.number(),
  label: z.string().trim().min(1, '请输入数据标签').max(100, '数据标签不能超过 100 个字符'),
  value: z.string().trim().min(1, '请输入数据键值').max(100, '数据键值不能超过 100 个字符'),
  sort: z.number().int('显示排序必须是整数').min(0, '显示排序不能小于 0'),
  status: z.number(),
  remark: z.string().trim().max(255, '备注不能超过 255 个字符')
})

function DictDataPanel({ dictionary, canAdd, canUpdate, canDelete }: { dictionary: SysDictRecord; canAdd: boolean; canUpdate: boolean; canDelete: boolean }) {
  const form = useForm<DictDataFormValues>({ resolver: zodResolver(dictDataSchema), defaultValues: emptyDictDataForm(dictionary.id) })
  const { message, modal } = AntApp.useApp()
  const queryClient = useQueryClient()
  const [queryParams, setQueryParams] = useState<DictDataQuery>({ pageNum: 1, pageSize: 10, dictId: dictionary.id })
  const [selectedIds, setSelectedIds] = useState<number[]>([])
  const [editingId, setEditingId] = useState<number>()
  const [dialogOpen, setDialogOpen] = useState(false)
  const dataQuery = useQuery({ queryKey: ['dict-data', queryParams], queryFn: async () => (await getDictDataListApi(queryParams)).data })

  const saveMutation = useMutation({
    mutationFn: async (values: DictDataFormValues) => values.id === undefined ? addDictDataApi(dictDataFormToPayload(values)) : updateDictDataApi(dictDataFormToPayload(values)),
    onSuccess: async (_, values) => { await queryClient.invalidateQueries({ queryKey: ['dict-data', queryParams] }); setDialogOpen(false); message.success(values.id === undefined ? '字典数据已新增' : '字典数据已更新') },
    onError: () => message.error('字典数据保存失败')
  })
  const deleteMutation = useMutation({
    mutationFn: (ids: number | number[]) => deleteDictDataApi(ids),
    onSuccess: async (_, ids) => { await queryClient.invalidateQueries({ queryKey: ['dict-data', queryParams] }); setSelectedIds([]); message.success(Array.isArray(ids) ? '选中字典数据已删除' : '字典数据已删除') },
    onError: () => message.error('字典数据删除失败')
  })

  const openCreate = () => {
    setEditingId(undefined)
    form.reset(emptyDictDataForm(dictionary.id))
    setDialogOpen(true)
  }

  const openEdit = (record: SysDictDataRecord) => {
    setEditingId(record.id)
    form.reset(dictDataRecordToForm(record))
    setDialogOpen(true)
  }

  const confirmDelete = (record: SysDictDataRecord) => {
    modal.confirm({ title: `删除字典数据“${record.label}”？`, content: '此操作不可撤销。', okText: '删除', okButtonProps: { danger: true }, cancelText: '取消', onOk: () => deleteMutation.mutateAsync(record.id) })
  }

  const confirmBatchDelete = () => {
    modal.confirm({ title: `删除选中的 ${selectedIds.length} 条字典数据？`, content: '此操作不可撤销。', okText: '批量删除', okButtonProps: { danger: true }, cancelText: '取消', onOk: () => deleteMutation.mutateAsync(selectedIds) })
  }

  const columns: ColumnsType<SysDictDataRecord> = [
    { title: 'ID', dataIndex: 'id', width: 76, align: 'center' },
    { title: '数据标签', dataIndex: 'label', width: 160, ellipsis: true, render: value => <span className="management-primary-text">{value}</span> },
    { title: '数据键值', dataIndex: 'value', width: 160, ellipsis: true, render: value => <code className="management-code">{value}</code> },
    { title: '排序', dataIndex: 'sort', width: 76, align: 'center' },
    { title: '状态', dataIndex: 'status', width: 88, align: 'center', render: status => <StatusTag status={status} /> },
    { title: '备注', dataIndex: 'remark', ellipsis: true, render: value => <EmptyValue value={value} /> },
    { title: '操作', key: 'action', width: 112, align: 'center', render: (_, record) => <div className="management-row-actions">{canUpdate ? <ManagementRowAction tone="edit" icon={<EditOutlined />} aria-label={`修改${record.label}`} onClick={() => openEdit(record)} /> : null}{canDelete ? <ManagementRowAction tone="delete" icon={<DeleteOutlined />} aria-label={`删除${record.label}`} loading={deleteMutation.isPending && deleteMutation.variables === record.id} onClick={() => confirmDelete(record)} /> : null}</div> }
  ]

  return (
    <div className="management-subpanel">
      <div className="management-subpanel-toolbar"><div className="management-toolbar-selection">{selectedIds.length ? `已选择 ${selectedIds.length} 项` : `${dictionary.type} · ${dataQuery.data?.total ?? 0} 条数据`}</div><div className="management-actions">{canDelete ? <Button type="button" variant="destructive" disabled={!selectedIds.length} onClick={confirmBatchDelete}><DeleteOutlined />批量删除</Button> : null}<Button type="button" variant="outline" onClick={() => void dataQuery.refetch()}><ReloadOutlined />刷新</Button>{canAdd ? <Button type="button" onClick={openCreate}><PlusOutlined />新增数据</Button> : null}</div></div>
      <Table<SysDictDataRecord> rowKey="id" loading={dataQuery.isLoading} columns={columns} dataSource={dataQuery.data?.records ?? []} pagination={false} scroll={{ x: 760 }} rowSelection={canDelete ? { selectedRowKeys: selectedIds, preserveSelectedRowKeys: true, onChange: keys => setSelectedIds(keys.map(Number)) } : undefined} />
      <div className="management-pagination"><ManagementPagination current={queryParams.pageNum} pageSize={queryParams.pageSize} total={dataQuery.data?.total ?? 0} onChange={(pageNum, pageSize) => setQueryParams(previous => ({ ...previous, pageNum, pageSize }))} /></div>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="max-w-[540px]">
          <Form {...form}>
            <form onSubmit={form.handleSubmit(values => saveMutation.mutate({ ...values, id: editingId, dictId: dictionary.id }))}>
              <DialogHeader><DialogTitle>{editingId ? '修改字典数据' : '新增字典数据'}</DialogTitle><DialogDescription>配置“{dictionary.name}”下的数据标签、键值、排序和状态。</DialogDescription></DialogHeader>
              <div className="management-dialog-body management-form-grid">
                <div className="management-form-span-2 grid gap-2"><Label htmlFor="dict-data-type">字典类型</Label><Input id="dict-data-type" disabled value={dictionary.type} /></div>
                <FormField control={form.control} name="label" render={({ field }) => <FormItem><FormLabel>数据标签</FormLabel><FormControl><Input placeholder="请输入数据标签" {...field} /></FormControl><FormMessage /></FormItem>} />
                <FormField control={form.control} name="value" render={({ field }) => <FormItem><FormLabel>数据键值</FormLabel><FormControl><Input placeholder="请输入数据键值" {...field} /></FormControl><FormMessage /></FormItem>} />
                <FormField control={form.control} name="sort" render={({ field }) => <FormItem><FormLabel>显示排序</FormLabel><FormControl><Input type="number" min={0} step={1} value={field.value} onBlur={field.onBlur} onChange={event => field.onChange(event.target.valueAsNumber)} /></FormControl><FormMessage /></FormItem>} />
                <FormField control={form.control} name="status" render={({ field }) => <FormItem><FormLabel>状态</FormLabel><FormControl><RadioGroup value={String(field.value)} onValueChange={value => field.onChange(Number(value))} className="management-radio-options">{[{ label: '启用', value: 1 }, { label: '禁用', value: 0 }].map(option => <label className="management-radio-option" htmlFor={`dict-data-status-${option.value}`} key={option.value}><RadioGroupItem id={`dict-data-status-${option.value}`} value={String(option.value)} />{option.label}</label>)}</RadioGroup></FormControl><FormMessage /></FormItem>} />
                <FormField control={form.control} name="remark" render={({ field }) => <FormItem className="management-form-span-2"><FormLabel>备注</FormLabel><FormControl><Textarea rows={3} maxLength={255} placeholder="请输入备注" {...field} /></FormControl><div className="management-character-count">{field.value.length}/255</div><FormMessage /></FormItem>} />
              </div>
              <DialogFooter><DialogClose asChild><Button type="button" variant="outline">取消</Button></DialogClose><Button type="submit" loading={saveMutation.isPending}>保存</Button></DialogFooter>
            </form>
          </Form>
        </DialogContent>
      </Dialog>
    </div>
  )
}

export function DictManagementPage() {
  const filterForm = useForm<DictFilterValues>({ defaultValues: { name: '', status: undefined } })
  const dictForm = useForm<DictFormValues>({ resolver: zodResolver(dictSchema), defaultValues: emptyDictForm })
  const { message, modal } = AntApp.useApp()
  const queryClient = useQueryClient()
  const permissions = useAuthStore(state => state.user.permissions)
  const [queryParams, setQueryParams] = useUrlQueryState(initialQuery, querySchema)
  const [selectedIds, setSelectedIds] = useState<number[]>([])
  const [editingId, setEditingId] = useState<number>()
  const [dialogOpen, setDialogOpen] = useState(false)
  const [activeDictionary, setActiveDictionary] = useState<SysDictRecord>()

  useEffect(() => filterForm.reset({ name: queryParams.name ?? '', status: queryParams.status }), [filterForm, queryParams.name, queryParams.status])

  const canAdd = permissions.includes('sys:dict:add')
  const canUpdate = permissions.includes('sys:dict:update')
  const canDelete = permissions.includes('sys:dict:delete')
  const dictQuery = useQuery({ queryKey: ['dictionaries', queryParams], queryFn: async () => (await getDictListApi(queryParams)).data })

  const saveMutation = useMutation({
    mutationFn: async (values: DictFormValues) => values.id === undefined ? addDictApi(dictFormToPayload(values)) : updateDictApi(dictFormToPayload(values)),
    onSuccess: async (_, values) => { await queryClient.invalidateQueries({ queryKey: ['dictionaries'] }); setDialogOpen(false); message.success(values.id === undefined ? '字典已新增' : '字典已更新') },
    onError: () => message.error('字典保存失败，请检查字典类型是否重复')
  })
  const deleteMutation = useMutation({
    mutationFn: (ids: number | number[]) => deleteDictApi(ids),
    onSuccess: async (_, ids) => { await queryClient.invalidateQueries({ queryKey: ['dictionaries'] }); setSelectedIds([]); message.success(Array.isArray(ids) ? '选中字典已删除' : '字典已删除') },
    onError: () => message.error('字典删除失败，可能仍包含字典数据')
  })

  const openCreate = () => {
    setEditingId(undefined)
    dictForm.reset(emptyDictForm)
    setDialogOpen(true)
  }

  const openEdit = (record: SysDictRecord) => {
    setEditingId(record.id)
    dictForm.reset(dictRecordToForm(record))
    setDialogOpen(true)
  }

  const confirmDelete = (record: SysDictRecord) => {
    modal.confirm({ title: `删除字典“${record.name}”？`, content: '字典下仍有数据时后端可能拒绝删除。', okText: '删除', okButtonProps: { danger: true }, cancelText: '取消', onOk: () => deleteMutation.mutateAsync(record.id) })
  }

  const confirmBatchDelete = () => {
    modal.confirm({ title: `删除选中的 ${selectedIds.length} 个字典？`, content: '此操作不可撤销。', okText: '批量删除', okButtonProps: { danger: true }, cancelText: '取消', onOk: () => deleteMutation.mutateAsync(selectedIds) })
  }

  const columns: ColumnsType<SysDictRecord> = [
    { title: '字典名称', dataIndex: 'name', width: 190, ellipsis: true, render: value => <span className="management-primary-text">{value}</span> },
    { title: '字典类型', dataIndex: 'type', width: 210, ellipsis: true, render: value => <code className="management-code">{value}</code> },
    { title: '状态', dataIndex: 'status', width: 88, align: 'center', render: status => <StatusTag status={status} /> },
    { title: '备注', dataIndex: 'remark', ellipsis: true, render: value => <EmptyValue value={value} /> },
    { title: '创建时间', dataIndex: 'createTime', width: 180, render: value => <EmptyValue value={formatDateTime(value)} /> },
    { title: '操作', key: 'action', width: 156, fixed: 'right', align: 'center', render: (_, record) => <div className="management-row-actions"><ManagementRowAction tone="data" icon={<UnorderedListOutlined />} aria-label={`查看${record.name}的字典数据`} onClick={() => setActiveDictionary(record)} />{canUpdate ? <ManagementRowAction tone="edit" icon={<EditOutlined />} aria-label={`修改${record.name}`} onClick={() => openEdit(record)} /> : null}{canDelete ? <ManagementRowAction tone="delete" icon={<DeleteOutlined />} aria-label={`删除${record.name}`} loading={deleteMutation.isPending && deleteMutation.variables === record.id} onClick={() => confirmDelete(record)} /> : null}</div> }
  ]

  return (
    <section className="management-page">
      <ManagementCard
        filters={<Form {...filterForm}><form className="management-filter-form" onSubmit={filterForm.handleSubmit(values => setQueryParams(previous => ({ ...previous, pageNum: 1, name: values.name?.trim() || undefined, status: values.status })))}>
          <FormField control={filterForm.control} name="name" render={({ field }) => <FormItem className="management-filter-field"><FormLabel>字典名称</FormLabel><FormControl><Input placeholder="请输入字典名称" {...field} /></FormControl><FormMessage /></FormItem>} />
          <FormField control={filterForm.control} name="status" render={({ field }) => <FormItem className="management-filter-field management-filter-field-select"><FormLabel>状态</FormLabel><Select value={field.value === undefined ? 'all' : String(field.value)} onValueChange={value => field.onChange(value === 'all' ? undefined : Number(value))}><FormControl><SelectTrigger><SelectValue placeholder="全部状态" /></SelectTrigger></FormControl><SelectContent><SelectItem value="all">全部状态</SelectItem><SelectItem value="1">启用</SelectItem><SelectItem value="0">禁用</SelectItem></SelectContent></Select><FormMessage /></FormItem>} />
          <div className="management-filter-actions"><Button type="submit"><SearchOutlined />搜索</Button><Button type="button" variant="outline" onClick={() => { filterForm.reset(); setQueryParams(previous => ({ pageNum: 1, pageSize: previous.pageSize })) }}><UndoOutlined />重置</Button></div>
        </form></Form>}
        toolbar={<><div className="management-toolbar-selection">{selectedIds.length ? `已选择 ${selectedIds.length} 项` : '字典列表'}</div><div className="management-actions">{canDelete ? <Button type="button" variant="destructive" disabled={!selectedIds.length} onClick={confirmBatchDelete}><DeleteOutlined />批量删除</Button> : null}<Button type="button" variant="outline" onClick={() => void dictQuery.refetch()}><ReloadOutlined />刷新</Button>{canAdd ? <Button type="button" onClick={openCreate}><PlusOutlined />新增字典</Button> : null}</div></>}
        pagination={<ManagementPagination current={queryParams.pageNum} pageSize={queryParams.pageSize} total={dictQuery.data?.total ?? 0} onChange={(pageNum, pageSize) => setQueryParams(previous => ({ ...previous, pageNum, pageSize }))} />}
      >
        <Table<SysDictRecord> rowKey="id" loading={dictQuery.isLoading} columns={columns} dataSource={dictQuery.data?.records ?? []} pagination={false} scroll={{ x: 980 }} rowSelection={canDelete ? { selectedRowKeys: selectedIds, preserveSelectedRowKeys: true, onChange: keys => setSelectedIds(keys.map(Number)) } : undefined} />
      </ManagementCard>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="max-w-[540px]">
          <Form {...dictForm}>
            <form onSubmit={dictForm.handleSubmit(values => saveMutation.mutate({ ...values, id: editingId }))}>
              <DialogHeader><DialogTitle>{editingId ? '修改字典' : '新增字典'}</DialogTitle><DialogDescription>维护字典名称、稳定类型、状态和用途说明。</DialogDescription></DialogHeader>
              <div className="management-dialog-body management-form-stack">
                <FormField control={dictForm.control} name="name" render={({ field }) => <FormItem><FormLabel>字典名称</FormLabel><FormControl><Input placeholder="请输入字典名称" {...field} /></FormControl><FormMessage /></FormItem>} />
                <FormField control={dictForm.control} name="type" render={({ field }) => <FormItem><FormLabel>字典类型</FormLabel><FormControl><Input placeholder="例如：sys_user_sex" {...field} /></FormControl><FormMessage /></FormItem>} />
                <FormField control={dictForm.control} name="status" render={({ field }) => <FormItem><FormLabel>状态</FormLabel><FormControl><RadioGroup value={String(field.value)} onValueChange={value => field.onChange(Number(value))} className="management-radio-options">{[{ label: '启用', value: 1 }, { label: '禁用', value: 0 }].map(option => <label className="management-radio-option" htmlFor={`dict-status-${option.value}`} key={option.value}><RadioGroupItem id={`dict-status-${option.value}`} value={String(option.value)} />{option.label}</label>)}</RadioGroup></FormControl><FormMessage /></FormItem>} />
                <FormField control={dictForm.control} name="remark" render={({ field }) => <FormItem><FormLabel>备注</FormLabel><FormControl><Textarea rows={3} maxLength={255} placeholder="请输入备注" {...field} /></FormControl><div className="management-character-count">{field.value.length}/255</div><FormMessage /></FormItem>} />
              </div>
              <DialogFooter><DialogClose asChild><Button type="button" variant="outline">取消</Button></DialogClose><Button type="submit" loading={saveMutation.isPending}>保存</Button></DialogFooter>
            </form>
          </Form>
        </DialogContent>
      </Dialog>

      <Sheet open={Boolean(activeDictionary)} onOpenChange={open => { if (!open) setActiveDictionary(undefined) }}>
        <SheetContent>
          <SheetHeader><SheetTitle className="text-base font-semibold">{activeDictionary ? `字典数据 · ${activeDictionary.name}` : '字典数据'}</SheetTitle><SheetDescription className="mt-1.5 text-xs text-[var(--nexora-placeholder)]">维护当前字典下可用的数据标签、键值和显示顺序。</SheetDescription></SheetHeader>
          <SheetBody>{activeDictionary ? <DictDataPanel key={activeDictionary.id} dictionary={activeDictionary} canAdd={canAdd} canUpdate={canUpdate} canDelete={canDelete} /> : null}</SheetBody>
        </SheetContent>
      </Sheet>
    </section>
  )
}
