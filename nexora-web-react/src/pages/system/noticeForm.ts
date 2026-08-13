import type { NoticePayload, NoticeRecord } from '@/api/notice'

export interface NoticeFormValues {
  id?: number
  title: string
  content: string
  contentFormat: 'text' | 'html'
  noticeType: 1 | 2
  targetType: 1 | 3
  targetUserIds: number[]
}

export const emptyNoticeForm: NoticeFormValues = {
  title: '',
  content: '',
  contentFormat: 'text',
  noticeType: 1,
  targetType: 3,
  targetUserIds: []
}

export function noticeRecordToForm(record: NoticeRecord): NoticeFormValues {
  return {
    id: record.id,
    title: record.title,
    content: record.content || '',
    contentFormat: record.contentFormat,
    noticeType: record.noticeType,
    targetType: record.noticeType === 2 ? 3 : record.targetType ?? 3,
    targetUserIds: record.noticeType === 2 ? [] : record.targetUserIds ?? []
  }
}

export function noticeFormToPayload(values: NoticeFormValues): NoticePayload {
  const targetType = values.noticeType === 2 ? 3 : values.targetType
  return {
    ...(values.id === undefined ? {} : { id: values.id }),
    title: values.title.trim(),
    content: values.content,
    contentFormat: values.contentFormat,
    noticeType: values.noticeType,
    targetType,
    targetUserIds: targetType === 1 ? [...new Set(values.targetUserIds)] : []
  }
}

export function htmlContentByteLength(content: string) {
  return new TextEncoder().encode(content).length
}
