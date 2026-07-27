import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

async function readSource(relativePath) {
  return readFile(new URL(`../${relativePath}`, import.meta.url), 'utf8')
}

const apiSource = await readSource('src/api/system/config.ts')
const pageSource = await readSource('src/views/system/config/index.vue')

test('configuration API exposes typed CRUD without batch deletion', () => {
  assert.match(apiSource, /export interface SysConfigRecord/)
  assert.match(apiSource, /configKey:\s*string/)
  assert.match(apiSource, /configValue:\s*string/)
  assert.match(apiSource, /url:\s*['"]\/sys\/config['"]/)
  assert.match(apiSource, /url:\s*`\/sys\/config\/delete\/\$\{id\}`/)
  assert.match(apiSource, /deleteConfigApi\(id:\s*number\)/)
  assert.doesNotMatch(apiSource, /number\[\]/)
})

test('configuration page enforces immutable keys and safe single-record deletion', () => {
  assert.match(pageSource, /v-model\.trim="configForm\.configKey"/)
  assert.match(pageSource, /:disabled="dialogType === 'edit'"/)
  assert.match(pageSource, /\^\[a-z\]\+\(\?:\[\._-\]\[a-z\]\+\)\*\$/)
  assert.match(pageSource, /maxlength="512"/)
  assert.match(pageSource, /show-word-limit/)
  assert.match(pageSource, /sys:config:add/)
  assert.match(pageSource, /sys:config:update/)
  assert.match(pageSource, /sys:config:delete/)
  assert.match(pageSource, /依赖它的业务可能使用默认值或执行失败/)
  assert.doesNotMatch(pageSource, /type="selection"|批量删除/)
})
