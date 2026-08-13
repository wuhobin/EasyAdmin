import { describe, expect, it } from 'vitest'
import { htmlContentByteLength, noticeFormToPayload, noticeRecordToForm } from '@/pages/system/noticeForm'

describe('notice form', () => {
  it('normalizes announcement recipients to all active users', () => {
    expect(noticeFormToPayload({ title: ' 系统维护 ', content: '今晚升级', contentFormat: 'text', noticeType: 2, targetType: 1, targetUserIds: [3, 3, 8] })).toEqual({
      title: '系统维护', content: '今晚升级', contentFormat: 'text', noticeType: 2, targetType: 3, targetUserIds: []
    })
  })

  it('keeps unique selected recipients for a notification', () => {
    expect(noticeFormToPayload({ id: 9, title: '提醒', content: '<b>内容</b>', contentFormat: 'html', noticeType: 1, targetType: 1, targetUserIds: [2, 2, 5] })).toEqual({
      id: 9, title: '提醒', content: '<b>内容</b>', contentFormat: 'html', noticeType: 1, targetType: 1, targetUserIds: [2, 5]
    })
  })

  it('restores drafts and measures HTML by UTF-8 bytes', () => {
    expect(noticeRecordToForm({ id: 7, title: '公告', content: '内容', contentFormat: 'text', noticeType: 2, targetType: 1, targetUserIds: [9] })).toMatchObject({ targetType: 3, targetUserIds: [] })
    expect(htmlContentByteLength('a中')).toBe(4)
  })
})
