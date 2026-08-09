import request from '@/utils/request'

export interface NoticeItem {
  id: number
  noticeId?: number
  title: string
  content?: string
  contentPreview?: string
  contentFormat: 'text' | 'html'
  noticeType: 1 | 2
  targetType?: 1 | 3
  targetUserIds?: number[]
  status?: 0 | 1
  createBy?: number
  createName?: string
  createTime?: string
  publishTime?: string
  updateTime?: string
  isRead?: number
  recipientCount?: number
  readCount?: number
  unreadCount?: number
}

export interface NoticeForm {
  id?: number
  title: string
  content: string
  contentFormat: 'text' | 'html'
  noticeType: 1 | 2
  targetType: 1 | 3
  targetUserIds: number[]
}

export function getNoticeListApi(params: Record<string, unknown>) {
  return request<{ records: NoticeItem[]; total: number }>({
    url: '/sys/notice', method: 'get', params
  })
}

export function getNoticeDetailApi(id: number) {
  return request<NoticeItem>({ url: `/sys/notice/${id}`, method: 'get' })
}

export function addNoticeApi(data: NoticeForm) {
  return request<number>({ url: '/sys/notice/add', method: 'post', data })
}

export function updateNoticeApi(data: NoticeForm) {
  return request({ url: '/sys/notice/update', method: 'put', data })
}

export function deleteNoticeApi(id: number) {
  return request({ url: `/sys/notice/delete/${id}`, method: 'delete' })
}

export function publishNoticeApi(id: number) {
  return request({ url: `/sys/notice/${id}/publish`, method: 'post' })
}

export function getMyNoticeListApi(params: { unreadOnly: boolean; pageNum: number; pageSize: number }) {
  return request<{ records: NoticeItem[]; total: number }>({ url: '/sys/notice/my', method: 'get', params })
}

export function getMyNoticeDetailApi(id: number) {
  return request<NoticeItem>({ url: `/sys/notice/my/${id}`, method: 'get' })
}

export function getUnreadNoticeCountApi() {
  return request<{ unreadCount: number }>({ url: '/sys/notice/my/unread-count', method: 'get' })
}

export function markNoticeReadApi(id: number) {
  return request({ url: `/sys/notice/my/${id}/read`, method: 'post' })
}

export function markAllNoticeReadApi() {
  return request({ url: '/sys/notice/my/read-all', method: 'post' })
}

export function getPendingAnnouncementsApi() {
  return request<NoticeItem[]>({ url: '/sys/notice/my/announcements/pending', method: 'get' })
}

export function acknowledgeAnnouncementsApi(ids: number[]) {
  return request({ url: '/sys/notice/my/announcements/read', method: 'post', data: ids })
}

export function issueNotificationWsTicketApi() {
  return request<{ ticket: string; expiresAt: string }>({ url: '/sys/notice/ws-ticket', method: 'post' })
}
