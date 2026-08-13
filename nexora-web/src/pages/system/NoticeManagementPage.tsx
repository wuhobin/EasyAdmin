import { DeleteOutlined, EditOutlined, EyeOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, SendOutlined, UndoOutlined } from '@ant-design/icons'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import AntApp from 'antd/es/app'
import Table from 'antd/es/table'
import Tag from 'antd/es/tag'
import type { ColumnsType } from 'antd/es/table'
import { useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { addNoticeApi, deleteNoticeApi, getNoticeDetailApi, getNoticeListApi, publishNoticeApi, updateNoticeApi, type NoticeQuery, type NoticeRecord } from '@/api/notice'
import { getUserListApi, type SysUserRecord } from '@/api/user'
import { NoticeContent } from '@/components/notifications/NoticeContent'
import { EmptyValue, ManagementCard, ManagementPagination, ManagementRowAction } from '@/components/management/ManagementUi'
import { Button } from '@/components/ui/button'
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { MultiSelect } from '@/components/ui/multi-select'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { emptyNoticeForm, htmlContentByteLength, noticeFormToPayload, noticeRecordToForm, type NoticeFormValues } from '@/pages/system/noticeForm'
import { useAuthStore } from '@/store/authStore'

interface NoticeFilterValues {
  title?: string
  noticeType?: 1 | 2
  status?: 0 | 1
}

const initialQuery: NoticeQuery = { pageNum: 1, pageSize: 10 }

const noticeSchema = z.object({
  id: z.number().optional(),
  title: z.string().trim().min(1, '请输入标题').max(62, '标题不能超过 62 个字符'),
  content: z.string().min(1, '请输入正文'),
  contentFormat: z.enum(['text', 'html']),
  noticeType: z.union([z.literal(1), z.literal(2)]),
  targetType: z.union([z.literal(1), z.literal(3)]),
  targetUserIds: z.array(z.number())
}).superRefine((values, context) => {
  if (values.contentFormat === 'text' && values.content.length > 20_000) context.addIssue({ code: 'custom', path: ['content'], message: '普通文案不能超过 20000 个字符' })
  if (values.contentFormat === 'html' && htmlContentByteLength(values.content) > 256 * 1024) context.addIssue({ code: 'custom', path: ['content'], message: 'HTML 内容不能超过 256KB' })
  if (values.contentFormat === 'html' && /(javascript|vbscript|data)\s*:/i.test(values.content)) context.addIssue({ code: 'custom', path: ['content'], message: 'HTML 中包含不安全的链接协议' })
  if (values.noticeType === 1 && values.targetType === 1 && !values.targetUserIds.length) context.addIssue({ code: 'custom', path: ['targetUserIds'], message: '请选择接收用户' })
})

async function getAllActiveUsers() {
  const users: SysUserRecord[] = []
  const pageSize = 100
  let pageNum = 1
  let total = Number.MAX_SAFE_INTEGER
  while (users.length < total) {
    const response = await getUserListApi({ pageNum, pageSize, status: 1 })
    const records = response.data.records ?? []
    users.push(...records)
    total = response.data.total ?? users.length
    if (!records.length) break
    pageNum += 1
  }
  return users
}

export function NoticeManagementPage() {
  const { message, modal } = AntApp.useApp()
  const queryClient = useQueryClient()
  const permissions = useAuthStore(state => state.user.permissions)
  const [queryParams, setQueryParams] = useState<NoticeQuery>(initialQuery)
  const [editorOpen, setEditorOpen] = useState(false)
  const [detail, setDetail] = useState<NoticeRecord>()
  const filterForm = useForm<NoticeFilterValues>({ defaultValues: { title: '', noticeType: undefined, status: undefined } })
  const form = useForm<NoticeFormValues>({ resolver: zodResolver(noticeSchema), defaultValues: emptyNoticeForm })
  const noticeType = form.watch('noticeType')
  const targetType = form.watch('targetType')
  const contentFormat = form.watch('contentFormat')
  const content = form.watch('content')

  const canAdd = permissions.includes('sys:notice:add')
  const canUpdate = permissions.includes('sys:notice:update')
  const canDelete = permissions.includes('sys:notice:delete')
  const canPublish = permissions.includes('sys:notice:publish')
  const noticesQuery = useQuery({ queryKey: ['notices', queryParams], queryFn: async () => (await getNoticeListApi(queryParams)).data })
  const usersQuery = useQuery({ queryKey: ['notice-active-users'], queryFn: getAllActiveUsers, enabled: editorOpen && noticeType === 1, staleTime: 60_000 })

  const saveMutation = useMutation({
    mutationFn: async (values: NoticeFormValues) => values.id === undefined ? addNoticeApi(noticeFormToPayload(values)) : updateNoticeApi(noticeFormToPayload(values)),
    onSuccess: async (_, values) => {
      await queryClient.invalidateQueries({ queryKey: ['notices'] })
      setEditorOpen(false)
      message.success(values.id === undefined ? '通知草稿已创建' : '通知草稿已更新')
    },
    onError: () => message.error('通知草稿保存失败，请检查内容和接收对象')
  })
  const deleteMutation = useMutation({
    mutationFn: deleteNoticeApi,
    onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: ['notices'] }); message.success('通知草稿已删除') },
    onError: () => message.error('通知草稿删除失败')
  })
  const publishMutation = useMutation({
    mutationFn: publishNoticeApi,
    onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: ['notices'] }); message.success('系统通知已发布') },
    onError: () => message.error('系统通知发布失败，请确认存在可接收的正常用户')
  })
  const detailMutation = useMutation({
    mutationFn: getNoticeDetailApi,
    onSuccess: response => setDetail(response.data),
    onError: () => message.error('通知详情加载失败')
  })

  const openCreate = () => {
    form.reset(emptyNoticeForm)
    setEditorOpen(true)
  }

  const openEdit = (record: NoticeRecord) => {
    form.reset(noticeRecordToForm(record))
    setEditorOpen(true)
  }

  const confirmDelete = (record: NoticeRecord) => {
    modal.confirm({ title: `删除草稿“${record.title}”？`, content: '删除后无法恢复。', okText: '删除', okButtonProps: { danger: true }, cancelText: '取消', onOk: () => deleteMutation.mutateAsync(record.id) })
  }

  const confirmPublish = (record: NoticeRecord) => {
    modal.confirm({ title: `发布“${record.title}”？`, content: '发布后不可修改或删除，并会立即发送给接收用户。', okText: '确认发布', cancelText: '取消', onOk: () => publishMutation.mutateAsync(record.id) })
  }

  const columns = useMemo<ColumnsType<NoticeRecord>>(() => [
    { title: '标题', dataIndex: 'title', width: 260, ellipsis: true, render: value => <span className="management-primary-text">{value}</span> },
    { title: '类型', dataIndex: 'noticeType', width: 88, align: 'center', render: value => value === 2 ? <Tag color="gold">公告</Tag> : <Tag color="blue">通知</Tag> },
    { title: '状态', dataIndex: 'status', width: 92, align: 'center', render: value => value === 1 ? <Tag color="green">已发布</Tag> : <Tag>草稿</Tag> },
    { title: '接收数', dataIndex: 'recipientCount', width: 90, align: 'center', render: value => value ?? 0 },
    { title: '已读 / 未读', key: 'readState', width: 116, align: 'center', render: (_, record) => `${record.readCount ?? 0} / ${record.unreadCount ?? 0}` },
    { title: '发布时间', dataIndex: 'publishTime', width: 180, render: value => <EmptyValue value={value} /> },
    { title: '创建人', dataIndex: 'createName', width: 120, ellipsis: true, render: value => <EmptyValue value={value} /> },
    {
      title: '操作', key: 'action', width: 190, fixed: 'right', align: 'center', render: (_, record) => <div className="management-row-actions">
        <ManagementRowAction tone="data" icon={<EyeOutlined />} aria-label={`查看${record.title}`} loading={detailMutation.isPending && detailMutation.variables === record.id} onClick={() => detailMutation.mutate(record.id)} />
        {record.status === 0 && canUpdate ? <ManagementRowAction tone="edit" icon={<EditOutlined />} aria-label={`编辑${record.title}`} onClick={() => openEdit(record)} /> : null}
        {record.status === 0 && canPublish ? <ManagementRowAction tone="approve" icon={<SendOutlined />} aria-label={`发布${record.title}`} loading={publishMutation.isPending && publishMutation.variables === record.id} onClick={() => confirmPublish(record)} /> : null}
        {record.status === 0 && canDelete ? <ManagementRowAction tone="delete" icon={<DeleteOutlined />} aria-label={`删除${record.title}`} loading={deleteMutation.isPending && deleteMutation.variables === record.id} onClick={() => confirmDelete(record)} /> : null}
      </div>
    }
  ], [canDelete, canPublish, canUpdate, deleteMutation.isPending, deleteMutation.variables, detailMutation.isPending, detailMutation.variables, publishMutation.isPending, publishMutation.variables])

  const applyFilters = (values: NoticeFilterValues) => setQueryParams(previous => ({ ...previous, pageNum: 1, title: values.title?.trim() || undefined, noticeType: values.noticeType, status: values.status }))
  const resetFilters = () => { filterForm.reset(); setQueryParams(previous => ({ pageNum: 1, pageSize: previous.pageSize })) }
  const contentLimit = contentFormat === 'html' ? 256 * 1024 : 20_000
  const contentSize = contentFormat === 'html' ? htmlContentByteLength(content) : content.length

  return (
    <section className="management-page notice-management-page">
      <ManagementCard
        filters={<Form {...filterForm}><form className="management-filter-form" onSubmit={filterForm.handleSubmit(applyFilters)}>
          <FormField control={filterForm.control} name="title" render={({ field }) => <FormItem className="management-filter-field"><FormLabel>标题</FormLabel><FormControl><Input placeholder="请输入标题" {...field} /></FormControl><FormMessage /></FormItem>} />
          <FormField control={filterForm.control} name="noticeType" render={({ field }) => <FormItem className="management-filter-field management-filter-field-select"><FormLabel>类型</FormLabel><Select value={field.value === undefined ? 'all' : String(field.value)} onValueChange={value => field.onChange(value === 'all' ? undefined : Number(value) as 1 | 2)}><FormControl><SelectTrigger><SelectValue placeholder="全部类型" /></SelectTrigger></FormControl><SelectContent><SelectItem value="all">全部类型</SelectItem><SelectItem value="1">通知</SelectItem><SelectItem value="2">公告</SelectItem></SelectContent></Select><FormMessage /></FormItem>} />
          <FormField control={filterForm.control} name="status" render={({ field }) => <FormItem className="management-filter-field management-filter-field-select"><FormLabel>状态</FormLabel><Select value={field.value === undefined ? 'all' : String(field.value)} onValueChange={value => field.onChange(value === 'all' ? undefined : Number(value) as 0 | 1)}><FormControl><SelectTrigger><SelectValue placeholder="全部状态" /></SelectTrigger></FormControl><SelectContent><SelectItem value="all">全部状态</SelectItem><SelectItem value="0">草稿</SelectItem><SelectItem value="1">已发布</SelectItem></SelectContent></Select><FormMessage /></FormItem>} />
          <div className="management-filter-actions"><Button type="submit"><SearchOutlined />搜索</Button><Button type="button" variant="outline" onClick={resetFilters}><UndoOutlined />重置</Button></div>
        </form></Form>}
        toolbar={<><div className="management-toolbar-selection">系统通知列表</div><div className="management-actions"><Button type="button" variant="outline" onClick={() => void noticesQuery.refetch()}><ReloadOutlined />刷新</Button>{canAdd ? <Button type="button" onClick={openCreate}><PlusOutlined />新建草稿</Button> : null}</div></>}
        pagination={<ManagementPagination current={queryParams.pageNum} pageSize={queryParams.pageSize} total={noticesQuery.data?.total ?? 0} onChange={(pageNum, pageSize) => setQueryParams(previous => ({ ...previous, pageNum, pageSize }))} />}
      >
        <Table<NoticeRecord> rowKey="id" loading={noticesQuery.isLoading} columns={columns} dataSource={noticesQuery.data?.records ?? []} pagination={false} scroll={{ x: 1240 }} />
      </ManagementCard>

      <Dialog open={editorOpen} onOpenChange={setEditorOpen}>
        <DialogContent className="max-w-[720px]">
          <Form {...form}>
            <form onSubmit={form.handleSubmit(values => saveMutation.mutate(values))}>
              <DialogHeader><DialogTitle>{form.getValues('id') === undefined ? '新建系统通知' : '编辑系统通知草稿'}</DialogTitle><DialogDescription>保存为草稿后可再次编辑，确认内容和接收范围后再发布。</DialogDescription></DialogHeader>
              <div className="management-dialog-body management-form-grid notice-editor-body">
                <FormField control={form.control} name="title" render={({ field }) => <FormItem className="management-form-span-2"><FormLabel>标题</FormLabel><FormControl><Input maxLength={62} placeholder="请输入通知标题" {...field} /></FormControl><div className="management-character-count">{field.value.length}/62</div><FormMessage /></FormItem>} />
                <FormField control={form.control} name="noticeType" render={({ field }) => <FormItem><FormLabel>类型</FormLabel><Select value={String(field.value)} onValueChange={value => { const next = Number(value) as 1 | 2; field.onChange(next); if (next === 2) { form.setValue('targetType', 3); form.setValue('targetUserIds', []) } }}><FormControl><SelectTrigger><SelectValue /></SelectTrigger></FormControl><SelectContent><SelectItem value="1">通知</SelectItem><SelectItem value="2">公告</SelectItem></SelectContent></Select><FormMessage /></FormItem>} />
                <FormField control={form.control} name="contentFormat" render={({ field }) => <FormItem><FormLabel>内容格式</FormLabel><Select value={field.value} onValueChange={field.onChange}><FormControl><SelectTrigger><SelectValue /></SelectTrigger></FormControl><SelectContent><SelectItem value="text">普通文案</SelectItem><SelectItem value="html">HTML / CSS</SelectItem></SelectContent></Select><FormMessage /></FormItem>} />
                {noticeType === 1 ? <FormField control={form.control} name="targetType" render={({ field }) => <FormItem className="management-form-span-2"><FormLabel>接收对象</FormLabel><Select value={String(field.value)} onValueChange={value => { const next = Number(value) as 1 | 3; field.onChange(next); if (next === 3) form.setValue('targetUserIds', []) }}><FormControl><SelectTrigger><SelectValue /></SelectTrigger></FormControl><SelectContent><SelectItem value="3">全部正常用户</SelectItem><SelectItem value="1">指定用户</SelectItem></SelectContent></Select><FormMessage /></FormItem>} /> : null}
                {noticeType === 1 && targetType === 1 ? <FormField control={form.control} name="targetUserIds" render={({ field, fieldState }) => <FormItem className="management-form-span-2"><FormLabel>指定用户</FormLabel><MultiSelect options={(usersQuery.data ?? []).map(user => ({ value: user.id, label: `${user.nickname || user.email}（${user.email}）` }))} value={field.value} onValueChange={field.onChange} loading={usersQuery.isLoading} invalid={Boolean(fieldState.error)} placeholder="请选择正常用户" searchPlaceholder="搜索用户" loadingText="正在加载用户" emptyText="没有匹配的正常用户" /><FormMessage /></FormItem>} /> : null}
                <FormField control={form.control} name="content" render={({ field }) => <FormItem className="management-form-span-2"><FormLabel>正文</FormLabel><FormControl><Textarea rows={12} className="notice-editor-textarea" placeholder={contentFormat === 'html' ? '直接粘贴 HTML / CSS 源码，脚本不会执行' : '请输入通知文案，支持换行'} {...field} /></FormControl><div className="management-character-count">{contentFormat === 'html' ? `${contentSize}/${contentLimit} 字节` : `${contentSize}/${contentLimit}`}</div><FormMessage /></FormItem>} />
              </div>
              <DialogFooter><DialogClose asChild><Button type="button" variant="outline">取消</Button></DialogClose><Button type="submit" loading={saveMutation.isPending}>保存草稿</Button></DialogFooter>
            </form>
          </Form>
        </DialogContent>
      </Dialog>

      <Dialog open={Boolean(detail)} onOpenChange={open => { if (!open) setDetail(undefined) }}>
        <DialogContent className="max-w-[720px]">
          <DialogHeader><DialogTitle>通知详情</DialogTitle><DialogDescription>查看通知内容、格式和接收范围。</DialogDescription></DialogHeader>
          {detail ? <div className="notice-detail-body"><div className="notice-detail-meta"><Tag color={detail.noticeType === 2 ? 'gold' : 'blue'}>{detail.noticeType === 2 ? '公告' : '通知'}</Tag><span>{detail.contentFormat === 'html' ? 'HTML / CSS' : '普通文案'}</span><span>{detail.targetType === 3 ? '全部正常用户' : `指定用户（${detail.targetUserIds?.length ?? 0} 人）`}</span><span>{detail.publishTime || detail.createTime || ''}</span></div><h2>{detail.title}</h2><NoticeContent notice={detail} /></div> : null}
          <DialogFooter><DialogClose asChild><Button type="button" variant="outline">关闭</Button></DialogClose></DialogFooter>
        </DialogContent>
      </Dialog>
    </section>
  )
}
