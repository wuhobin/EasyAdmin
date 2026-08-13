import Pagination from 'antd/es/pagination'
import Tag from 'antd/es/tag'
import type { ReactNode } from 'react'
import { Button, type ButtonProps } from '@/components/ui/button'

type ActionTone = 'add' | 'edit' | 'delete' | 'approve' | 'settings' | 'data'

export function ManagementRowAction({ tone, className = '', icon, children, ...props }: ButtonProps & { tone: ActionTone; icon?: ReactNode }) {
  return <Button type="button" variant="ghost" size="icon" className={`management-row-action management-row-action-${tone} ${className}`.trim()} {...props}>{icon}{children}</Button>
}

export function ManagementCard({ filters, toolbar, children, pagination }: { filters?: ReactNode; toolbar: ReactNode; children: ReactNode; pagination?: ReactNode }) {
  return (
    <div className="management-data-card">
      {filters ? <div className="management-filters">{filters}</div> : null}
      <div className="management-toolbar">{toolbar}</div>
      <div className="management-table-wrap">{children}</div>
      {pagination ? <div className="management-pagination">{pagination}</div> : null}
    </div>
  )
}

export function ManagementPagination({ current, pageSize, total, pageSizeOptions = [10, 20, 30, 50], onChange }: { current: number; pageSize: number; total: number; pageSizeOptions?: number[]; onChange: (page: number, pageSize: number) => void }) {
  return <Pagination current={current} pageSize={pageSize} total={total} showSizeChanger pageSizeOptions={pageSizeOptions} showTotal={value => `共 ${value} 条`} onChange={onChange} />
}

export function StatusTag({ status, pending = false }: { status: number; pending?: boolean }) {
  if (pending && status === 2) return <Tag color="gold">待审核</Tag>
  return status === 1 ? <Tag color="green">启用</Tag> : <Tag>禁用</Tag>
}

export function EmptyValue({ value }: { value?: ReactNode }) {
  return value === undefined || value === null || value === '' ? <span className="management-empty-value">-</span> : <>{value}</>
}
