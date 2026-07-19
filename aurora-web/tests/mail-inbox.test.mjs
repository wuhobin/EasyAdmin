import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

async function readSource(relativePath) {
  return readFile(new URL(`../${relativePath}`, import.meta.url), 'utf8')
}

const apiSource = await readSource('src/api/mail/index.ts')
const pageSource = await readSource('src/views/mail/index.vue')
const accountPageSource = await readSource('src/views/mail/account/index.vue')

test('mail API reads messages on demand and downloads attachments without a history endpoint', () => {
  assert.match(apiSource, /getLatestMailsApi/)
  assert.match(apiSource, /url:\s*['"]\/mail\/inbox\/list['"]/)
  assert.match(apiSource, /getMailDetailApi/)
  assert.match(apiSource, /url:\s*['"]\/mail\/inbox\/detail['"]/)
  assert.match(apiSource, /downloadMailAttachmentApi/)
  assert.match(apiSource, /responseType:\s*['"]blob['"]/)
  assert.doesNotMatch(apiSource, /mail\/history|mail\/code/)
})

test('mail page auto refreshes, isolates HTML, and notifies for unseen UIDs', () => {
  assert.match(pageSource, /window\.setInterval\(\(\) => loadMessages\(true\), 15_000\)/)
  assert.match(pageSource, /ElNotification/)
  assert.match(pageSource, /accountId.*uidValidity.*uid/s)
  assert.match(pageSource, /sandbox=""/)
  assert.match(pageSource, /:srcdoc="mailDocument"/)
  assert.match(pageSource, /外部追踪图片已阻止/)
  assert.match(pageSource, /邮件实时来自邮箱服务器，系统不保存历史正文/)
})

test('mail account page exposes view, test, edit, enable state, and delete controls', () => {
  assert.match(accountPageSource, /邮箱列表/)
  assert.match(accountPageSource, /openDetail\(row\)/)
  assert.match(accountPageSource, /testConnection\(row\)/)
  assert.match(accountPageSource, /openEditDialog\(row\)/)
  assert.match(accountPageSource, /deleteAccount\(row\)/)
  assert.match(accountPageSource, /accountEnabled/)
  assert.match(accountPageSource, /mail:account:update/)
  assert.match(accountPageSource, /mail:account:delete/)
  assert.match(accountPageSource, /授权码已经加密保存/)
})
