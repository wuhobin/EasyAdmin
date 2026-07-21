import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

async function readSource(relativePath) {
  return readFile(new URL(`../${relativePath}`, import.meta.url), 'utf8')
}

const apiSource = await readSource('src/api/mail/index.ts')
const pageSource = await readSource('src/views/mail/index.vue')
const accountPageSource = await readSource('src/views/mail/account/index.vue')
const providerOptionsSource = await readSource('src/composables/useMailProviderOptions.ts')
const providerValidationSource = await readSource('src/utils/mail-provider.ts')
const accountDialogSource = await readSource('src/components/MailAccountDialog/index.vue')

test('mail API reads messages on demand and downloads attachments without a history endpoint', () => {
  assert.match(apiSource, /getLatestMailsApi/)
  assert.match(apiSource, /url:\s*['"]\/mail\/inbox\/list['"]/)
  assert.match(apiSource, /getMailDetailApi/)
  assert.match(apiSource, /url:\s*['"]\/mail\/inbox\/detail['"]/)
  assert.match(apiSource, /downloadMailAttachmentApi/)
  assert.match(apiSource, /responseType:\s*['"]blob['"]/)
  assert.match(apiSource, /MailMessagePage/)
  assert.match(apiSource, /cursor/)
  assert.match(apiSource, /signal/)
  assert.doesNotMatch(apiSource, /mail\/history|mail\/code/)
})

test('mail page refreshes without overlap, isolates HTML, and cancels stale requests', () => {
  assert.match(pageSource, /window\.setTimeout/)
  assert.match(pageSource, /AbortController/)
  assert.match(pageSource, /visibilitychange/)
  assert.match(pageSource, /listRequestId/)
  assert.match(pageSource, /detailRequestId/)
  assert.match(pageSource, /loadMore/)
  assert.match(pageSource, /ElNotification/)
  assert.match(pageSource, /accountId.*uidValidity.*uid/s)
  assert.match(pageSource, /sandbox="allow-popups allow-popups-to-escape-sandbox"/)
  assert.match(pageSource, /:srcdoc="mailDocument"/)
  assert.doesNotMatch(pageSource, /外部追踪图片已阻止/)
  assert.match(pageSource, /邮件实时来自邮箱服务器，系统不保存历史正文/)
})

test('mail message list is constrained to an independently scrollable grid row', () => {
  assert.match(pageSource, /grid-template-rows:\s*minmax\(0,\s*1fr\)/)
  assert.match(pageSource, /\.account-panel,\s*\.message-panel\s*\{[^}]*min-height:\s*0[^}]*overflow:\s*hidden/s)
  assert.match(pageSource, /\.message-list\s*\{[^}]*min-height:\s*0[^}]*overflow-y:\s*auto/s)
})

test('mail account page exposes management controls and shares the account dialog', () => {
  assert.match(accountPageSource, /邮箱列表/)
  assert.match(accountPageSource, /openDetail\(row\)/)
  assert.match(accountPageSource, /testConnection\(row\)/)
  assert.match(accountPageSource, /openEditDialog\(row\)/)
  assert.match(accountPageSource, /deleteAccount\(row\)/)
  assert.match(accountPageSource, /MailAccountDialog/)
  assert.match(pageSource, /MailAccountDialog/)
  assert.match(accountPageSource, /mail:account:update/)
  assert.match(accountPageSource, /mail:account:delete/)
  assert.match(accountPageSource, /授权码已经加密保存/)
})

test('mail provider options and domains come from the mail backend', () => {
  assert.match(providerOptionsSource, /getMailProvidersApi/)
  assert.match(apiSource, /url:\s*['"]\/mail\/account\/providers['"]/)
  assert.match(pageSource, /useMailProviderOptions/)
  assert.match(accountPageSource, /useMailProviderOptions/)
  assert.doesNotMatch(pageSource, /const providerOptions[^=]*=\s*\[/)
  assert.doesNotMatch(accountPageSource, /const providerOptions[^=]*=\s*\[/)
})

test('shared mail account dialog validates against the backend provider domain', () => {
  assert.doesNotMatch(providerValidationSource, /qq\.com|163\.com|126\.com|yeah\.net/)
  assert.match(providerValidationSource, /buildMailAddress/)
  assert.match(providerValidationSource, /mailAddressAccount/)
  assert.match(providerValidationSource, /replaceMailProviderDomain/)
  assert.match(providerValidationSource, /isMailAddressForDomain/)
  assert.match(accountDialogSource, /email-domain-suffix/)
  assert.match(accountDialogSource, /domainOf/)
  assert.match(accountDialogSource, /validateField\(['"]email['"]\)/)
})
