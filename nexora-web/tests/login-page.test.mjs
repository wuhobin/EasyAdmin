import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const loginPagePath = new URL('../src/views/login/index.vue', import.meta.url)
const loginPageSource = await readFile(loginPagePath, 'utf8')
const sliderCaptchaPath = new URL('../src/components/SliderCaptcha/index.vue', import.meta.url)
const sliderCaptchaSource = await readFile(sliderCaptchaPath, 'utf8')
const authApiPath = new URL('../src/api/system/auth.ts', import.meta.url)
const authApiSource = await readFile(authApiPath, 'utf8')
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

test('authentication validation runs on actions instead of field blur or change', () => {
  const validationRulesSource = loginPageSource.match(/const emailRules:[\s\S]*?const isDark/)?.[0] ?? ''
  assert.match(validationRulesSource, /trigger:\s*['"]submit['"]/)
  assert.doesNotMatch(validationRulesSource, /trigger:\s*['"](?:blur|change)['"]/)
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
  assert.match(loginPageSource, /v-model="registerForm\.password"[^>]*placeholder="请输入密码（6～20 位字符）"/)
  assert.match(loginPageSource, /sendRegisterCodeApi\(registerForm\)/)
  assert.match(loginPageSource, /codeCountdown\.value = 60/)
  assert.match(loginPageSource, /loginForm\.email = registerForm\.email/)
  assert.doesNotMatch(loginPageSource, /registerForm\.confirmPassword/)
})

test('registration opens image verification before creating the account', () => {
  const registerHandler = loginPageSource.match(
    /const handleRegister = async \(\) => \{[\s\S]*?\n\}/
  )?.[0] ?? ''
  const captchaSuccessHandler = loginPageSource.match(
    /const handleImageCaptchaSuccess = async \(captchaId: string\) => \{[\s\S]*?\n\}/
  )?.[0] ?? ''

  assert.match(loginPageSource, /import SliderCaptcha from/)
  assert.match(loginPageSource, /<SliderCaptcha[\s\S]*v-model="captchaDialogVisible"[\s\S]*@success="handleImageCaptchaSuccess"/)
  assert.match(registerHandler, /captchaDialogVisible\.value = true/)
  assert.doesNotMatch(registerHandler, /registerApi\(/)
  assert.match(captchaSuccessHandler, /captchaDialogVisible\.value = false/)
  assert.match(captchaSuccessHandler, /registerApi\(\{[\s\S]*captchaId/)
})

test('image captcha API follows the tianai challenge and track contract', () => {
  assert.match(authApiSource, /export interface ImageCaptchaResult/)
  assert.match(authApiSource, /backgroundImage:\s*string/)
  assert.match(authApiSource, /templateImage:\s*string/)
  assert.match(authApiSource, /export interface ImageCaptchaTrack/)
  assert.match(authApiSource, /url:\s*['"]\/auth\/image['"]/)
  assert.match(authApiSource, /\/auth\/image\/\$\{encodeURIComponent\(captchaId\)\}\/match/)
  assert.match(authApiSource, /export type RegisterParams[\s\S]*captchaId:\s*string/)
})

test('slider captcha renders server images and only accepts an explicit match result', () => {
  assert.match(sliderCaptchaSource, /generateImageCaptchaApi\(\)/)
  assert.match(sliderCaptchaSource, /:src="captcha\.backgroundImage"/)
  assert.match(sliderCaptchaSource, /:src="captcha\.templateImage"/)
  assert.match(sliderCaptchaSource, /matchImageCaptchaApi\(currentCaptcha\.id, track\)/)
  assert.match(sliderCaptchaSource, /if \(matched === true\)/)
  assert.match(sliderCaptchaSource, /emit\(['"]success['"], currentCaptcha\.id\)/)
  assert.doesNotMatch(sliderCaptchaSource, /completeWithKeyboard|threshold\s*=|maxWidth\s*-\s*45/)
})

test('slider captcha sends bounded relative tracks with rendered image dimensions', () => {
  assert.match(sliderCaptchaSource, /getBoundingClientRect\(\)/)
  assert.match(sliderCaptchaSource, /bgImageWidth:\s*Math\.round\(backgroundRect\.width\)/)
  assert.match(sliderCaptchaSource, /templateImageWidth:\s*Math\.round\(templateRect\.width\)/)
  assert.match(sliderCaptchaSource, /x:\s*type === ['"]DOWN['"] \? 0 : sliderLeft\.value/)
  assert.match(sliderCaptchaSource, /y:\s*event\.pageY - dragStartY/)
  assert.match(sliderCaptchaSource, /appendTrackPoint\(event, ['"]DOWN['"]\)/)
  assert.match(sliderCaptchaSource, /appendTrackPoint\(event, ['"]MOVE['"]\)/)
  assert.match(sliderCaptchaSource, /appendTrackPoint\(event, ['"]UP['"]\)/)
  assert.match(sliderCaptchaSource, /stopTime - dragStartTime < 300 \|\| trackList\.length < 10/)
  assert.match(sliderCaptchaSource, /left:\s*Math\.round\(sliderLeft\.value\)/)
  assert.match(sliderCaptchaSource, /top:\s*0/)
  assert.match(sliderCaptchaSource, /data:\s*currentCaptcha\.data/)
})

test('failed image matching reloads the challenge instead of unlocking registration', () => {
  const submitTrackSource = sliderCaptchaSource.match(
    /const submitTrack = async \(event: PointerEvent\) => \{[\s\S]*?\n\}/
  )?.[0] ?? ''

  assert.match(submitTrackSource, /if \(matched === true\)/)
  assert.match(submitTrackSource, /status\.value = ['"]error['"]/)
  assert.match(submitTrackSource, /scheduleReload\(\)/)
  assert.match(sliderCaptchaSource, /status === ['"]success['"] \|\| status === ['"]error['"]/)
  assert.match(sliderCaptchaSource, /status\.value !== ['"]idle['"]/)
})

test('forgot password opens an email verification dialog and resets the password', () => {
  assert.match(loginPageSource, /v-model="resetDialogVisible"/)
  assert.match(loginPageSource, /v-model="resetPasswordForm\.email"/)
  assert.match(loginPageSource, /v-model="resetPasswordForm\.code"/)
  assert.match(loginPageSource, /v-model="resetPasswordForm\.password"/)
  assert.match(loginPageSource, /v-model="resetPasswordForm\.password"[\s\S]*?placeholder="请输入新密码（6～20 位字符）"/)
  assert.match(loginPageSource, /v-model="resetPasswordForm\.confirmPassword"/)
  assert.match(loginPageSource, /sendResetPasswordCodeApi\(\{ email: resetPasswordForm\.email \}\)/)
  assert.match(loginPageSource, /resetPasswordApi\(\{/)
  assert.match(loginPageSource, /resetCodeCountdown\.value = 60/)
  assert.match(loginPageSource, /两次输入的密码不一致/)
  assert.match(loginPageSource, /loginForm\.email = resetPasswordForm\.email/)
  assert.match(loginPageSource, /密码重置成功，请使用新密码登录/)
  assert.doesNotMatch(loginPageSource, /class="field-hint"/)
})

test('reset dialog reserves validation space without changing height', () => {
  assert.match(loginPageSource, /\.reset-password-dialog \.el-form-item\)[^{]*\{[^}]*margin-bottom:\s*28px/s)
  assert.match(loginPageSource, /\.reset-password-dialog \.el-dialog__body > \.el-form:last-child \.el-form-item:last-child\)[^{]*\{[^}]*margin-bottom:\s*36px/s)
  assert.match(loginPageSource, /\.reset-password-dialog \.el-form-item__error\)[^{]*\{[^}]*position:\s*absolute/s)
  assert.match(loginPageSource, /\.reset-password-dialog \.el-form-item__error\)[^{]*\{[^}]*top:\s*100%/s)
})

test('reset dialog avoids internal scrolling and separates footer actions', () => {
  assert.match(loginPageSource, /\.reset-password-dialog \.el-dialog__body\)[^{]*\{[^}]*overflow:\s*hidden/s)
  assert.match(loginPageSource, /\.reset-password-dialog \.el-dialog__footer\)[^{]*\{[^}]*padding:\s*14px 26px 20px/s)
  assert.match(loginPageSource, /@media \(max-height:\s*620px\)[\s\S]*?\.reset-password-dialog \.el-form-item\)[^{]*\{[^}]*margin-bottom:\s*24px/s)
  assert.match(loginPageSource, /@media \(max-height:\s*620px\)[\s\S]*?\.reset-password-dialog \.el-dialog__body\)[^{]*\{[^}]*padding:\s*14px 22px 16px/s)
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
