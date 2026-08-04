import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

async function readSource(relativePath) {
  return readFile(new URL(`../${relativePath}`, import.meta.url), 'utf8')
}

async function readOptionalSource(relativePath) {
  try {
    return await readSource(relativePath)
  } catch (error) {
    if (error?.code === 'ENOENT') return ''
    throw error
  }
}

const authSource = await readSource('src/utils/auth.ts')
const authApiSource = await readSource('src/api/system/auth.ts')
const userStoreSource = await readSource('src/store/modules/user.ts')
const requestSource = await readSource('src/utils/request.ts')
const permissionSource = await readSource('src/plugins/permission.ts')
const permissionStoreSource = await readSource('src/store/modules/permission.ts')
const sidebarSource = await readSource('src/layouts/components/Sidebar/index.vue')
const routerSource = await readSource('src/router/index.ts')
const authSessionSource = await readOptionalSource('src/utils/auth-session.ts')
const authSessionPluginSource = await readOptionalSource('src/plugins/authSession.ts')
const mainSource = await readSource('src/main.ts')
const uploadSource = await readSource('src/components/Upload/Image.vue')
const profileSource = await readSource('src/views/system/user/profile/index.vue')
const userApiSource = await readSource('src/api/system/user.ts')
const userManagementSource = await readSource('src/views/system/user/index.vue')
const operationLogSource = await readSource('src/views/system/log/operation/index.vue')
const packageJson = JSON.parse(await readSource('package.json'))
const packageLock = JSON.parse(await readSource('package-lock.json'))
const gitignoreSource = await readSource('.gitignore')

test('token cookie supports session and three-day remember-me lifetimes', () => {
  assert.match(authSource, /REMEMBER_ME_DAYS\s*=\s*3/)
  assert.match(authSource, /setToken\(token:\s*string,\s*rememberMe\s*=\s*false\)/)
  assert.match(authSource, /sameSite:\s*['"]lax['"]/)
  assert.match(authSource, /secure:/)
  assert.match(authSource, /rememberMe\s*\?\s*REMEMBER_ME_DAYS\s*:\s*undefined/)
  assert.match(authSource, /Cookies\.remove\(TokenKey,\s*\{\s*path:\s*['"]\/['"]\s*\}\)/)
})

test('authentication API types match the shared login and registration form', () => {
  assert.match(authApiSource, /export interface AuthParams/)
  assert.match(authApiSource, /export interface CurrentUserResult/)
  assert.match(authApiSource, /export interface LoginResult extends CurrentUserResult/)
  assert.match(authApiSource, /rememberMe:\s*boolean/)
  assert.match(authApiSource, /email:\s*string/)
  assert.match(authApiSource, /password:\s*string/)
  assert.match(authApiSource, /code\?:\s*string/)
  const authParamsSource = authApiSource.match(
    /export interface AuthParams\s*\{[\s\S]*?\n\}/
  )?.[0] ?? ''
  assert.doesNotMatch(authParamsSource, /captchaId/)
  assert.match(authApiSource, /export type RegisterParams[\s\S]*captchaId\?:\s*string/)
  assert.doesNotMatch(authApiSource, /username:\s*string/)
  assert.match(authApiSource, /Promise<ApiResponse<LoginResult>>/)
  assert.match(authApiSource, /Promise<ApiResponse<CurrentUserResult>>/)
  assert.match(authApiSource, /request<LoginResult>/)
  assert.match(authApiSource, /request<CurrentUserResult>/)
  assert.match(authApiSource, /\/auth\/register\/sendCode/)
  assert.match(authApiSource, /\/auth\/register/)
  assert.doesNotMatch(authApiSource, /as unknown as Promise/)
  assert.doesNotMatch(authApiSource, /captchaCode|captchaKey/)
})

test('user store tracks initialization and centralizes local session cleanup', () => {
  assert.match(userStoreSource, /const initialized = ref\(false\)/)
  assert.match(userStoreSource, /setToken\(data\.token,\s*loginData\.rememberMe\)/)
  assert.match(userStoreSource, /initialized\.value = true/)
  assert.match(userStoreSource, /function clearSession\(\)/)
  assert.match(userStoreSource, /function forceLogout\(\)/)
  assert.match(userStoreSource, /resetRouter\(\)/)
  assert.match(userStoreSource, /initialized\.value = false/)
  assert.match(userStoreSource, /clearSession\(\)[\s\S]*location\.reload\(\)/)
})

test('request wrapper exposes business-response generics and preserves binary responses', () => {
  assert.match(requestSource, /export interface ApiResponse<T/)
  assert.match(requestSource, /function request<T\s*=\s*any>/)
  assert.match(requestSource, /Promise<ApiResponse<T>>/)
  assert.match(requestSource, /responseType:\s*['"]blob['"]\s*\|\s*['"]arraybuffer['"]/)
})

test('request interceptor delegates unauthorized handling without importing application state', () => {
  assert.match(requestSource, /notifyUnauthorized/)
  assert.doesNotMatch(requestSource, /useUserStore|ElMessageBox|window\.location/)
  assert.match(authSessionSource, /registerUnauthorizedHandler/)
  assert.match(authSessionSource, /let unauthorizedPromise:\s*Promise<void>\s*\|\s*null/)
  assert.match(authSessionPluginSource, /userStore\.forceLogout\(\)/)
  assert.match(authSessionPluginSource, /ElMessageBox\.alert/)
  assert.match(authSessionPluginSource, /router\.replace\(['"]\/login['"]\)/)
  assert.match(mainSource, /setupAuthSession\(\)/)
})

test('route initialization uses explicit user-store state', () => {
  assert.match(permissionSource, /if \(!userStore\.initialized\)/)
  assert.match(permissionSource, /let initializationPromise:\s*Promise<void>\s*\|\s*null/)
  assert.match(permissionSource, /let initializationToken:\s*string\s*\|\s*undefined/)
  assert.match(permissionSource, /initializationToken !== token/)
  assert.match(permissionSource, /userStore\.markInitialized\(\)/)
  assert.match(userStoreSource, /function markInitialized\(\)/)
  assert.doesNotMatch(permissionSource, /if \(!userStore\.user\.nickname\)/)
})

test('dynamic route conversion rejects records with null paths before Vue Router registration', () => {
  assert.match(permissionStoreSource, /typeof route\.path !== ["']string["']/)
  assert.match(permissionStoreSource, /Array\.isArray\(tmpRoute\.children\)/)
  assert.match(permissionStoreSource, /return;/)
})

test('top-level page menus keep the main layout and remain single sidebar items', () => {
  assert.match(permissionStoreSource, /wrapRootMenuWithLayout/)
  assert.match(permissionStoreSource, /component:\s*Layout[\s\S]*children:\s*\[pageRoute\]/)
  assert.match(permissionStoreSource, /singleMenu:\s*true/)
  assert.match(sidebarSource, /menuRoute\.meta\?\.singleMenu/)
  assert.match(sidebarSource, /path:\s*menuRoute\.path[\s\S]*children:\s*undefined/)
})

test('profile is a hidden static route available to every authenticated user', () => {
  assert.match(routerSource, /path:\s*["']\/system["'][\s\S]*path:\s*["']profile["']/)
  assert.match(routerSource, /component:\s*\(\)\s*=>\s*import\(["']@\/views\/system\/user\/profile\/index\.vue["']\)/)
  assert.match(routerSource, /name:\s*["']Profile["']/)
  assert.match(routerSource, /title:\s*["']个人中心["'][\s\S]*hidden:\s*true/)
})

test('profile redesign preserves account editing and avatar upload behavior', () => {
  assert.match(profileSource, /class="identity-pass"/)
  assert.match(profileSource, /class="settings-workspace"/)
  assert.match(profileSource, /class="settings-panel"/)
  assert.match(profileSource, /v-model="userForm\.nickname"/)
  assert.match(profileSource, /v-model="pwdForm\.oldPassword"/)
  assert.match(profileSource, /:http-request="handleAvatarUpload"/)
  assert.match(
    profileSource,
    /const handleAvatarUpload[\s\S]*?await updateUserProfileApi\([\s\S]*?await userStore\.getUserInfo\(\)/
  )
  assert.match(profileSource, /@click="submitUserForm"/)
  assert.match(profileSource, /@click="submitPwdForm"/)
  assert.match(profileSource, /@click="openEmailDialog"/)
  assert.match(profileSource, /sendChangeEmailCodeApi/)
  assert.match(profileSource, /changeEmailApi/)
  assert.match(profileSource, /codeCountdown\.value = 60/)
  assert.doesNotMatch(profileSource, /v-model="userForm\.email"/)
  assert.doesNotMatch(profileSource, /v-permission=["'][^"']*sys:user:update[^"']*["']/)
  assert.match(profileSource, /v-if="profileLoading"/)
  assert.match(profileSource, /v-else-if="profileLoadFailed"/)
  assert.match(profileSource, /@click="getUser\(\)"/)
  assert.match(profileSource, /autocomplete="current-password"/)
  assert.match(profileSource, /autocomplete="new-password"/)
  assert.match(profileSource, /:global\(html\.dark\) \.profile-page/)
  assert.match(profileSource, /@media \(prefers-reduced-motion: reduce\)/)
  assert.match(profileSource, /\.settings-workspace\s*\{[\s\S]*grid-template-columns:/)
})

test('user management and operation logs use email, nickname, and user id identities', () => {
  assert.match(userManagementSource, /queryParams\.email/)
  assert.match(userManagementSource, /prop="email"/)
  assert.match(userManagementSource, /:disabled="dialog\.type === 'edit'"/)
  assert.match(userManagementSource, /userForm\.id === 1/)
  assert.doesNotMatch(userManagementSource, /username/)
  assert.match(userApiSource, /export interface SysUserForm/)
  assert.doesNotMatch(userApiSource, /SysUserCreatePayload|SysUserUpdatePayload/)
  assert.match(userManagementSource, /createUserApi\(\{\s*nickname:/)
  assert.match(userManagementSource, /updateUserApi\(\{\s*id:/)
  assert.match(userManagementSource, /auditUserApi\(row\.id\)/)
  assert.match(userManagementSource, /row\.status === 2/)
  assert.match(userApiSource, /\/sys\/user\/profile\/email\/sendCode/)
  assert.match(userApiSource, /\/sys\/user\/profile\/changeEmail/)
  assert.match(operationLogSource, /queryParams\.userId/)
  assert.match(operationLogSource, /prop="userId"/)
  assert.doesNotMatch(operationLogSource, /username/)
})

test('route initialization only clears the session after an unauthorized error', () => {
  assert.match(requestSource, /isUnauthorized/)
  assert.match(permissionSource, /if \(isUnauthorizedError\(error\)\)/)
  assert.doesNotMatch(permissionSource, /userStore\.forceLogout\(\)/)
  assert.match(permissionSource, /if \(isUnauthorizedError\(error\)\) \{[^}]*next\(false\);/)
  assert.match(permissionSource, /else \{[\s\S]*isReportedRequestError\(error\)[\s\S]*ElMessage\.error\([\s\S]*next\(false\);/)
  assert.match(requestSource, /new RequestError\([^\n]+false, true\)/)
  assert.match(authSessionPluginSource, /userStore\.forceLogout\(\)/)
  assert.match(authSessionPluginSource, /router\.replace\(['"]\/login['"]\)/)
})

test('image uploads send the same Bearer authorization scheme as axios', () => {
  assert.match(uploadSource, /const token = getToken\(\)/)
  assert.match(uploadSource, /Authorization:\s*token\s*\?\s*`Bearer \$\{token\}`\s*:\s*['"]["']/)
})

test('unused Pinia persistence dependency is removed', () => {
  assert.equal(packageJson.dependencies?.['pinia-plugin-persistedstate'], undefined)
})

test('frontend dependencies use the coordinated Node 18 compatible versions', () => {
  assert.equal(packageJson.engines?.node, '>=18')
  assert.equal(packageJson.dependencies.vue, '^3.5.40')
  assert.equal(packageJson.dependencies['vue-router'], '^4.6.4')
  assert.equal(packageJson.dependencies.pinia, '^3.0.4')
  assert.equal(packageJson.dependencies['element-plus'], '^2.14.3')
  assert.equal(packageJson.dependencies.axios, '^1.18.1')
  assert.equal(packageJson.devDependencies.vite, '^6.4.3')
  assert.equal(packageJson.devDependencies['@vitejs/plugin-vue'], '^5.2.4')
  assert.equal(packageJson.devDependencies.typescript, '^5.9.3')
  assert.equal(packageJson.devDependencies['vue-tsc'], '^3.3.7')
  assert.equal(packageJson.devDependencies['unplugin-auto-import'], '^20.3.0')
  assert.equal(packageJson.devDependencies['@types/vue-router'], undefined)
  assert.equal(packageJson.devDependencies['@vue/runtime-core'], undefined)
  assert.equal(packageJson.dependencies['svg-sprite-loader'], undefined)
  assert.equal(packageJson.devDependencies['vite-svg-loader'], undefined)
})

test('package scripts expose repeatable test, typecheck, and full verification commands', () => {
  assert.equal(
    packageJson.scripts.test,
    'node --test tests/login-page.test.mjs tests/login-session.test.mjs tests/file-management.test.mjs tests/mail-inbox.test.mjs tests/configuration-management.test.mjs tests/online-users.test.mjs'
  )
  assert.equal(packageJson.scripts.typecheck, 'vue-tsc --noEmit')
  assert.equal(packageJson.scripts.check, 'npm run typecheck && npm run test && npm run build')
})

test('npm lockfile is generated and tracked for reproducible installs', () => {
  assert.equal(packageLock.name, packageJson.name)
  assert.ok(packageLock.lockfileVersion >= 3)
  assert.doesNotMatch(gitignoreSource, /^package-lock\.json$/m)
})
