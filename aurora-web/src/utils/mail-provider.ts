import type { MailProvider } from '@/api/mail'

const mailProviderDomains: Record<MailProvider, string> = {
  QQ: 'qq.com',
  NETEASE_163: '163.com',
  NETEASE_126: '126.com',
  YEAH: 'yeah.net'
}

export function mailProviderDomain(provider: MailProvider): string {
  return mailProviderDomains[provider] || ''
}

export function mailAddressAccount(value: string): string {
  return value.trim().split('@')[0] || ''
}

export function buildMailAddress(account: string, provider: MailProvider): string {
  const normalized = mailAddressAccount(account).toLowerCase()
  const domain = mailProviderDomain(provider)
  return normalized && domain ? `${normalized}@${domain}` : normalized
}

export function replaceMailProviderDomain(
  value: string,
  previousProvider: MailProvider,
  provider: MailProvider
): string {
  const normalized = value.trim().toLowerCase()
  const previousDomain = mailProviderDomain(previousProvider)
  const domain = mailProviderDomain(provider)
  if (!normalized || !previousDomain || !domain || !normalized.endsWith(`@${previousDomain}`)) {
    return normalized
  }
  return `${normalized.slice(0, normalized.length - previousDomain.length)}${domain}`
}

export function isMailAddressForProvider(value: string, provider: MailProvider): boolean {
  const domain = mailProviderDomain(provider)
  const normalized = value.trim().toLowerCase()
  return Boolean(domain && normalized.endsWith(`@${domain}`) && normalized.length > domain.length + 1)
}
