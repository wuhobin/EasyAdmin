import { ClockCircleOutlined, DeleteOutlined, EditOutlined, HistoryOutlined, PlayCircleOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, UndoOutlined } from '@ant-design/icons'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import AntApp from 'antd/es/app'
import Table from 'antd/es/table'
import Tag from 'antd/es/tag'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router-dom'
import { z } from 'zod'
import { addJobApi, changeJobStatusApi, deleteJobApi, getJobDetailApi, getJobListApi, runJobApi, updateJobApi, type JobQuery, type JobRecord } from '@/api/job'
import { ManagementCard, ManagementPagination, ManagementRowAction } from '@/components/management/ManagementUi'
import { CronBuilderDialog } from '@/components/monitor/CronBuilderDialog'
import { Button } from '@/components/ui/button'
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Switch } from '@/components/ui/switch'
import { emptyJobForm, jobFormToPayload, jobRecordToForm, type JobFormValues } from '@/pages/monitor/monitorForms'
import { flattenRoutes } from '@/routes/routeAdapter'
import { useAuthStore } from '@/store/authStore'
import { useUrlQueryState, type UrlQuerySchema } from '@/utils/urlQueryState'
import { useRouteStore } from '@/store/routeStore'

interface JobFilterValues { jobName: string; jobGroup?: string; status?: string }
const initialQuery: JobQuery = { pageNum: 1, pageSize: 10 }
const querySchema: UrlQuerySchema<JobQuery> = { pageNum: 'number', pageSize: 'number', jobName: 'string', jobGroup: 'string', status: 'string' }
const jobGroupLabels: Record<string, string> = { DEFAULT: '默认', SYSTEM: '系统' }
const schema = z.object({
  jobId: z.number().optional(),
  jobName: z.string().trim().min(1, '请输入任务名称').max(100, '任务名称不能超过 100 个字符'),
  jobGroup: z.string().min(1, '请选择任务组'),
  invokeTarget: z.string().trim().min(1, '请输入调用方法').max(500, '调用方法不能超过 500 个字符'),
  cronExpression: z.string().trim().min(1, '请输入 Cron 表达式').refine(value => value.split(/\s+/).length >= 6 && value.split(/\s+/).length <= 7, 'Cron 表达式应为 6 或 7 个字段'),
  misfirePolicy: z.string(), concurrent: z.string(), status: z.string()
})

export function ScheduledJobsPage() {
  const { message, modal } = AntApp.useApp()
  const queryClient = useQueryClient()
  const permissions = useAuthStore(state => state.user.permissions)
  const routes = useRouteStore(state => state.routes)
  const [queryParams, setQueryParams] = useUrlQueryState(initialQuery, querySchema)
  const [selectedIds, setSelectedIds] = useState<number[]>([])
  const [dialogOpen, setDialogOpen] = useState(false)
  const [cronOpen, setCronOpen] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const filterForm = useForm<JobFilterValues>({ defaultValues: { jobName: '', jobGroup: undefined, status: undefined } })

  useEffect(() => filterForm.reset({ jobName: queryParams.jobName ?? '', jobGroup: queryParams.jobGroup, status: queryParams.status }), [filterForm, queryParams.jobGroup, queryParams.jobName, queryParams.status])
  const jobForm = useForm<JobFormValues>({ resolver: zodResolver(schema), defaultValues: emptyJobForm })
  const canAdd = permissions.includes('sys:job:add')
  const canUpdate = permissions.includes('sys:job:update')
  const canDelete = permissions.includes('sys:job:delete')
  const canChangeStatus = permissions.includes('sys:job:changeStatus')
  const jobsQuery = useQuery({ queryKey: ['scheduled-jobs', queryParams], queryFn: async () => (await getJobListApi(queryParams)).data })

  const refreshJobs = async () => { await queryClient.invalidateQueries({ queryKey: ['scheduled-jobs'] }) }
  const saveMutation = useMutation({
    mutationFn: async (values: JobFormValues) => values.jobId === undefined ? addJobApi(jobFormToPayload(values)) : updateJobApi(jobFormToPayload(values)),
    onSuccess: async (_, values) => { setDialogOpen(false); await refreshJobs(); message.success(values.jobId === undefined ? '定时任务已创建' : '定时任务已更新') },
    onError: () => message.error('定时任务保存失败')
  })
  const deleteMutation = useMutation({
    mutationFn: deleteJobApi,
    onSuccess: async (_, ids) => { setSelectedIds([]); await refreshJobs(); message.success(Array.isArray(ids) ? '选中的定时任务已删除' : '定时任务已删除') },
    onError: () => message.error('定时任务删除失败')
  })
  const statusMutation = useMutation({
    mutationFn: ({ jobId, status }: { jobId: number; status: string }) => changeJobStatusApi(jobId, status),
    onSuccess: async (_, variables) => { await refreshJobs(); message.success(variables.status === '0' ? '任务已启用' : '任务已暂停') },
    onError: () => { void refreshJobs(); message.error('任务状态修改失败') }
  })
  const runMutation = useMutation({ mutationFn: runJobApi, onSuccess: () => message.success('任务已提交执行'), onError: () => message.error('任务执行失败') })

  const openCreate = () => { jobForm.reset(emptyJobForm); setDialogOpen(true) }
  const openEdit = async (record: JobRecord) => {
    setDetailLoading(true)
    setDialogOpen(true)
    try { jobForm.reset(jobRecordToForm((await getJobDetailApi(record.jobId)).data)) }
    catch { setDialogOpen(false); message.error('任务详情加载失败') }
    finally { setDetailLoading(false) }
  }

  const confirmDelete = (record: JobRecord) => modal.confirm({ title: `删除定时任务“${record.jobName}”？`, content: '删除后无法恢复。', okText: '删除', okButtonProps: { danger: true }, cancelText: '取消', onOk: () => deleteMutation.mutateAsync(record.jobId) })
  const confirmBatchDelete = () => modal.confirm({ title: `删除选中的 ${selectedIds.length} 个定时任务？`, content: '此操作不可撤销。', okText: '批量删除', okButtonProps: { danger: true }, cancelText: '取消', onOk: () => deleteMutation.mutateAsync(selectedIds) })
  const confirmStatusChange = (record: JobRecord, checked: boolean) => {
    const status = checked ? '0' : '1'
    modal.confirm({ title: `${checked ? '启用' : '暂停'}定时任务“${record.jobName}”？`, content: checked ? '启用后任务将按 Cron 规则继续调度。' : '暂停后不会产生新的自动调度。', okText: checked ? '确认启用' : '确认暂停', cancelText: '取消', onOk: () => statusMutation.mutateAsync({ jobId: record.jobId, status }) })
  }
  const confirmRun = (record: JobRecord) => modal.confirm({ title: `立即执行“${record.jobName}”？`, content: `调用目标：${record.invokeTarget}`, okText: '执行一次', cancelText: '取消', onOk: () => runMutation.mutateAsync({ jobId: record.jobId, jobGroup: record.jobGroup }) })

  const logPath = flattenRoutes(routes).find(route => route.component === '/monitor/job/log' || route.fullPath === '/system/log/job-log')?.fullPath || '/system/log/job-log'
  const columns: ColumnsType<JobRecord> = [
    { title: 'ID', dataIndex: 'jobId', width: 78, align: 'center' },
    { title: '任务名称', dataIndex: 'jobName', width: 170, ellipsis: true },
    { title: '任务组名', dataIndex: 'jobGroup', width: 110, align: 'center', render: value => jobGroupLabels[value] ?? value },
    { title: '调用目标字符串', dataIndex: 'invokeTarget', width: 250, ellipsis: true, render: value => <code className="management-code">{value}</code> },
    { title: 'Cron 表达式', dataIndex: 'cronExpression', width: 180, ellipsis: true, render: value => <code className="management-code">{value}</code> },
    { title: '状态', dataIndex: 'status', width: 100, align: 'center', render: (status, record) => canChangeStatus ? <Switch checked={status === '0'} disabled={statusMutation.isPending} aria-label={`${status === '0' ? '暂停' : '启用'}${record.jobName}`} onCheckedChange={checked => confirmStatusChange(record, checked)} /> : status === '0' ? <Tag color="green">正常</Tag> : <Tag>暂停</Tag> },
    { title: '操作', key: 'action', width: 156, fixed: 'right', align: 'center', render: (_, record) => <div className="management-row-actions"><ManagementRowAction tone="approve" icon={<PlayCircleOutlined />} aria-label={`执行一次${record.jobName}`} loading={runMutation.isPending && runMutation.variables?.jobId === record.jobId} onClick={() => confirmRun(record)} />{canUpdate ? <ManagementRowAction tone="edit" icon={<EditOutlined />} aria-label={`修改${record.jobName}`} onClick={() => void openEdit(record)} /> : null}{canDelete ? <ManagementRowAction tone="delete" icon={<DeleteOutlined />} aria-label={`删除${record.jobName}`} loading={deleteMutation.isPending && deleteMutation.variables === record.jobId} onClick={() => confirmDelete(record)} /> : null}</div> }
  ]

  const applyFilters = (values: JobFilterValues) => setQueryParams(previous => ({ ...previous, pageNum: 1, jobName: values.jobName.trim() || undefined, jobGroup: values.jobGroup, status: values.status }))
  const resetFilters = () => { filterForm.reset(); setQueryParams(previous => ({ pageNum: 1, pageSize: previous.pageSize })) }

  return (
    <section className="management-page scheduled-jobs-page">
      <ManagementCard
        filters={<Form {...filterForm}><form className="management-filter-form" onSubmit={filterForm.handleSubmit(applyFilters)}><FormField control={filterForm.control} name="jobName" render={({ field }) => <FormItem className="management-filter-field"><FormLabel>任务名称</FormLabel><FormControl><Input placeholder="请输入任务名称" {...field} /></FormControl><FormMessage /></FormItem>} /><FormField control={filterForm.control} name="jobGroup" render={({ field }) => <FormItem className="management-filter-field management-filter-field-select"><FormLabel>任务组名</FormLabel><Select value={field.value ?? 'all'} onValueChange={value => field.onChange(value === 'all' ? undefined : value)}><FormControl><SelectTrigger><SelectValue placeholder="全部任务组" /></SelectTrigger></FormControl><SelectContent><SelectItem value="all">全部任务组</SelectItem><SelectItem value="DEFAULT">默认</SelectItem><SelectItem value="SYSTEM">系统</SelectItem></SelectContent></Select><FormMessage /></FormItem>} /><FormField control={filterForm.control} name="status" render={({ field }) => <FormItem className="management-filter-field management-filter-field-select"><FormLabel>任务状态</FormLabel><Select value={field.value ?? 'all'} onValueChange={value => field.onChange(value === 'all' ? undefined : value)}><FormControl><SelectTrigger><SelectValue placeholder="全部状态" /></SelectTrigger></FormControl><SelectContent><SelectItem value="all">全部状态</SelectItem><SelectItem value="0">正常</SelectItem><SelectItem value="1">暂停</SelectItem></SelectContent></Select><FormMessage /></FormItem>} /><div className="management-filter-actions"><Button type="submit"><SearchOutlined />搜索</Button><Button type="button" variant="outline" onClick={resetFilters}><UndoOutlined />重置</Button></div></form></Form>}
        toolbar={<><div className="management-toolbar-selection">{selectedIds.length ? `已选择 ${selectedIds.length} 项` : '定时任务列表'}</div><div className="management-actions">{canDelete ? <Button type="button" variant="destructive" disabled={!selectedIds.length} onClick={confirmBatchDelete}><DeleteOutlined />批量删除</Button> : null}<Button variant="outline" asChild><Link to={logPath}><HistoryOutlined />调度日志</Link></Button><Button type="button" variant="outline" loading={jobsQuery.isFetching} onClick={() => void jobsQuery.refetch()}><ReloadOutlined />刷新</Button>{canAdd ? <Button type="button" onClick={openCreate}><PlusOutlined />新增任务</Button> : null}</div></>}
        pagination={<ManagementPagination current={queryParams.pageNum} pageSize={queryParams.pageSize} total={jobsQuery.data?.total ?? 0} onChange={(pageNum, pageSize) => setQueryParams(previous => ({ ...previous, pageNum: pageSize === previous.pageSize ? pageNum : 1, pageSize }))} />}
      >
        <Table<JobRecord> rowKey="jobId" loading={jobsQuery.isLoading} columns={columns} dataSource={jobsQuery.data?.records ?? []} pagination={false} scroll={{ x: 1120 }} rowSelection={canDelete ? { selectedRowKeys: selectedIds, preserveSelectedRowKeys: true, onChange: keys => setSelectedIds(keys.map(Number)) } : undefined} />
      </ManagementCard>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="max-w-[720px]">
          <Form {...jobForm}><form onSubmit={jobForm.handleSubmit(values => saveMutation.mutate(values))}><DialogHeader><DialogTitle>{jobForm.getValues('jobId') === undefined ? '添加定时任务' : '修改定时任务'}</DialogTitle><DialogDescription>配置任务调用目标、执行周期与异常处理策略。</DialogDescription></DialogHeader><div className="management-dialog-body management-form-grid job-dialog-body" aria-busy={detailLoading}><FormField control={jobForm.control} name="jobName" render={({ field }) => <FormItem><FormLabel>任务名称</FormLabel><FormControl><Input placeholder="请输入任务名称" disabled={detailLoading} {...field} /></FormControl><FormMessage /></FormItem>} /><FormField control={jobForm.control} name="jobGroup" render={({ field }) => <FormItem><FormLabel>任务组名</FormLabel><Select value={field.value} onValueChange={field.onChange} disabled={detailLoading}><FormControl><SelectTrigger><SelectValue placeholder="请选择任务组" /></SelectTrigger></FormControl><SelectContent><SelectItem value="DEFAULT">默认</SelectItem><SelectItem value="SYSTEM">系统</SelectItem></SelectContent></Select><FormMessage /></FormItem>} /><FormField control={jobForm.control} name="invokeTarget" render={({ field }) => <FormItem className="management-form-span-2"><FormLabel>调用方法</FormLabel><FormControl><Input placeholder="例如：sampleTask.run('value')" disabled={detailLoading} {...field} /></FormControl><FormMessage /></FormItem>} /><FormField control={jobForm.control} name="cronExpression" render={({ field }) => <FormItem className="management-form-span-2"><FormLabel>Cron 表达式</FormLabel><div className="job-cron-input"><FormControl><Input placeholder="0 0 * * * ?" disabled={detailLoading} {...field} /></FormControl><Button type="button" variant="outline" aria-label="打开 Cron 表达式生成器" onClick={() => setCronOpen(true)}><ClockCircleOutlined />生成器</Button></div><FormMessage /></FormItem>} /><FormField control={jobForm.control} name="misfirePolicy" render={({ field }) => <FormItem><FormLabel>执行策略</FormLabel><FormControl><RadioGroup value={field.value} onValueChange={field.onChange} className="management-radio-options job-radio-options">{[{ value: '1', label: '立即执行' }, { value: '2', label: '执行一次' }, { value: '3', label: '放弃执行' }].map(option => <label key={option.value} className="management-radio-option"><RadioGroupItem value={option.value} />{option.label}</label>)}</RadioGroup></FormControl><FormMessage /></FormItem>} /><FormField control={jobForm.control} name="concurrent" render={({ field }) => <FormItem><FormLabel>并发策略</FormLabel><FormControl><RadioGroup value={field.value} onValueChange={field.onChange} className="management-radio-options job-radio-options">{[{ value: '0', label: '允许并发' }, { value: '1', label: '禁止并发' }].map(option => <label key={option.value} className="management-radio-option"><RadioGroupItem value={option.value} />{option.label}</label>)}</RadioGroup></FormControl><FormMessage /></FormItem>} /></div><DialogFooter><DialogClose asChild><Button type="button" variant="outline">取消</Button></DialogClose><Button type="submit" loading={saveMutation.isPending || detailLoading}>{jobForm.getValues('jobId') === undefined ? '创建任务' : '保存修改'}</Button></DialogFooter></form></Form>
        </DialogContent>
      </Dialog>
      <CronBuilderDialog open={cronOpen} value={jobForm.watch('cronExpression')} onOpenChange={setCronOpen} onConfirm={value => jobForm.setValue('cronExpression', value, { shouldDirty: true, shouldValidate: true })} />
    </section>
  )
}
