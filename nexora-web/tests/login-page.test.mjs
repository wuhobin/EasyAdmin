import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const loginPagePath = new URL('../src/views/login/index.vue', import.meta.url)
const loginPageSource = await readFile(loginPagePath, 'utf8')
const indexHtmlPath = new URL('../index.html', import.meta.url)
const indexHtmlSource = await readFile(indexHtmlPath, 'utf8')
const logoPath = new URL('../src/layouts/components/Sidebar/Logo.vue', import.meta.url)
const logoSource = await readFile(logoPath, 'utf8')

test('login page keeps the account login form', () => {
  assert.match(loginPageSource, /v-model="loginForm\.username"/)
  assert.match(loginPageSource, /v-model="loginForm\.password"/)
  assert.match(loginPageSource, /v-model="loginForm\.rememberMe"/)
  assert.match(loginPageSource, /@click="handleLogin"/)
})

test('login page submits the remember-me value from the form model', () => {
  assert.doesNotMatch(loginPageSource, /const rememberMe = ref\(/)
  assert.match(loginPageSource, /rememberMe:\s*false/)
})

test('login page does not prefill demo credentials', () => {
  assert.match(loginPageSource, /username:\s*['"]["']/)
  assert.match(loginPageSource, /password:\s*['"]["']/)
  assert.doesNotMatch(loginPageSource, /username:\s*['"]test['"]/)
  assert.doesNotMatch(loginPageSource, /password:\s*['"]123456['"]/)
})

test('login page only exposes account password login', () => {
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
})

test('app uses the Nexora brand mark in the page and browser chrome', () => {
  assert.match(indexHtmlSource, /href="\/favicon\.svg"/)
  assert.doesNotMatch(indexHtmlSource, /vite\.svg/)
  assert.match(logoSource, /nexora-logo\.svg/)
})
