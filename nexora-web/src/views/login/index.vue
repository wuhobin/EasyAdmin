<template>
  <div class="login-container" :class="{ dark: isDark }">
    <button class="theme-toggle" type="button" :aria-label="isDark ? '切换到浅色模式' : '切换到深色模式'" @click="toggleTheme">
      <el-icon><component :is="isDark ? Sunny : Moon" /></el-icon>
    </button>
    <main class="split-screen">
      <section class="brand-section">
        <div class="brand-accent" aria-hidden="true"></div>
        <div class="brand-content">
          <h1>{{ settingsStore.title }}</h1>
          <p class="brand-description">清晰、高效、安全的企业管理工作台</p>
          <div class="feature-list">
            <div><el-icon><Monitor /></el-icon><span>统一工作台</span></div>
            <div><el-icon><Lock /></el-icon><span>安全权限体系</span></div>
            <div><el-icon><Histogram /></el-icon><span>运行状态洞察</span></div>
          </div>
          <div class="brand-meta"><span class="status-dot"></span>系统服务正常 <span class="meta-divider"></span> v1.0</div>
        </div>
      </section>
      <section class="login-section">
        <div class="login-box">
          <div class="login-heading">
            <p class="eyebrow">WELCOME BACK</p>
            <h2>登录管理后台</h2>
            <p>请输入账号信息继续</p>
          </div>
          <el-form ref="loginFormRef" label-position="top" :model="loginForm" :rules="rules" @keyup.enter="handleLogin">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="loginForm.username" name="username" placeholder="请输入用户名" :prefix-icon="User" size="large" autocomplete="username" :spellcheck="false" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="loginForm.password" name="password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password size="large" autocomplete="current-password" />
            </el-form-item>
            <div class="login-options">
              <el-checkbox v-model="loginForm.rememberMe">记住我</el-checkbox>
              <button class="forgot-password" type="button" @click="handleForgotPassword">忘记密码？</button>
            </div>
            <el-button :loading="loading" type="primary" size="large" class="login-button" @click="handleLogin">
              {{ loading ? '登录中…' : '登录' }}
            </el-button>
          </el-form>
          <p class="login-footer">Copyright © 2024 Nexora Admin</p>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import router from '@/router'
import type { FormInstance } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Histogram, Lock, Monitor, Moon, Sunny, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/modules/user'
import { useSettingsStore } from '@/store/modules/settings'
import type { LoginParams } from '@/api/system/auth'
import Logo from '@/layouts/components/Sidebar/Logo.vue'

const userStore = useUserStore()
const settingsStore = useSettingsStore()
const loginFormRef = ref<FormInstance>()
const loading = ref(false)
const loginForm = reactive<LoginParams>({ username: '', password: '', rememberMe: false, source: 'ADMIN' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }, { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }]
}
const handleForgotPassword = () => {
  ElMessage.info('请联系系统管理员重置密码')
}
const isDark = computed(() => settingsStore.theme === 'dark')
const toggleTheme = () => settingsStore.saveSettings({ theme: isDark.value ? 'light' : 'dark' })
const handleLogin = async () => {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate()
  loading.value = true
  userStore.login(loginForm).then(() => { router.push('/'); ElMessage.success('登录成功') }).catch((err) => { console.error('Login failed:', err) }).finally(() => { loading.value = false })
}
</script>

<style lang="scss" scoped>
.login-container { min-height: 100vh; background: var(--el-bg-color-page); color: var(--el-text-color-primary); }
.split-screen { min-height: 100vh; display: grid; grid-template-columns: minmax(0, 1.15fr) minmax(420px, .85fr); }
.brand-section { position: relative; overflow: hidden; display: grid; place-items: center; padding: 48px; color: #eff6ff; background: #102a43; }
.brand-content { position: relative; z-index: 1; max-width: 500px; }
.brand-mark { display: inline-flex; align-items: center; gap: 10px; margin-bottom: 40px; color: #fff; font-weight: 700; letter-spacing: 0; }
.brand-mark span { font-size: 14px; }
.brand-accent { position: absolute; top: -20%; left: 68%; width: 1px; height: 145%; background: #38bdf8; opacity: .24; transform: rotate(30deg); transform-origin: center; }
.eyebrow { margin: 0 0 10px; color: var(--el-color-primary); font-size: 12px; font-weight: 700; letter-spacing: 0; }
.brand-section .eyebrow { color: #bfdbfe; }
h1, h2 { margin: 0; letter-spacing: 0; }
h1 { font-size: clamp(36px, 4vw, 56px); }
.brand-description { max-width: 360px; margin: 18px 0 42px; color: #cbd5e1; font-size: 17px; line-height: 1.7; }
.feature-list { display: grid; gap: 14px; }
.feature-list div { display: flex; align-items: center; gap: 12px; color: #dbeafe; }
.feature-list .el-icon { color: #93c5fd; }
.brand-meta { display: flex; align-items: center; gap: 9px; margin-top: 54px; color: #9fb3c8; font-size: 12px; }
.status-dot { width: 7px; height: 7px; border-radius: 50%; background: #34d399; box-shadow: 0 0 0 4px rgba(52,211,153,.13); }
.meta-divider { width: 1px; height: 12px; background: rgba(203,213,225,.28); }
.login-section { display: grid; place-items: center; padding: 48px; background: var(--el-bg-color); }
.login-box { width: min(100%, 380px); }.login-heading { margin-bottom: 32px; }.login-heading h2 { font-size: 30px; }.login-heading > p:last-child { margin: 10px 0 0; color: var(--el-text-color-secondary); }
:deep(.el-form-item__label) { margin-bottom: 7px; color: var(--el-text-color-primary); font-size: 13px; font-weight: 600; line-height: 1.4; }
:deep(.el-input__wrapper) { min-height: 48px; padding: 0 14px; border: 1px solid var(--el-border-color); border-radius: 9px; box-shadow: none; transition: border-color .2s ease, box-shadow .2s ease; }
:deep(.el-input__wrapper.is-focus) { border-color: var(--el-color-primary); box-shadow: 0 0 0 3px var(--el-color-primary-light-8); }
.login-options { display: flex; justify-content: space-between; align-items: center; margin: 2px 0 24px; font-size: 13px; }.forgot-password { padding: 0; border: 0; color: var(--el-color-primary); background: transparent; font: inherit; cursor: pointer; }.forgot-password:hover { text-decoration: underline; }.forgot-password:focus-visible { border-radius: 4px; outline: 3px solid var(--el-color-primary-light-8); outline-offset: 3px; }
.login-button { width: 100%; min-height: 48px; font-weight: 600; }.login-footer { margin: 32px 0 0; color: var(--el-text-color-placeholder); font-size: 12px; text-align: center; }
.theme-toggle { position: fixed; z-index: 5; top: 20px; right: 20px; display: grid; width: 44px; height: 44px; place-items: center; color: var(--el-text-color-secondary); border: 1px solid var(--el-border-color); border-radius: 9px; background: var(--el-bg-color); cursor: pointer; transition: color .2s ease, border-color .2s ease, background .2s ease; }.theme-toggle:hover { color: var(--el-color-primary); border-color: var(--el-color-primary); }.theme-toggle:focus-visible { outline: 3px solid var(--el-color-primary-light-8); outline-offset: 3px; }
.dark .brand-section { background: #0b1f33; }.dark .login-section { background: var(--el-bg-color-page); }
@media (max-width: 900px) { .split-screen { grid-template-columns: 1fr; }.brand-section { display: none; }.login-section { min-height: 100vh; }.theme-toggle { background: var(--el-bg-color); } }
@media (max-width: 480px) { .login-section { padding: 28px 24px; }.login-heading h2 { font-size: 26px; } }
@media (prefers-reduced-motion: reduce) { *, *::before, *::after { scroll-behavior: auto !important; transition-duration: .01ms !important; animation-duration: .01ms !important; } }
</style>
