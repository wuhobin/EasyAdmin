import request from '@/api/client'
import type { PageResult } from '@/types/api'

export type NoticeContentFormat = 'text' | 'html'
export type NoticeType = 1 | 2
export type NoticeTargetType = 1 | 3
export type NoticeStatus = 0 | 1

export interface NoticeRecord {
  id: number
  noticeId?: number
  title: string
  content?: string
  contentPreview?: string
  contentFormat: NoticeContentFormat
  noticeType: NoticeType
  targetType?: NoticeTargetType
  targetUserIds?: number[]
  status?: NoticeStatus
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

export interface NoticeQuery {
  pageNum: number
  pageSize: number
  title?: string
  noticeType?: NoticeType
  status?: NoticeStatus
}

export interface NoticePayload {
  id?: number
  title: string
  content: string
  contentFormat: NoticeContentFormat
  noticeType: NoticeType
  targetType: NoticeTargetType
  targetUserIds: number[]
}

export interface MyNoticeQuery {
  unreadOnly: boolean
  pageNum: number
  pageSize: number
}

export interface NotificationTicket {
  ticket: string
  expiresAt: string
}

export const getNoticeListApi = (params: NoticeQuery) =>
  request<PageResult<NoticeRecord>>({ url: '/sys/notice', method: 'get', params })

export const getNoticeDetailApi = (id: number) =>
  request<NoticeRecord>({ url: `/sys/notice/${id}`, method: 'get' })

export const addNoticeApi = (data: NoticePayload) =>
  request<number>({ url: '/sys/notice/add', method: 'post', data })

export const updateNoticeApi = (data: NoticePayload) =>
  request<void>({ url: '/sys/notice/update', method: 'put', data })

export const deleteNoticeApi = (id: number) =>
  request<void>({ url: `/sys/notice/delete/${id}`, method: 'delete' })

export const publishNoticeApi = (id: number) =>
  request<void>({ url: `/sys/notice/${id}/publish`, method: 'post' })

export const getMyNoticeListApi = (params: MyNoticeQuery) =>
  request<PageResult<NoticeRecord>>({ url: '/sys/notice/my', method: 'get', params })

export const getMyNoticeDetailApi = (id: number) =>
  request<NoticeRecord>({ url: `/sys/notice/my/${id}`, method: 'get' })

export const getUnreadNoticeCountApi = () =>
  request<{ unreadCount: number }>({ url: '/sys/notice/my/unread-count', method: 'get' })

export const markNoticeReadApi = (id: number) =>
  request<void>({ url: `/sys/notice/my/${id}/read`, method: 'post' })

export const markAllNoticeReadApi = () =>
  request<void>({ url: '/sys/notice/my/read-all', method: 'post' })

export const getPendingAnnouncementsApi = () =>
  request<NoticeRecord[]>({ url: '/sys/notice/my/announcements/pending', method: 'get' })

export const acknowledgeAnnouncementsApi = (ids: number[]) =>
  request<void>({ url: '/sys/notice/my/announcements/read', method: 'post', data: ids })

export const issueNotificationWsTicketApi = () =>
  request<NotificationTicket>({ url: '/sys/notice/ws-ticket', method: 'post' })
