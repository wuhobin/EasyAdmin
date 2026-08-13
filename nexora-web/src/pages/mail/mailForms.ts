import type { MailAccount, MailAccountPayload, MailProvider, MailProviderConfig } from '@/api/mail'

export interface MailAccountFormValues {
  id?: number
  accountName: string
  provider: MailProvider | ''
  email: string
  authCode: string
  enabled: number
  sort: number
}

export const emptyMailAccountForm: MailAccountFormValues = {
  accountName: '',
  provider: '',
  email: '',
  authCode: '',
  enabled: 1,
  sort: 0
}

export function mailAddressAccount(value: string) {
  return value.trim().split('@')[0] || ''
}

export function buildMailAddress(account: string, domain: string) {
  const normalized = mailAddressAccount(account).toLowerCase()
  return normalized && domain ? `${normalized}@${domain}` : normalized
}

export function replaceMailProviderDomain(value: string, previousDomain: string, domain: string) {
  const normalized = value.trim().toLowerCase()
  if (!normalized || !previousDomain || !domain || !normalized.endsWith(`@${previousDomain}`)) return normalized
  return `${normalized.slice(0, normalized.length - previousDomain.length)}${domain}`
}

export function isMailAddressForDomain(value: string, domain: string) {
  const normalized = value.trim().toLowerCase()
  return Boolean(domain && normalized.endsWith(`@${domain}`) && normalized.length > domain.length + 1)
}

export function defaultMailProvider(providers: MailProviderConfig[]): MailProvider | '' {
  return providers.find(provider => provider.defaultProvider)?.value || providers[0]?.value || ''
}

export function providerDomain(providers: MailProviderConfig[], provider: MailProvider | '') {
  return providers.find(item => item.value === provider)?.domain || ''
}

export function mailAccountToForm(account: MailAccount): MailAccountFormValues {
  return {
    id: account.id,
    accountName: account.accountName,
    provider: account.provider,
    email: account.email,
    authCode: '',
    enabled: account.enabled,
    sort: account.sort
  }
}

export function mailAccountFormToPayload(values: MailAccountFormValues): MailAccountPayload {
  return {
    ...(values.id === undefined ? {} : { id: values.id }),
    accountName: values.accountName.trim(),
    provider: values.provider as MailProvider,
    email: values.email.trim().toLowerCase(),
    authCode: values.authCode || undefined,
    enabled: values.enabled,
    sort: values.sort
  }
}

export function providerMark(provider: MailProvider) {
  if (provider === 'QQ') return 'Q'
  if (provider === 'NETEASE_163') return '163'
  if (provider === 'NETEASE_126') return '126'
  if (provider === 'YEAH') return 'Y'
  return 'G'
}

export function providerClass(provider: MailProvider) {
  return provider.toLowerCase().replace('_', '-')
}

export function providerLabel(providers: MailProviderConfig[], provider: MailProvider) {
  return providers.find(item => item.value === provider)?.label || provider
}
