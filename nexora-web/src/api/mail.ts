import request from '@/api/client'

export type MailProvider = 'QQ' | 'NETEASE_163' | 'NETEASE_126' | 'YEAH' | 'GMAIL'

export interface MailProviderConfig {
  label: string
  value: MailProvider
  domain: string
  imapHost: string
  imapPort: number
  defaultProvider: boolean
}

export interface MailAccount {
  id: number
  accountName: string
  provider: MailProvider
  email: string
  enabled: number
  sort: number
  lastConnectTime?: string
  lastError?: string
}

export interface MailAccountPayload {
  id?: number
  accountName: string
  provider: MailProvider
  email: string
  authCode?: string
  enabled: number
  sort: number
}

export interface MailMessageSummary {
  accountId: number
  accountName: string
  provider: MailProvider
  uid: number
  uidValidity: number
  fromName: string
  fromAddress: string
  subject: string
  receivedTime?: string
  read: boolean
  hasAttachment: boolean
}

export interface MailAttachment {
  partId: string
  fileName: string
  contentType: string
  size: number
}

export interface MailMessageDetail {
  accountId: number
  uid: number
  uidValidity: number
  fromName: string
  fromAddress: string
  recipients: string[]
  subject: string
  receivedTime?: string
  bodyHtml?: string
  bodyText?: string
  attachments: MailAttachment[]
}

export interface MailMessagePage {
  items: MailMessageSummary[]
  nextCursor?: string
  hasMore: boolean
}

export const getMailAccountsApi = () =>
  request<MailAccount[]>({ url: '/mail/account/list', method: 'get' })

export const getMailProvidersApi = () =>
  request<MailProviderConfig[]>({ url: '/mail/account/providers', method: 'get' })

export const addMailAccountApi = (data: MailAccountPayload) =>
  request<MailAccount>({ url: '/mail/account', method: 'post', data })

export const updateMailAccountApi = (data: MailAccountPayload) =>
  request<void>({ url: '/mail/account', method: 'put', data })

export const deleteMailAccountApi = (id: number) =>
  request<void>({ url: `/mail/account/${id}`, method: 'delete' })

export const testMailAccountApi = (id: number) =>
  request<void>({ url: `/mail/account/${id}/test`, method: 'post', timeout: 30000 })

export const getLatestMailsApi = (accountId?: number, limit = 30, cursor?: string, signal?: AbortSignal, refresh = false) =>
  request<MailMessagePage>({
    url: '/mail/inbox/list',
    method: 'get',
    params: { accountId, limit, cursor, refresh },
    signal,
    timeout: 30000
  })

export const getMailDetailApi = (message: MailMessageSummary, signal?: AbortSignal) =>
  request<MailMessageDetail>({
    url: '/mail/inbox/detail',
    method: 'get',
    params: { accountId: message.accountId, uid: message.uid, uidValidity: message.uidValidity },
    signal,
    timeout: 30000
  })

export const openMailApi = (message: MailMessageSummary, signal?: AbortSignal) =>
  request<MailMessageDetail>({
    url: '/mail/inbox/open',
    method: 'post',
    params: { accountId: message.accountId, uid: message.uid, uidValidity: message.uidValidity },
    signal,
    timeout: 30000
  })

export const markMailReadApi = (message: MailMessageSummary, signal?: AbortSignal) =>
  request<void>({
    url: '/mail/inbox/read',
    method: 'post',
    params: { accountId: message.accountId, uid: message.uid, uidValidity: message.uidValidity },
    signal,
    timeout: 30000
  })

export const downloadMailAttachmentApi = (message: MailMessageDetail, partId: string) =>
  request<Blob>({
    url: '/mail/inbox/attachment',
    method: 'get',
    params: { accountId: message.accountId, uid: message.uid, uidValidity: message.uidValidity, partId },
    responseType: 'blob',
    timeout: 0
  }) as unknown as Promise<Blob>
