import request from '@/utils/request'

export type MailProvider = 'QQ' | 'NETEASE_163' | 'NETEASE_126' | 'YEAH'

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

export interface MailAccountForm {
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

export function getMailAccountsApi() {
  return request<MailAccount[]>({ url: '/mail/account/list', method: 'get' })
}

export function getMailProvidersApi() {
  return request<MailProviderConfig[]>({ url: '/mail/account/providers', method: 'get' })
}

export function addMailAccountApi(data: MailAccountForm) {
  return request<MailAccount>({ url: '/mail/account', method: 'post', data })
}

export function updateMailAccountApi(data: MailAccountForm) {
  return request<void>({ url: '/mail/account', method: 'put', data })
}

export function deleteMailAccountApi(id: number) {
  return request<void>({ url: `/mail/account/${id}`, method: 'delete' })
}

export function testMailAccountApi(id: number) {
  return request<void>({ url: `/mail/account/${id}/test`, method: 'post', timeout: 30000 })
}

export function getLatestMailsApi(accountId?: number, limit = 30, cursor?: string, signal?: AbortSignal) {
  return request<MailMessagePage>({
    url: '/mail/inbox/list',
    method: 'get',
    params: { accountId, limit, cursor },
    signal,
    timeout: 30000
  })
}

export function getMailDetailApi(message: MailMessageSummary, signal?: AbortSignal) {
  return request<MailMessageDetail>({
    url: '/mail/inbox/detail',
    method: 'get',
    params: {
      accountId: message.accountId,
      uid: message.uid,
      uidValidity: message.uidValidity
    },
    signal,
    timeout: 30000
  })
}

export function downloadMailAttachmentApi(message: MailMessageDetail, partId: string) {
  return request({
    url: '/mail/inbox/attachment',
    method: 'get',
    params: {
      accountId: message.accountId,
      uid: message.uid,
      uidValidity: message.uidValidity,
      partId
    },
    responseType: 'blob',
    timeout: 0
  })
}
