<template>
  <div class="login-container" :class="{ dark: isDark }">
    <main class="split-screen">
      <section class="brand-section" aria-labelledby="brand-title">
        <div class="brand-grid" aria-hidden="true"></div>
        <header class="brand-lockup">
          <span class="brand-mark">
            <img :src="logoUrl" alt="" width="28" height="28" aria-hidden="true" />
          </span>
          <span>{{ settingsStore.title }}</span>
        </header>

        <div class="brand-content">
          <h1 id="brand-title">安全进入你的<br />管理工作台</h1>
          <p class="brand-description">统一身份与权限边界，让每次访问清晰可控。</p>
        </div>

        <footer class="brand-meta">
          <span class="service-status"><span class="status-dot" aria-hidden="true"></span>接入服务正常</span>
          <span class="meta-divider" aria-hidden="true"></span>
          <span>加密连接</span>
        </footer>
      </section>

      <section class="login-section">
        <div class="auth-topbar">
          <div class="mobile-brand">
            <span class="brand-mark brand-mark-small">
              <img :src="logoUrl" alt="" width="24" height="24" aria-hidden="true" />
            </span>
            <span>{{ settingsStore.title }}</span>
          </div>
          <button class="theme-toggle" type="button" :aria-label="isDark ? '切换到浅色模式' : '切换到深色模式'" @click="toggleTheme">
            <el-icon><component :is="isDark ? Sunny : Moon" /></el-icon>
          </button>
        </div>

        <section class="login-box" aria-label="账号访问">
          <div
            v-if="registerEnabled || !registerConfigLoaded"
            class="auth-tabs"
            :class="{ 'is-loading': !registerConfigLoaded, 'is-register': activeMode === 'register' }"
            role="tablist"
            aria-label="认证方式"
            :aria-hidden="!registerConfigLoaded"
          >
            <button
              id="login-tab"
              type="button"
              role="tab"
              aria-controls="login-panel"
              :aria-selected="activeMode === 'login'"
              :tabindex="activeMode === 'login' ? 0 : -1"
              :class="{ active: activeMode === 'login' }"
              @click="switchMode('login')"
              @keydown.right.prevent="switchMode('register', true)"
            >
              登录
            </button>
            <button
              id="register-tab"
              type="button"
              role="tab"
              aria-controls="register-panel"
              :aria-selected="activeMode === 'register'"
              :tabindex="activeMode === 'register' ? 0 : -1"
              :class="{ active: activeMode === 'register' }"
              @click="switchMode('register')"
              @keydown.left.prevent="switchMode('login', true)"
            >
              注册
            </button>
          </div>

          <p class="mode-description" aria-live="polite">
            {{ activeMode === 'login' ? '使用工作邮箱进入管理工作台' : '验证邮箱并创建新账号' }}
          </p>

          <div class="auth-form-stage">
            <transition name="auth-form">
              <el-form
                v-if="activeMode === 'login'"
                id="login-panel"
                ref="loginFormRef"
                key="login"
                :role="registerEnabled ? 'tabpanel' : undefined"
                :aria-labelledby="registerEnabled ? 'login-tab' : undefined"
                label-position="top"
                :model="loginForm"
                :rules="loginRules"
                @keyup.enter="handleLogin"
              >
                <el-form-item prop="email">
                  <el-input v-model="loginForm.email" name="email" type="email" placeholder="请输入邮箱" aria-label="邮箱" :prefix-icon="Message" size="large" autocomplete="email" :spellcheck="false" />
                </el-form-item>
                <el-form-item prop="password">
                  <el-input v-model="loginForm.password" name="password" type="password" placeholder="请输入密码" aria-label="密码" :prefix-icon="Lock" show-password size="large" autocomplete="current-password" />
                </el-form-item>
                <div class="login-options">
                  <el-checkbox v-model="loginForm.rememberMe">记住我</el-checkbox>
                  <button class="forgot-password" type="button" @click="handleForgotPassword">忘记密码？</button>
                </div>
                <el-button :loading="loading" type="primary" size="large" class="login-button" @click="handleLogin">
                  {{ loading ? '正在验证…' : '进入工作台' }}
                </el-button>
              </el-form>

              <el-form
                v-else
                id="register-panel"
                ref="registerFormRef"
                key="register"
                role="tabpanel"
                aria-labelledby="register-tab"
                label-position="top"
                :model="registerForm"
                :rules="registerRules"
                @keyup.enter="handleRegister"
              >
                <el-form-item prop="email">
                  <el-input v-model="registerForm.email" name="register-email" type="email" placeholder="请输入邮箱" aria-label="邮箱" :prefix-icon="Message" size="large" autocomplete="email" :spellcheck="false" />
                </el-form-item>
                <el-form-item prop="code">
                  <div class="verification-row">
                    <el-input v-model="registerForm.code" name="register-code" inputmode="numeric" maxlength="8" placeholder="请输入验证码" aria-label="邮箱验证码" :prefix-icon="Key" size="large" autocomplete="one-time-code" />
                    <el-button
                      class="code-button"
                      size="large"
                      :loading="codeSending"
                      :disabled="codeCountdown > 0"
                      :aria-label="codeCountdown > 0 ? `${codeCountdown} 秒后可重新获取验证码` : '获取邮箱验证码'"
                      @click="handleSendRegisterCode"
                    >
                      {{ codeCountdown > 0 ? `${codeCountdown} 秒` : '获取验证码' }}
                    </el-button>
                  </div>
                </el-form-item>
                <el-form-item prop="password">
                  <el-input v-model="registerForm.password" name="register-password" type="password" placeholder="请输入密码" aria-label="密码" :prefix-icon="Lock" show-password size="large" autocomplete="new-password" />
                  <p class="field-hint">使用 6～20 位字符</p>
                </el-form-item>
                <el-button :loading="registering" type="primary" size="large" class="login-button" @click="handleRegister">
                  {{ registering ? '正在创建…' : '创建账号' }}
                </el-button>
              </el-form>
            </transition>
          </div>

          <div class="security-note">
            <el-icon aria-hidden="true"><Lock /></el-icon>
            <span>{{ activeMode === 'login' ? '安全连接已建立，会话凭证将被加密传输' : '验证码仅用于完成本次账号注册' }}</span>
          </div>
        </section>

        <p class="login-footer">© 2026 Nexora Admin · Secure Access</p>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import router from '@/router'
import type { FormInstance, FormItemRule, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Key, Lock, Message, Moon, Sunny } from '@element-plus/icons-vue'
import logoUrl from '@/assets/brand/nexora-logo.svg'
import { useUserStore } from '@/store/modules/user'
import { useSettingsStore } from '@/store/modules/settings'
import { registerApi, sendRegisterCodeApi, type AuthParams } from '@/api/system/auth'
import { getConfigValueApi } from '@/api/system/config'

type AuthMode = 'login' | 'register'

const REGISTER_ENABLED_CONFIG_KEY = 'register.enabled'
const userStore = useUserStore()
const settingsStore = useSettingsStore()
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()
const loading = ref(false)
const registering = ref(false)
const codeSending = ref(false)
const codeCountdown = ref(0)
const registerEnabled = ref(false)
const registerConfigLoaded = ref(false)
const activeMode = ref<AuthMode>('login')
let countdownTimer: ReturnType<typeof setInterval> | undefined

const createAuthForm = (): AuthParams => ({
  email: '',
  password: '',
  code: '',
  rememberMe: false,
  source: 'ADMIN'
})
const loginForm = reactive<AuthParams>(createAuthForm())
const registerForm = reactive<AuthParams>(createAuthForm())

const emailRules: FormItemRule[] = [
  { required: true, message: '请输入邮箱', trigger: 'blur' },
  { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
]
const passwordRules: FormItemRule[] = [
  { required: true, message: '请输入密码', trigger: 'blur' },
  { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
]
const loginRules: FormRules<AuthParams> = {
  email: emailRules,
  password: passwordRules
}
const registerRules: FormRules<AuthParams> = {
  email: emailRules,
  code: [
    { required: true, message: '请输入邮箱验证码', trigger: 'blur' },
    { pattern: /^\d{4,8}$/, message: '邮箱验证码格式不正确', trigger: 'blur' }
  ],
  password: passwordRules
}

const isDark = computed(() => settingsStore.theme === 'dark')
const toggleTheme = () => settingsStore.saveSettings({ theme: isDark.value ? 'light' : 'dark' })

const switchMode = (mode: AuthMode, focusTab = false) => {
  if (mode === 'register' && !registerEnabled.value) return
  activeMode.value = mode
  nextTick(() => {
    loginFormRef.value?.clearValidate()
    registerFormRef.value?.clearValidate()
    if (focusTab) document.getElementById(`${mode}-tab`)?.focus()
  })
}

const loadRegisterConfig = async () => {
  try {
    const { data } = await getConfigValueApi(REGISTER_ENABLED_CONFIG_KEY)
    registerEnabled.value = data === 'true'
  } catch {
    registerEnabled.value = false
  } finally {
    registerConfigLoaded.value = true
  }
}

const handleForgotPassword = () => {
  ElMessage.info('请联系系统管理员重置密码')
}

const handleLogin = async () => {
  if (!loginFormRef.value || !(await loginFormRef.value.validate().catch(() => false))) return
  loading.value = true
  try {
    await userStore.login(loginForm)
    await router.push('/')
    ElMessage.success('登录成功')
  } catch (error) {
    console.error('Login failed:', error)
  } finally {
    loading.value = false
  }
}

const startCodeCountdown = () => {
  if (countdownTimer) clearInterval(countdownTimer)
  codeCountdown.value = 60
  countdownTimer = setInterval(() => {
    codeCountdown.value -= 1
    if (codeCountdown.value <= 0 && countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = undefined
    }
  }, 1000)
}

const handleSendRegisterCode = async () => {
  if (!registerFormRef.value) return
  const emailValid = await registerFormRef.value.validateField('email').then(() => true).catch(() => false)
  if (!emailValid) return
  codeSending.value = true
  try {
    await sendRegisterCodeApi(registerForm)
    startCodeCountdown()
    ElMessage.success('验证码已发送，请查收邮箱')
  } catch (error) {
    console.error('Register code send failed:', error)
  } finally {
    codeSending.value = false
  }
}

const handleRegister = async () => {
  if (!registerFormRef.value || !(await registerFormRef.value.validate().catch(() => false))) return
  registering.value = true
  try {
    await registerApi(registerForm)
    loginForm.email = registerForm.email
    loginForm.password = ''
    registerForm.password = ''
    registerForm.code = ''
    switchMode('login')
    ElMessage.success('注册成功，请登录')
  } catch (error) {
    console.error('Register failed:', error)
  } finally {
    registering.value = false
  }
}

onMounted(loadRegisterConfig)
onBeforeUnmount(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<style lang="scss" scoped>
.login-container {
  --auth-brand: #dce8ee;
  --auth-brand-soft: #e8f1f4;
  --auth-brand-text: #183747;
  --auth-brand-muted: #5d7481;
  --auth-brand-border: rgba(3, 105, 161, .12);
  --auth-brand-grid: rgba(3, 105, 161, .07);
  --auth-brand-mark: rgba(255, 255, 255, .34);
  --auth-success: #36d399;
  --auth-action: #0369a1;
  --auth-action-hover: #075985;
  --auth-action-on: #ffffff;
  --auth-focus: rgba(3, 105, 161, .24);
  --auth-canvas: #f1f5f8;
  --auth-surface: #ffffff;
  --auth-text: #142333;
  --auth-muted: #607083;
  --auth-border: #d8e1e8;
  --auth-fill: #edf2f6;
  --auth-shadow: 0 16px 44px rgba(22, 43, 65, .07);
  min-height: 100vh;
  min-height: 100dvh;
  overflow-x: hidden;
  color: var(--auth-text);
  background: var(--auth-canvas);
  font-family: "Segoe UI Variable", "PingFang SC", "Microsoft YaHei", sans-serif;
}

.login-container.dark {
  --auth-brand: #0c2432;
  --auth-brand-soft: #102b3a;
  --auth-brand-text: #e8f0f7;
  --auth-brand-muted: #9fb5c3;
  --auth-brand-border: rgba(125, 211, 252, .1);
  --auth-brand-grid: rgba(125, 211, 252, .05);
  --auth-brand-mark: rgba(255, 255, 255, .07);
  --auth-canvas: #08111b;
  --auth-surface: #101c28;
  --auth-text: #e8f0f7;
  --auth-muted: #9cabb9;
  --auth-border: #293847;
  --auth-fill: #172533;
  --auth-shadow: 0 18px 48px rgba(0, 0, 0, .22);
  --auth-action: #38bdf8;
  --auth-action-hover: #7dd3fc;
  --auth-action-on: #041520;
  --auth-focus: rgba(56, 189, 248, .34);
}

.split-screen {
  display: grid;
  box-sizing: border-box;
  min-height: 100vh;
  min-height: 100dvh;
  grid-template-columns: minmax(400px, 9fr) minmax(500px, 11fr);
  padding: 18px;
}

.brand-section {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  display: flex;
  box-sizing: border-box;
  min-height: 100%;
  flex-direction: column;
  padding: clamp(32px, 4vw, 64px);
  color: var(--auth-brand-text);
  border: 1px solid var(--auth-brand-border);
  border-radius: 24px;
  background: linear-gradient(145deg, var(--auth-brand), var(--auth-brand-soft));
}

.brand-section::after {
  position: absolute;
  z-index: -1;
  right: -24%;
  bottom: -30%;
  width: 56%;
  aspect-ratio: 1;
  border: 1px solid var(--auth-brand-border);
  border-radius: 50%;
  box-shadow: 0 0 0 64px var(--auth-brand-grid);
  content: "";
}

.brand-grid {
  position: absolute;
  z-index: -2;
  inset: 0;
  opacity: .18;
  background-image:
    linear-gradient(var(--auth-brand-grid) 1px, transparent 1px),
    linear-gradient(90deg, var(--auth-brand-grid) 1px, transparent 1px);
  background-position: -1px -1px;
  background-size: 64px 64px;
  mask-image: linear-gradient(135deg, #000 4%, transparent 64%);
}

.brand-lockup,
.mobile-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -.01em;
}

.brand-mark {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border: 1px solid var(--auth-brand-border);
  border-radius: 12px;
  background: var(--auth-brand-mark);
}

.brand-mark img { display: block; }
.brand-mark-small { width: 38px; height: 38px; border-color: var(--auth-border); background: var(--auth-fill); }

.brand-content {
  width: min(100%, 420px);
  margin: auto 0;
  padding: 48px 0;
}

h1 { margin: 0; letter-spacing: -.03em; }

h1 {
  max-width: 420px;
  font-size: clamp(32px, 3.2vw, 44px);
  font-weight: 650;
  line-height: 1.22;
}

.brand-description {
  max-width: 360px;
  margin: 18px 0 0;
  color: var(--auth-brand-muted);
  font-size: 15px;
  line-height: 1.7;
}

.brand-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--auth-brand-muted);
  font-size: 12px;
}

.service-status { display: inline-flex; align-items: center; gap: 9px; color: var(--auth-brand-text); }
.status-dot { width: 7px; height: 7px; border-radius: 50%; background: var(--auth-success); box-shadow: 0 0 0 4px rgba(54, 211, 153, .12); }
.meta-divider { width: 1px; height: 12px; background: var(--auth-brand-border); }

.login-section {
  position: relative;
  display: grid;
  box-sizing: border-box;
  min-width: 0;
  grid-template-rows: auto 1fr auto;
  padding: 24px clamp(24px, 4vw, 56px) 22px;
  background: transparent;
}

.auth-topbar { display: flex; min-height: 48px; justify-content: flex-end; align-items: center; }
.mobile-brand { display: none; color: var(--auth-text); }

.theme-toggle {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  color: var(--auth-muted);
  border: 1px solid var(--auth-border);
  border-radius: 12px;
  background: var(--auth-surface);
  box-shadow: 0 8px 24px rgba(22, 43, 65, .06);
  cursor: pointer;
  touch-action: manipulation;
  transition: color .2s ease, border-color .2s ease, background-color .2s ease, transform .2s ease;
}

.theme-toggle:hover { color: var(--auth-action); border-color: var(--auth-action); }
.theme-toggle:active { transform: scale(.96); }
.theme-toggle:focus-visible,
.auth-tabs button:focus-visible,
.forgot-password:focus-visible { outline: 3px solid var(--auth-focus); outline-offset: 3px; }

.login-box {
  --el-color-primary: var(--auth-action);
  box-sizing: border-box;
  align-self: center;
  width: min(100%, 430px);
  margin: 16px auto;
  padding: 30px;
  border: 1px solid var(--auth-border);
  border-radius: 18px;
  background: var(--auth-surface);
  box-shadow: var(--auth-shadow);
}

.auth-tabs {
  position: relative;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
  margin-bottom: 20px;
  padding: 4px;
  border-radius: 13px;
  background: var(--auth-fill);
}

.auth-tabs::before {
  position: absolute;
  z-index: 0;
  top: 4px;
  bottom: 4px;
  left: 4px;
  width: calc((100% - 12px) / 2);
  border-radius: 10px;
  background: var(--auth-surface);
  box-shadow: 0 2px 8px rgba(22, 43, 65, .08);
  content: "";
  transition: transform .24s cubic-bezier(.22, 1, .36, 1);
}

.auth-tabs.is-register::before {
  transform: translateX(calc(100% + 4px));
}

.auth-tabs.is-loading {
  visibility: hidden;
  pointer-events: none;
}

.auth-tabs button {
  position: relative;
  z-index: 1;
  min-height: 44px;
  padding: 0 16px;
  color: var(--auth-muted);
  border: 0;
  border-radius: 10px;
  background: transparent;
  font: inherit;
  font-size: 14px;
  font-weight: 650;
  cursor: pointer;
  touch-action: manipulation;
  transition: color .2s ease, background-color .2s ease, box-shadow .2s ease;
}

.auth-tabs button.active {
  color: var(--auth-text);
}

.mode-description { margin: 0 0 20px; color: var(--auth-muted); font-size: 14px; line-height: 1.6; }
.auth-form-stage { position: relative; min-height: 300px; }

:deep(.el-form-item) { margin-bottom: 28px; }
:deep(.el-form-item__error) { padding-top: 5px; }
:deep(.el-input__wrapper) {
  min-height: 46px;
  padding: 0 13px;
  border: 1px solid var(--auth-border);
  border-radius: 11px;
  background: var(--auth-surface);
  box-shadow: none;
  transition: border-color .2s ease, box-shadow .2s ease;
}

:deep(.el-input__wrapper:hover) { border-color: var(--auth-action); }
:deep(.el-input__wrapper.is-focus) { border-color: var(--auth-action); box-shadow: 0 0 0 3px var(--auth-focus); }
:deep(.el-input__inner) { min-width: 0; color: var(--auth-text); font-size: 14px; }
:deep(.el-input__inner:-webkit-autofill),
:deep(.el-input__inner:-webkit-autofill:hover),
:deep(.el-input__inner:-webkit-autofill:focus),
:deep(.el-input__inner:-webkit-autofill:active) {
  caret-color: var(--auth-text);
  -webkit-text-fill-color: var(--auth-text);
  -webkit-box-shadow: 0 0 0 1000px var(--auth-surface) inset;
  box-shadow: 0 0 0 1000px var(--auth-surface) inset;
  transition: background-color 9999s ease-out 0s;
}
:deep(.el-checkbox__label) { color: var(--auth-muted); font-size: 13px; }

.verification-row { display: grid; width: 100%; grid-template-columns: minmax(0, 1fr) 112px; gap: 8px; }
.code-button { min-height: 46px; margin: 0; padding-inline: 12px; border-radius: 11px; font-weight: 600; }
.code-button:disabled { cursor: not-allowed; }
.field-hint { width: 100%; margin: 7px 0 0; color: var(--auth-muted); font-size: 12px; line-height: 1.5; }

.login-options { display: flex; justify-content: space-between; align-items: center; margin: -2px 0 14px; font-size: 13px; }
.forgot-password { min-height: 44px; padding: 0 2px; border: 0; color: var(--auth-action); background: transparent; font: inherit; cursor: pointer; touch-action: manipulation; }
.forgot-password:hover { text-decoration: underline; text-underline-offset: 4px; }

.login-button {
  --el-button-bg-color: var(--auth-action);
  --el-button-border-color: var(--auth-action);
  --el-button-text-color: var(--auth-action-on);
  --el-button-hover-bg-color: var(--auth-action-hover);
  --el-button-hover-border-color: var(--auth-action-hover);
  --el-button-hover-text-color: var(--auth-action-on);
  --el-button-active-bg-color: var(--auth-action-hover);
  --el-button-active-border-color: var(--auth-action-hover);
  width: 100%;
  min-height: 46px;
  border-radius: 11px;
  font-size: 15px;
  font-weight: 650;
  letter-spacing: .01em;
}
.security-note { display: flex; align-items: flex-start; gap: 8px; min-height: 19px; margin-top: 14px; color: var(--auth-muted); font-size: 12px; line-height: 1.55; }
.security-note .el-icon { flex: 0 0 auto; margin-top: 2px; color: var(--auth-action); }
.login-footer { align-self: end; margin: 12px 0 0; color: var(--auth-muted); font-size: 11px; text-align: center; }

.auth-form-enter-active {
  transition: opacity .18s ease-out;
}

.auth-form-leave-active {
  position: absolute;
  inset: 0;
  width: 100%;
  pointer-events: none;
  transition: opacity .12s ease-in;
}

.auth-form-enter-from,
.auth-form-leave-to { opacity: 0; }

@media (max-width: 1024px) {
  .split-screen { grid-template-columns: minmax(340px, 9fr) minmax(480px, 11fr); }
  .brand-section { padding: 36px; }
  .brand-content { padding: 36px 0; }
  h1 { font-size: clamp(32px, 4vw, 40px); }
  .login-section { padding-inline: 32px; }
}

@media (max-height: 800px) and (min-width: 821px) {
  .brand-section { padding-block: 28px; }
  .brand-content { padding: 24px 0; }
  h1 { font-size: clamp(30px, 3.4vw, 38px); }
  .brand-description { margin-top: 14px; font-size: 14px; }
  .login-section { padding-block: 16px 14px; }
  .auth-topbar { min-height: 44px; }
  .login-box { margin-block: 8px; padding: 26px 28px; }
  .auth-tabs { margin-bottom: 18px; }
  .mode-description { margin-bottom: 16px; }
  .login-footer { margin-top: 8px; }
}

@media (max-width: 820px) {
  .split-screen { grid-template-columns: 1fr; padding: 0; }
  .brand-section { display: none; }
  .login-section { min-height: 100vh; min-height: 100dvh; padding: 24px clamp(24px, 7vw, 56px); }
  .auth-topbar { justify-content: space-between; }
  .mobile-brand { display: flex; }
  .login-box { margin-block: 32px; }
}

@media (max-width: 480px) {
  .login-section { padding: 18px 16px 20px; }
  .login-box { width: 100%; margin-block: 24px; padding: 24px 20px; border-radius: 18px; }
  .auth-tabs { margin-bottom: 20px; }
  .mode-description { margin-bottom: 18px; }
  .verification-row { grid-template-columns: minmax(0, 1fr) 112px; gap: 8px; }
  .login-footer { font-size: 10px; }
}

@media (max-width: 360px) {
  .auth-form-stage { min-height: 354px; }
  .verification-row { grid-template-columns: 1fr; }
  .code-button { width: 100%; }
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    scroll-behavior: auto !important;
    transition-duration: .01ms !important;
    animation-duration: .01ms !important;
    animation-iteration-count: 1 !important;
  }
}
</style>
