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
          <p class="brand-kicker">SECURE ACCESS / 安全接入</p>
          <h1 id="brand-title">从一次可信验证，<br />进入整个工作台。</h1>
          <p class="brand-description">统一身份、权限与业务入口，让每次访问都有清晰边界。</p>

          <div class="access-route" role="list" aria-label="进入工作台的访问流程">
            <div class="route-step route-step-active" role="listitem" aria-current="step">
              <span class="route-node" aria-hidden="true"></span>
              <span class="route-number">01</span>
              <span class="route-copy"><strong>身份验证</strong><small>确认邮箱与访问凭证</small></span>
            </div>
            <div class="route-step" role="listitem">
              <span class="route-node" aria-hidden="true"></span>
              <span class="route-number">02</span>
              <span class="route-copy"><strong>权限装载</strong><small>匹配账号角色与边界</small></span>
            </div>
            <div class="route-step" role="listitem">
              <span class="route-node" aria-hidden="true"></span>
              <span class="route-number">03</span>
              <span class="route-copy"><strong>进入工作台</strong><small>开始处理你的业务</small></span>
            </div>
          </div>
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

        <section class="login-box" aria-labelledby="auth-title">
          <div v-if="registerEnabled" class="auth-tabs" role="tablist" aria-label="认证方式">
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

          <div class="login-heading">
            <p class="eyebrow">{{ activeMode === 'login' ? 'WELCOME BACK' : 'CREATE ACCESS' }}</p>
            <h2 id="auth-title">{{ activeMode === 'login' ? '欢迎回来' : '创建你的账号' }}</h2>
            <p>{{ activeMode === 'login' ? '使用工作邮箱进入管理工作台' : '完成邮箱验证，即可创建账号' }}</p>
          </div>

          <transition name="auth-form" mode="out-in">
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
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="loginForm.email" name="email" type="email" placeholder="name@company.com" :prefix-icon="Message" size="large" autocomplete="email" :spellcheck="false" />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input v-model="loginForm.password" name="password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password size="large" autocomplete="current-password" />
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
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="registerForm.email" name="register-email" type="email" placeholder="name@company.com" :prefix-icon="Message" size="large" autocomplete="email" :spellcheck="false" />
              </el-form-item>
              <el-form-item label="邮箱验证码" prop="code">
                <div class="verification-row">
                  <el-input v-model="registerForm.code" name="register-code" inputmode="numeric" maxlength="8" placeholder="请输入验证码" :prefix-icon="Key" size="large" autocomplete="one-time-code" />
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
              <el-form-item label="密码" prop="password">
                <el-input v-model="registerForm.password" name="register-password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password size="large" autocomplete="new-password" />
                <p class="field-hint">使用 6～20 位字符</p>
              </el-form-item>
              <el-button :loading="registering" type="primary" size="large" class="login-button" @click="handleRegister">
                {{ registering ? '正在创建…' : '创建账号' }}
              </el-button>
            </el-form>
          </transition>

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
  --auth-brand: #071d2d;
  --auth-brand-deep: #041520;
  --auth-signal: #38bdf8;
  --auth-signal-soft: rgba(56, 189, 248, .16);
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
  --auth-shadow: 0 24px 70px rgba(22, 43, 65, .1);
  min-height: 100vh;
  min-height: 100dvh;
  overflow-x: hidden;
  color: var(--auth-text);
  background: var(--auth-canvas);
  font-family: "Segoe UI Variable", "PingFang SC", "Microsoft YaHei", sans-serif;
}

.login-container.dark {
  --auth-brand: #061722;
  --auth-brand-deep: #031018;
  --auth-canvas: #08111b;
  --auth-surface: #101c28;
  --auth-text: #e8f0f7;
  --auth-muted: #9cabb9;
  --auth-border: #293847;
  --auth-fill: #172533;
  --auth-shadow: 0 28px 80px rgba(0, 0, 0, .32);
  --auth-action: #38bdf8;
  --auth-action-hover: #7dd3fc;
  --auth-action-on: #041520;
  --auth-focus: rgba(56, 189, 248, .34);
}

.split-screen {
  display: grid;
  min-height: 100vh;
  min-height: 100dvh;
  grid-template-columns: minmax(480px, 1.06fr) minmax(440px, .94fr);
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
  color: #f2f8fc;
  background: var(--auth-brand);
}

.brand-section::after {
  position: absolute;
  z-index: -1;
  right: -18%;
  bottom: -35%;
  width: 68%;
  aspect-ratio: 1;
  border: 1px solid rgba(56, 189, 248, .18);
  border-radius: 50%;
  box-shadow: 0 0 0 56px rgba(56, 189, 248, .035), 0 0 0 112px rgba(56, 189, 248, .018);
  content: "";
}

.brand-grid {
  position: absolute;
  z-index: -2;
  inset: 0;
  opacity: .34;
  background-image:
    linear-gradient(rgba(125, 211, 252, .07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(125, 211, 252, .07) 1px, transparent 1px);
  background-position: -1px -1px;
  background-size: 48px 48px;
  mask-image: linear-gradient(135deg, #000 8%, transparent 68%);
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
  border: 1px solid rgba(186, 230, 253, .2);
  border-radius: 12px;
  background: rgba(255, 255, 255, .06);
}

.brand-mark img { display: block; }
.brand-mark-small { width: 38px; height: 38px; border-color: var(--auth-border); background: var(--auth-fill); }

.brand-content {
  width: min(100%, 620px);
  margin: auto 0;
  padding: 72px 0;
}

.brand-kicker,
.eyebrow,
.route-number {
  font-family: "Cascadia Code", "SFMono-Regular", Consolas, monospace;
}

.brand-kicker {
  margin: 0 0 22px;
  color: #7dd3fc;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: .12em;
}

h1,
h2 { margin: 0; letter-spacing: -.035em; }

h1 {
  max-width: 620px;
  font-size: clamp(38px, 4.2vw, 64px);
  font-weight: 680;
  line-height: 1.14;
}

.brand-description {
  max-width: 500px;
  margin: 24px 0 48px;
  color: #afc2d1;
  font-size: 17px;
  line-height: 1.75;
}

.access-route {
  position: relative;
  display: grid;
  max-width: 500px;
  gap: 0;
}

.route-step {
  position: relative;
  display: grid;
  min-height: 72px;
  grid-template-columns: 16px 34px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}

.route-step:not(:last-child)::after {
  position: absolute;
  top: 18px;
  bottom: -2px;
  left: 7px;
  width: 1px;
  background: rgba(148, 180, 201, .28);
  content: "";
}

.route-step-active::after {
  background: linear-gradient(var(--auth-signal), rgba(148, 180, 201, .28));
}

.route-node {
  position: relative;
  z-index: 1;
  width: 9px;
  height: 9px;
  margin-top: 5px;
  border: 2px solid #7892a5;
  border-radius: 50%;
  background: var(--auth-brand);
}

.route-step-active .route-node {
  border-color: var(--auth-signal);
  background: var(--auth-signal);
  box-shadow: 0 0 0 6px var(--auth-signal-soft);
  animation: route-pulse 2.4s ease-out infinite;
}

.route-number {
  padding-top: 1px;
  color: #7892a5;
  font-size: 11px;
  font-variant-numeric: tabular-nums;
}

.route-step-active .route-number { color: #7dd3fc; }
.route-copy { display: grid; gap: 5px; }
.route-copy strong { color: #eaf4fa; font-size: 15px; font-weight: 600; }
.route-copy small { color: #8fa7b9; font-size: 13px; line-height: 1.5; }

.brand-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #8fa7b9;
  font-size: 12px;
}

.service-status { display: inline-flex; align-items: center; gap: 9px; color: #bad0de; }
.status-dot { width: 7px; height: 7px; border-radius: 50%; background: var(--auth-success); box-shadow: 0 0 0 4px rgba(54, 211, 153, .12); }
.meta-divider { width: 1px; height: 12px; background: rgba(186, 208, 222, .25); }

.login-section {
  position: relative;
  display: grid;
  box-sizing: border-box;
  min-width: 0;
  grid-template-rows: auto 1fr auto;
  padding: 28px clamp(40px, 6vw, 96px) 26px;
  background: var(--auth-canvas);
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
  width: min(100%, 530px);
  margin: 16px auto;
  padding: clamp(28px, 4vw, 44px);
  border: 1px solid var(--auth-border);
  border-radius: 24px;
  background: var(--auth-surface);
  box-shadow: var(--auth-shadow);
}

.auth-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
  margin-bottom: 34px;
  padding: 4px;
  border-radius: 13px;
  background: var(--auth-fill);
}

.auth-tabs button {
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
  background: var(--auth-surface);
  box-shadow: 0 2px 8px rgba(22, 43, 65, .08);
}

.login-heading { margin-bottom: 32px; }
.eyebrow { margin: 0 0 12px; color: var(--auth-action); font-size: 11px; font-weight: 700; letter-spacing: .1em; }
.login-heading h2 { font-size: clamp(28px, 3vw, 34px); font-weight: 680; line-height: 1.2; }
.login-heading > p:last-child { margin: 12px 0 0; color: var(--auth-muted); font-size: 15px; line-height: 1.6; }

:deep(.el-form-item) { margin-bottom: 24px; }
:deep(.el-form-item__label) { margin-bottom: 8px; color: var(--auth-text); font-size: 14px; font-weight: 600; line-height: 1.4; }
:deep(.el-form-item__error) { padding-top: 5px; }
:deep(.el-input__wrapper) {
  min-height: 50px;
  padding: 0 15px;
  border: 1px solid var(--auth-border);
  border-radius: 11px;
  background: var(--auth-surface);
  box-shadow: none;
  transition: border-color .2s ease, box-shadow .2s ease, background-color .2s ease;
}

:deep(.el-input__wrapper:hover) { border-color: var(--auth-action); }
:deep(.el-input__wrapper.is-focus) { border-color: var(--auth-action); box-shadow: 0 0 0 3px var(--auth-focus); }
:deep(.el-input__inner) { min-width: 0; color: var(--auth-text); font-size: 15px; }
:deep(.el-checkbox__label) { color: var(--auth-muted); font-size: 13px; }

.verification-row { display: grid; width: 100%; grid-template-columns: minmax(0, 1fr) 122px; gap: 10px; }
.code-button { min-height: 50px; margin: 0; border-radius: 11px; font-weight: 600; }
.code-button:disabled { cursor: not-allowed; }
.field-hint { width: 100%; margin: 7px 0 0; color: var(--auth-muted); font-size: 12px; line-height: 1.5; }

.login-options { display: flex; justify-content: space-between; align-items: center; margin: -2px 0 26px; font-size: 13px; }
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
  min-height: 50px;
  border-radius: 11px;
  font-size: 15px;
  font-weight: 650;
  letter-spacing: .01em;
}
.security-note { display: flex; align-items: flex-start; gap: 8px; margin-top: 24px; color: var(--auth-muted); font-size: 12px; line-height: 1.55; }
.security-note .el-icon { flex: 0 0 auto; margin-top: 2px; color: var(--auth-action); }
.login-footer { align-self: end; margin: 12px 0 0; color: var(--auth-muted); font-size: 11px; text-align: center; }

.auth-form-enter-active { transition: opacity .26s ease-out, transform .26s cubic-bezier(.16, 1, .3, 1); }
.auth-form-leave-active { transition: opacity .16s ease-in, transform .16s ease-in; }
.auth-form-enter-from { opacity: 0; transform: translateY(8px); }
.auth-form-leave-to { opacity: 0; transform: translateY(-4px); }

@keyframes route-pulse {
  0%, 55% { box-shadow: 0 0 0 6px var(--auth-signal-soft); }
  80%, 100% { box-shadow: 0 0 0 12px rgba(56, 189, 248, 0); }
}

@media (max-width: 1024px) {
  .split-screen { grid-template-columns: minmax(390px, .9fr) minmax(420px, 1.1fr); }
  .brand-section { padding: 36px; }
  .brand-content { padding: 56px 0; }
  h1 { font-size: clamp(36px, 5vw, 48px); }
  .brand-description { margin-bottom: 40px; }
  .login-section { padding-inline: 40px; }
}

@media (max-height: 800px) and (min-width: 821px) {
  .brand-section { padding-block: 28px; }
  .brand-content { padding: 24px 0; }
  .brand-kicker { margin-bottom: 16px; }
  h1 { font-size: clamp(36px, 4vw, 48px); }
  .brand-description { margin: 16px 0 28px; font-size: 15px; }
  .route-step { min-height: 58px; }
  .login-section { padding-block: 16px 14px; }
  .auth-topbar { min-height: 44px; }
  .login-box { margin-block: 8px; padding: 20px 36px; }
  .auth-tabs { margin-bottom: 22px; }
  .login-heading { margin-bottom: 22px; }
  .login-heading h2 { font-size: 30px; }
  .login-heading > p:last-child { margin-top: 8px; }
  :deep(.el-form-item) { margin-bottom: 16px; }
  :deep(.el-input__wrapper) { min-height: 46px; }
  .login-options { margin: -2px 0 12px; }
  .login-button,
  .code-button { min-height: 46px; }
  .security-note { margin-top: 12px; }
  .login-footer { margin-top: 8px; }
}

@media (max-width: 820px) {
  .split-screen { grid-template-columns: 1fr; }
  .brand-section { display: none; }
  .login-section { min-height: 100vh; min-height: 100dvh; padding: 24px clamp(24px, 7vw, 56px); }
  .auth-topbar { justify-content: space-between; }
  .mobile-brand { display: flex; }
  .login-box { margin-block: 32px; }
}

@media (max-width: 480px) {
  .login-section { padding: 18px 16px 20px; }
  .login-box { width: 100%; margin-block: 24px; padding: 26px 20px; border-radius: 20px; }
  .auth-tabs { margin-bottom: 28px; }
  .login-heading { margin-bottom: 28px; }
  .login-heading h2 { font-size: 28px; }
  .login-heading > p:last-child { font-size: 14px; }
  .verification-row { grid-template-columns: minmax(0, 1fr) 112px; gap: 8px; }
  .code-button { padding-inline: 12px; }
  .login-footer { font-size: 10px; }
}

@media (max-width: 360px) {
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
