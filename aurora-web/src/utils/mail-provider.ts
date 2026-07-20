export function mailAddressAccount(value: string): string {
  return value.trim().split('@')[0] || ''
}

export function buildMailAddress(account: string, domain: string): string {
  const normalized = mailAddressAccount(account).toLowerCase()
  return normalized && domain ? `${normalized}@${domain}` : normalized
}

export function replaceMailProviderDomain(
  value: string,
  previousDomain: string,
  domain: string
): string {
  const normalized = value.trim().toLowerCase()
  if (!normalized || !previousDomain || !domain || !normalized.endsWith(`@${previousDomain}`)) {
    return normalized
  }
  return `${normalized.slice(0, normalized.length - previousDomain.length)}${domain}`
}

export function isMailAddressForDomain(value: string, domain: string): boolean {
  const normalized = value.trim().toLowerCase()
  return Boolean(domain && normalized.endsWith(`@${domain}`) && normalized.length > domain.length + 1)
}
