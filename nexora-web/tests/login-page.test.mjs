import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const loginPagePath = new URL('../src/views/login/index.vue', import.meta.url)
const loginPageSource = await readFile(loginPagePath, 'utf8')
const indexHtmlPath = new URL('../index.html', import.meta.url)
const indexHtmlSource = await readFile(indexHtmlPath, 'utf8')
const logoPath = new URL('../src/layouts/components/Sidebar/Logo.vue', import.meta.url)
const logoSource = await readFile(logoPath, 'utf8')

test('login page keeps the email login form', () => {
  assert.match(loginPageSource, /v-model="loginForm\.email"/)
  assert.match(loginPageSource, /autocomplete="email"/)
  assert.match(loginPageSource, /v-model="loginForm\.password"/)
  assert.match(loginPageSource, /v-model="loginForm\.rememberMe"/)
  assert.match(loginPageSource, /@click="handleLogin"/)
})

test('authentication fields use placeholders without visible labels', () => {
  assert.doesNotMatch(loginPageSource, /<el-form-item\s+label=/)
  assert.match(loginPageSource, /v-model="loginForm\.email"[^>]*placeholder="请输入邮箱"[^>]*aria-label="邮箱"/)
  assert.match(loginPageSource, /v-model="loginForm\.password"[^>]*placeholder="请输入密码"[^>]*aria-label="密码"/)
  assert.match(loginPageSource, /v-model="registerForm\.code"[^>]*placeholder="请输入验证码"[^>]*aria-label="邮箱验证码"/)
})

test('login page submits the remember-me value from the form model', () => {
  assert.doesNotMatch(loginPageSource, /const rememberMe = ref\(/)
  assert.match(loginPageSource, /rememberMe:\s*false/)
})

test('login page exposes registration only when the public system setting is true', () => {
  assert.match(loginPageSource, /getConfigValueApi\(REGISTER_ENABLED_CONFIG_KEY\)/)
  assert.match(loginPageSource, /data === ['"]true['"]/)
  assert.match(loginPageSource, /v-if="registerEnabled \|\| !registerConfigLoaded"/)
  assert.match(loginPageSource, /activeMode === ['"]register['"]/)
})

test('registration config loading reserves the tab space to prevent layout shift', () => {
  assert.match(loginPageSource, /const registerConfigLoaded = ref\(false\)/)
  assert.match(loginPageSource, /registerConfigLoaded\.value = true/)
  assert.match(loginPageSource, /'is-loading': !registerConfigLoaded/)
  assert.match(loginPageSource, /\.auth-tabs\.is-loading\s*\{[^}]*visibility:\s*hidden/s)
})

test('registration uses email code and password with a sixty-second cooldown', () => {
  assert.match(loginPageSource, /v-model="registerForm\.email"/)
  assert.match(loginPageSource, /v-model="registerForm\.code"/)
  assert.match(loginPageSource, /v-model="registerForm\.password"/)
  assert.match(loginPageSource, /sendRegisterCodeApi\(registerForm\)/)
  assert.match(loginPageSource, /registerApi\(registerForm\)/)
  assert.match(loginPageSource, /codeCountdown\.value = 60/)
  assert.match(loginPageSource, /loginForm\.email = registerForm\.email/)
  assert.doesNotMatch(loginPageSource, /confirmPassword/)
})

test('login and registration navigation remains accessible and motion-safe', () => {
  assert.match(loginPageSource, /role="tablist"/)
  assert.match(loginPageSource, /aria-controls="login-panel"/)
  assert.match(loginPageSource, /aria-controls="register-panel"/)
  assert.match(loginPageSource, /:tabindex="activeMode === 'login' \? 0 : -1"/)
  assert.match(loginPageSource, /document\.getElementById\(`\$\{mode\}-tab`\)\?\.focus\(\)/)
  assert.match(loginPageSource, /aria-label="codeCountdown > 0/)
  assert.match(loginPageSource, /@media \(prefers-reduced-motion: reduce\)/)
  assert.match(loginPageSource, /min-height:\s*100dvh/)
})

test('login and registration use a stable overlapping form transition', () => {
  assert.match(loginPageSource, /class="auth-form-stage"/)
  assert.match(loginPageSource, /\.auth-form-stage\s*\{\s*position:\s*relative;\s*min-height:\s*300px;/)
  assert.match(loginPageSource, /\.auth-form-leave-active\s*\{[^}]*position:\s*absolute;/s)
  assert.match(loginPageSource, /\.auth-form-enter-active\s*\{[^}]*opacity \.18s/s)
  assert.doesNotMatch(loginPageSource, /\.auth-form-(?:enter-from|leave-to)\s*\{[^}]*transform:/s)
  assert.doesNotMatch(loginPageSource, /<transition name="auth-form" mode="out-in">/)
  assert.doesNotMatch(loginPageSource, /WELCOME BACK|CREATE ACCESS|欢迎回来|创建你的账号/)
  assert.doesNotMatch(loginPageSource, /class="access-route"/)
  assert.match(loginPageSource, /安全进入你的(?:<br \/>)?管理工作台/)
})

test('active authentication tab uses a sliding selection indicator', () => {
  assert.match(loginPageSource, /'is-register': activeMode === 'register'/)
  assert.match(loginPageSource, /\.auth-tabs::before\s*\{[^}]*transition:\s*transform \.24s/s)
  assert.match(loginPageSource, /\.auth-tabs\.is-register::before\s*\{[^}]*translateX/s)
})

test('login page does not prefill demo credentials', () => {
  assert.match(loginPageSource, /email:\s*['"]["']/)
  assert.match(loginPageSource, /password:\s*['"]["']/)
  assert.doesNotMatch(loginPageSource, /email:\s*['"]test['"]/)
  assert.doesNotMatch(loginPageSource, /password:\s*['"]123456['"]/)
})

test('browser autofill keeps the active login theme colors', () => {
  assert.match(loginPageSource, /\.el-input__inner:-webkit-autofill/)
  assert.match(loginPageSource, /-webkit-text-fill-color:\s*var\(--auth-text\)/)
  assert.match(loginPageSource, /box-shadow:\s*0 0 0 1000px var\(--auth-surface\) inset/)
})

test('input backgrounds switch themes without a delayed white flash', () => {
  const inputWrapperRule = loginPageSource.match(/:deep\(\.el-input__wrapper\)\s*\{([^}]*)\}/s)?.[1] ?? ''
  assert.match(inputWrapperRule, /transition:\s*border-color \.2s ease, box-shadow \.2s ease;/)
  assert.doesNotMatch(inputWrapperRule, /background-color/)
})

test('validation errors use reserved space without changing the form height', () => {
  assert.match(loginPageSource, /\.el-form-item\)\s*\{\s*margin-bottom:\s*28px;/)
  assert.doesNotMatch(loginPageSource, /\.el-form-item\.is-error\)/)
})

test('login page does not restore removed alternative login methods', () => {
  const removedFeatureMarkers = [
    'SliderCaptcha',
    '<slider-captcha',
    'loginType',
    'qrcode',
    'social-login',
    'handleSocialLogin'
  ]

  for (const marker of removedFeatureMarkers) {
    assert.doesNotMatch(loginPageSource, new RegExp(marker, 'i'))
  }
  assert.doesNotMatch(loginPageSource, /loginForm\.username/)
})

test('app uses the Nexora brand mark in the page and browser chrome', () => {
  assert.match(indexHtmlSource, /href="\/favicon\.svg"/)
  assert.doesNotMatch(indexHtmlSource, /vite\.svg/)
  assert.match(logoSource, /nexora-logo\.svg/)
})
