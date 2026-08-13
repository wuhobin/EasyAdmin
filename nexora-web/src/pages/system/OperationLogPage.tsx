import { DeleteOutlined, ReloadOutlined, SearchOutlined, UndoOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import AntApp from 'antd/es/app'
import Table from 'antd/es/table'
import Tag from 'antd/es/tag'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { deleteOperationLogApi, getOperationLogListApi, type OperationLogQuery, type OperationLogRecord } from '@/api/operationLog'
import { EmptyValue, ManagementCard, ManagementPagination, ManagementRowAction } from '@/components/management/ManagementUi'
import { Button } from '@/components/ui/button'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { useAuthStore } from '@/store/authStore'
import { formatDateTime } from '@/utils/format'
import { useUrlQueryState, type UrlQuerySchema } from '@/utils/urlQueryState'

interface OperationLogFilterValues {
  userId?: number
}

const initialQuery: OperationLogQuery = { pageNum: 1, pageSize: 10 }
const querySchema: UrlQuerySchema<OperationLogQuery> = { pageNum: 'number', pageSize: 'number', userId: 'number' }

const methodColors: Record<string, string> = {
  GET: 'blue',
  POST: 'green',
  PUT: 'gold',
  DELETE: 'red',
  PATCH: 'purple'
}

function formatRequestParams(value?: string) {
  if (!value) return '-'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

export function OperationLogPage() {
  const { message, modal } = AntApp.useApp()
  const queryClient = useQueryClient()
  const permissions = useAuthStore(state => state.user.permissions)
  const [queryParams, setQueryParams] = useUrlQueryState(initialQuery, querySchema)
  const [selectedIds, setSelectedIds] = useState<number[]>([])
  const filterForm = useForm<OperationLogFilterValues>({ defaultValues: { userId: undefined } })
  useEffect(() => filterForm.reset({ userId: queryParams.userId }), [filterForm, queryParams.userId])
  const canDelete = permissions.includes('sys:operateLog:delete')
  const logsQuery = useQuery({ queryKey: ['operation-logs', queryParams], queryFn: async () => (await getOperationLogListApi(queryParams)).data })

  const deleteMutation = useMutation({
    mutationFn: (ids: number | number[]) => deleteOperationLogApi(ids),
    onSuccess: async (_, ids) => {
      await queryClient.invalidateQueries({ queryKey: ['operation-logs'] })
      setSelectedIds([])
      message.success(Array.isArray(ids) ? '选中的操作日志已删除' : '操作日志已删除')
    },
    onError: () => message.error('操作日志删除失败')
  })

  const confirmDelete = (record: OperationLogRecord) => {
    modal.confirm({
      title: `删除用户 ID ${record.userId} 的这条操作日志？`,
      content: '删除后无法恢复。',
      okText: '删除',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: () => deleteMutation.mutateAsync(record.id)
    })
  }

  const confirmBatchDelete = () => {
    modal.confirm({
      title: `删除选中的 ${selectedIds.length} 条操作日志？`,
      content: '此操作不可撤销。',
      okText: '批量删除',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: () => deleteMutation.mutateAsync(selectedIds)
    })
  }

  const columns: ColumnsType<OperationLogRecord> = [
    { title: 'ID', dataIndex: 'id', width: 82, align: 'center' },
    { title: '用户 ID', dataIndex: 'userId', width: 100, align: 'center' },
    { title: '请求接口', dataIndex: 'requestUrl', width: 220, ellipsis: true, render: value => <EmptyValue value={value} /> },
    { title: '请求方式', dataIndex: 'type', width: 100, align: 'center', render: value => value ? <Tag color={methodColors[String(value).toUpperCase()]}>{String(value).toUpperCase()}</Tag> : <EmptyValue /> },
    { title: '接口名', dataIndex: 'operationName', width: 150, ellipsis: true, render: value => <EmptyValue value={value} /> },
    { title: 'IP', dataIndex: 'ip', width: 130, render: value => <EmptyValue value={value} /> },
    { title: 'IP 来源', dataIndex: 'source', width: 180, ellipsis: true, render: value => <EmptyValue value={value} /> },
    { title: '请求耗时', dataIndex: 'spendTime', width: 112, align: 'center', render: value => value === undefined || value === null ? <EmptyValue /> : <Tag>{value} ms</Tag> },
    { title: '创建时间', dataIndex: 'createTime', width: 180, render: value => <EmptyValue value={formatDateTime(value)} /> },
    {
      title: '操作',
      key: 'action',
      width: 88,
      fixed: 'right',
      align: 'center',
      render: (_, record) => canDelete ? <div className="management-row-actions"><ManagementRowAction tone="delete" icon={<DeleteOutlined />} aria-label={`删除操作日志 ${record.id}`} loading={deleteMutation.isPending && deleteMutation.variables === record.id} onClick={() => confirmDelete(record)} /></div> : null
    }
  ]

  const applyFilters = (values: OperationLogFilterValues) => {
    setQueryParams(previous => ({ ...previous, pageNum: 1, userId: values.userId }))
  }

  const resetFilters = () => {
    filterForm.reset()
    setQueryParams(previous => ({ pageNum: 1, pageSize: previous.pageSize }))
  }

  return (
    <section className="management-page">
      <ManagementCard
        filters={<Form {...filterForm}><form className="management-filter-form" onSubmit={filterForm.handleSubmit(applyFilters)}>
          <FormField control={filterForm.control} name="userId" render={({ field }) => <FormItem className="management-filter-field"><FormLabel>用户 ID</FormLabel><FormControl><Input type="number" min={1} step={1} placeholder="请输入用户 ID" value={field.value ?? ''} onBlur={field.onBlur} onChange={event => field.onChange(event.target.value ? event.target.valueAsNumber : undefined)} /></FormControl><FormMessage /></FormItem>} />
          <div className="management-filter-actions"><Button type="submit"><SearchOutlined />搜索</Button><Button type="button" variant="outline" onClick={resetFilters}><UndoOutlined />重置</Button></div>
        </form></Form>}
        toolbar={<><div className="management-toolbar-selection">{selectedIds.length ? `已选择 ${selectedIds.length} 项` : '操作日志列表'}</div><div className="management-actions">{canDelete ? <Button type="button" variant="destructive" disabled={!selectedIds.length} onClick={confirmBatchDelete}><DeleteOutlined />批量删除</Button> : null}<Button type="button" variant="outline" onClick={() => void logsQuery.refetch()}><ReloadOutlined />刷新</Button></div></>}
        pagination={<ManagementPagination current={queryParams.pageNum} pageSize={queryParams.pageSize} total={logsQuery.data?.total ?? 0} onChange={(pageNum, pageSize) => setQueryParams(previous => ({ ...previous, pageNum, pageSize }))} />}
      >
        <Table<OperationLogRecord>
          rowKey="id"
          loading={logsQuery.isLoading}
          columns={columns}
          dataSource={logsQuery.data?.records ?? []}
          pagination={false}
          scroll={{ x: 1350 }}
          rowSelection={canDelete ? {
            selectedRowKeys: selectedIds,
            preserveSelectedRowKeys: true,
            getCheckboxProps: record => ({ title: `选择操作日志 ${record.id}` }),
            onChange: keys => setSelectedIds(keys.map(Number))
          } : undefined}
          expandable={{
            expandedRowRender: record => <div className="management-log-detail"><div className="management-log-detail-row"><span>请求接口</span><code>{`${record.classPath ?? ''}${record.requestUrl ?? ''}` || '-'}</code></div><div className="management-log-detail-row"><span>请求参数</span><pre>{formatRequestParams(record.paramsJson)}</pre></div></div>,
            columnWidth: 48
          }}
        />
      </ManagementCard>
    </section>
  )
}
