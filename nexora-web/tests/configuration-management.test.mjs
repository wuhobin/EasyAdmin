import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

async function readSource(relativePath) {
  return readFile(new URL(`../${relativePath}`, import.meta.url), 'utf8')
}

const apiSource = await readSource('src/api/system/config.ts')
const pageSource = await readSource('src/views/system/config/index.vue')
const systemFormSource = await readSource('src/views/system/config/components/SystemConfigForm.vue')
const registerFormSource = await readSource('src/views/system/config/components/RegisterConfigForm.vue')
const loginFormSource = await readSource('src/views/system/config/components/LoginConfigForm.vue')
const passwordFormSource = await readSource('src/views/system/config/components/PasswordConfigForm.vue')
const publicStoreSource = await readSource('src/store/modules/publicConfig.ts')
const passwordPolicySource = await readSource('src/utils/password-policy.ts')
const userPageSource = await readSource('src/views/system/user/index.vue')

test('configuration API exposes fixed group endpoints without generic CRUD', () => {
  assert.match(apiSource, /type ConfigGroupCode = 'system' \| 'register' \| 'login' \| 'password'/)
  assert.match(apiSource, /interface ConfigValueByGroup/)
  assert.match(apiSource, /url:\s*'\/sys\/config-group\/list'/)
  assert.match(apiSource, /url:\s*'\/sys\/config-group\/public'/)
  assert.match(apiSource, /\/sys\/config-group\/\$\{groupCode\}/)
  assert.match(apiSource, /url:\s*'\/sys\/config-group\/refresh'/)
  assert.doesNotMatch(apiSource, /addConfigApi|deleteConfigApi|SysConfigRecord|configKey/)
})

test('configuration page uses four dedicated group forms and whole-group save', () => {
  assert.match(pageSource, /SystemConfigForm/)
  assert.match(pageSource, /RegisterConfigForm/)
  assert.match(pageSource, /LoginConfigForm/)
  assert.match(pageSource, /PasswordConfigForm/)
  assert.match(pageSource, /:label="tab\.groupName"/)
  assert.doesNotMatch(pageSource, /name: '系统配置'|name: '注册配置'|name: '登录配置'|name: '密码配置'/)
  assert.match(systemFormSource, /defineModel<SystemConfig>/)
  assert.match(registerFormSource, /defineModel<RegisterConfig>/)
  assert.match(loginFormSource, /defineModel<LoginConfig>/)
  assert.match(passwordFormSource, /defineModel<PasswordConfig>/)
  assert.match(systemFormSource, /v-model\.trim="model\.siteName"/)
  assert.match(registerFormSource, /v-model="model\.verifyEmail"/)
  assert.match(loginFormSource, /v-model="model\.maxRetryCount"/)
  assert.match(passwordFormSource, /v-model="model\.requireSpecial"/)
  assert.match(pageSource, /updateConfigGroupApi\('system', forms\.system\)/)
  assert.match(pageSource, /refreshConfigGroupCacheApi\(\)/)
  assert.match(pageSource, /sys:config:update/)
  assert.doesNotMatch(pageSource, /as never/)
  assert.doesNotMatch(pageSource, /sys:config:add|sys:config:delete|分页|搜索/)
})

test('public configuration drives branding, password policy, and pending-user audit', () => {
  assert.match(publicStoreSource, /getPublicConfigApi\(\)/)
  assert.match(publicStoreSource, /applyDocumentTitle/)
  assert.match(passwordPolicySource, /Array\.from\(password\)\.length/)
  assert.match(passwordPolicySource, /TextEncoder\(\).*72/s)
  assert.match(passwordPolicySource, /\\p\{Lu\}/)
  assert.match(passwordPolicySource, /\\p\{Ll\}/)
  assert.match(passwordPolicySource, /\\p\{Nd\}/)
  assert.match(passwordPolicySource, /requireUppercase/)
  assert.match(passwordPolicySource, /requireLowercase/)
  assert.match(passwordPolicySource, /requireNumber/)
  assert.match(passwordPolicySource, /requireSpecial/)
  assert.match(userPageSource, /row\.status === 2/)
  assert.match(userPageSource, /审核通过/)
  assert.match(userPageSource, /auditUserApi\(row\.id\)/)
})
