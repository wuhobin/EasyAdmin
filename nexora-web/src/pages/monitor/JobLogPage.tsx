import { DeleteOutlined, ReloadOutlined, SearchOutlined, UndoOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import AntApp from 'antd/es/app'
import Table from 'antd/es/table'
import Tag from 'antd/es/tag'
import type { ColumnsType } from 'antd/es/table'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { cleanJobLogApi, deleteJobLogApi, getJobLogListApi, type JobLogQuery, type JobLogRecord } from '@/api/jobLog'
import { EmptyValue, ManagementCard, ManagementPagination, ManagementRowAction } from '@/components/management/ManagementUi'
import { Button } from '@/components/ui/button'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { useAuthStore } from '@/store/authStore'

interface JobLogFilterValues {
  jobName?: string
  jobGroup?: string
  status?: string
}

const initialQuery: JobLogQuery = { pageNum: 1, pageSize: 10 }

const jobGroupLabels: Record<string, string> = {
  DEFAULT: '默认',
  SYSTEM: '系统'
}

export function JobLogPage() {
  const { message, modal } = AntApp.useApp()
  const queryClient = useQueryClient()
  const permissions = useAuthStore(state => state.user.permissions)
  const [queryParams, setQueryParams] = useState<JobLogQuery>(initialQuery)
  const [selectedIds, setSelectedIds] = useState<number[]>([])
  const filterForm = useForm<JobLogFilterValues>({ defaultValues: { jobName: '', jobGroup: undefined, status: undefined } })
  const canDelete = permissions.includes('sys:jobLog:delete')
  const canClean = permissions.includes('sys:jobLog:clean')
  const logsQuery = useQuery({ queryKey: ['job-logs', queryParams], queryFn: async () => (await getJobLogListApi(queryParams)).data })

  const deleteMutation = useMutation({
    mutationFn: (ids: number | number[]) => deleteJobLogApi(ids),
    onSuccess: async (_, ids) => {
      await queryClient.invalidateQueries({ queryKey: ['job-logs'] })
      setSelectedIds([])
      message.success(Array.isArray(ids) ? '选中的调度日志已删除' : '调度日志已删除')
    },
    onError: () => message.error('调度日志删除失败')
  })
  const cleanMutation = useMutation({
    mutationFn: cleanJobLogApi,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['job-logs'] })
      setSelectedIds([])
      message.success('调度日志已清空')
    },
    onError: () => message.error('调度日志清空失败')
  })

  const confirmDelete = (record: JobLogRecord) => {
    modal.confirm({
      title: `删除“${record.jobName}”的这条调度日志？`,
      content: '删除后无法恢复。',
      okText: '删除',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: () => deleteMutation.mutateAsync(record.logId)
    })
  }

  const confirmBatchDelete = () => {
    modal.confirm({
      title: `删除选中的 ${selectedIds.length} 条调度日志？`,
      content: '此操作不可撤销。',
      okText: '批量删除',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: () => deleteMutation.mutateAsync(selectedIds)
    })
  }

  const confirmClean = () => {
    modal.confirm({
      title: '清空全部调度日志？',
      content: '全部历史调度日志将被永久删除，此操作不可撤销。',
      okText: '确认清空',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: () => cleanMutation.mutateAsync()
    })
  }

  const columns: ColumnsType<JobLogRecord> = [
    { title: 'ID', dataIndex: 'logId', width: 82, align: 'center' },
    { title: '任务名称', dataIndex: 'jobName', width: 170, ellipsis: true, render: value => <span className="management-primary-text">{value}</span> },
    { title: '任务组名', dataIndex: 'jobGroup', width: 112, align: 'center', render: value => <EmptyValue value={value ? jobGroupLabels[value] ?? value : undefined} /> },
    { title: '调用目标字符串', dataIndex: 'invokeTarget', width: 250, ellipsis: true, render: value => <EmptyValue value={value} /> },
    { title: '日志信息', dataIndex: 'jobMessage', width: 250, ellipsis: true, render: value => <EmptyValue value={value} /> },
    { title: '执行状态', dataIndex: 'status', width: 100, align: 'center', render: value => value === '0' ? <Tag color="green">成功</Tag> : value === '1' ? <Tag color="red">失败</Tag> : <EmptyValue value={value} /> },
    { title: '执行时间', dataIndex: 'startTime', width: 180, render: value => <EmptyValue value={value} /> },
    {
      title: '操作',
      key: 'action',
      width: 88,
      fixed: 'right',
      align: 'center',
      render: (_, record) => canDelete ? <div className="management-row-actions"><ManagementRowAction tone="delete" icon={<DeleteOutlined />} aria-label={`删除调度日志 ${record.jobName}`} loading={deleteMutation.isPending && deleteMutation.variables === record.logId} onClick={() => confirmDelete(record)} /></div> : null
    }
  ]

  const applyFilters = (values: JobLogFilterValues) => {
    setQueryParams(previous => ({
      ...previous,
      pageNum: 1,
      jobName: values.jobName?.trim() || undefined,
      jobGroup: values.jobGroup,
      status: values.status
    }))
  }

  const resetFilters = () => {
    filterForm.reset()
    setQueryParams(previous => ({ pageNum: 1, pageSize: previous.pageSize }))
  }

  return (
    <section className="management-page">
      <ManagementCard
        filters={<Form {...filterForm}><form className="management-filter-form" onSubmit={filterForm.handleSubmit(applyFilters)}>
          <FormField control={filterForm.control} name="jobName" render={({ field }) => <FormItem className="management-filter-field"><FormLabel>任务名称</FormLabel><FormControl><Input placeholder="请输入任务名称" {...field} /></FormControl><FormMessage /></FormItem>} />
          <FormField control={filterForm.control} name="jobGroup" render={({ field }) => <FormItem className="management-filter-field management-filter-field-select"><FormLabel>任务组名</FormLabel><Select value={field.value ?? 'all'} onValueChange={value => field.onChange(value === 'all' ? undefined : value)}><FormControl><SelectTrigger><SelectValue placeholder="全部任务组" /></SelectTrigger></FormControl><SelectContent><SelectItem value="all">全部任务组</SelectItem><SelectItem value="DEFAULT">默认</SelectItem><SelectItem value="SYSTEM">系统</SelectItem></SelectContent></Select><FormMessage /></FormItem>} />
          <FormField control={filterForm.control} name="status" render={({ field }) => <FormItem className="management-filter-field management-filter-field-select"><FormLabel>执行状态</FormLabel><Select value={field.value ?? 'all'} onValueChange={value => field.onChange(value === 'all' ? undefined : value)}><FormControl><SelectTrigger><SelectValue placeholder="全部状态" /></SelectTrigger></FormControl><SelectContent><SelectItem value="all">全部状态</SelectItem><SelectItem value="0">成功</SelectItem><SelectItem value="1">失败</SelectItem></SelectContent></Select><FormMessage /></FormItem>} />
          <div className="management-filter-actions"><Button type="submit"><SearchOutlined />搜索</Button><Button type="button" variant="outline" onClick={resetFilters}><UndoOutlined />重置</Button></div>
        </form></Form>}
        toolbar={<><div className="management-toolbar-selection">{selectedIds.length ? `已选择 ${selectedIds.length} 项` : '调度日志列表'}</div><div className="management-actions">{canDelete ? <Button type="button" variant="destructive" disabled={!selectedIds.length} onClick={confirmBatchDelete}><DeleteOutlined />批量删除</Button> : null}{canClean ? <Button type="button" variant="destructive" loading={cleanMutation.isPending} onClick={confirmClean}><DeleteOutlined />清空日志</Button> : null}<Button type="button" variant="outline" onClick={() => void logsQuery.refetch()}><ReloadOutlined />刷新</Button></div></>}
        pagination={<ManagementPagination current={queryParams.pageNum} pageSize={queryParams.pageSize} total={logsQuery.data?.total ?? 0} onChange={(pageNum, pageSize) => setQueryParams(previous => ({ ...previous, pageNum, pageSize }))} />}
      >
        <Table<JobLogRecord> rowKey="logId" loading={logsQuery.isLoading} columns={columns} dataSource={logsQuery.data?.records ?? []} pagination={false} scroll={{ x: 1230 }} rowSelection={canDelete ? {
          selectedRowKeys: selectedIds,
          preserveSelectedRowKeys: true,
          getCheckboxProps: record => ({ title: `选择调度日志 ${record.jobName}` }),
          onChange: keys => setSelectedIds(keys.map(Number))
        } : undefined} />
      </ManagementCard>
    </section>
  )
}
