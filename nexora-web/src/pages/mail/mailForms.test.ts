import { describe, expect, it } from 'vitest'
import { buildMailAddress, isMailAddressForDomain, mailAccountFormToPayload, mailAddressAccount, replaceMailProviderDomain } from '@/pages/mail/mailForms'

describe('mail account form helpers', () => {
  it('builds a normalized provider address from the local account', () => {
    expect(mailAddressAccount(' Admin@QQ.com ')).toBe('Admin')
    expect(buildMailAddress(' Admin@old.com ', 'qq.com')).toBe('admin@qq.com')
    expect(isMailAddressForDomain('admin@qq.com', 'qq.com')).toBe(true)
  })

  it('only replaces a known previous provider domain', () => {
    expect(replaceMailProviderDomain('admin@qq.com', 'qq.com', '163.com')).toBe('admin@163.com')
    expect(replaceMailProviderDomain('admin@example.com', 'qq.com', '163.com')).toBe('admin@example.com')
  })

  it('trims account payloads and omits an unchanged authorization code', () => {
    expect(mailAccountFormToPayload({ id: 7, accountName: ' 工作邮箱 ', provider: 'QQ', email: ' Admin@QQ.com ', authCode: '', enabled: 1, sort: 3 })).toEqual({
      id: 7,
      accountName: '工作邮箱',
      provider: 'QQ',
      email: 'admin@qq.com',
      authCode: undefined,
      enabled: 1,
      sort: 3
    })
  })
})
