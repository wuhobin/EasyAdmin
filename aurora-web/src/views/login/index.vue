<template>
  <div class="login-container" :class="{ dark: isDark }">
    <button class="theme-toggle" type="button" :aria-label="isDark ? '切换到浅色模式' : '切换到深色模式'" @click="toggleTheme">
      <el-icon><component :is="isDark ? Sunny : Moon" /></el-icon>
    </button>
    <main class="split-screen">
      <section class="brand-section">
        <div class="brand-orb orb-one"></div>
        <div class="brand-orb orb-two"></div>
        <div class="brand-grid"></div>
        <div class="brand-content">
          <div class="brand-logo"><Logo :size="54" /></div>
          <p class="eyebrow">AURORA ADMIN</p>
          <h1>{{ settingsStore.title }}</h1>
          <p class="brand-description">清晰、高效、安全的企业管理工作台</p>
          <div class="feature-list">
            <div><el-icon><Monitor /></el-icon><span>统一工作台</span></div>
            <div><el-icon><Lock /></el-icon><span>安全权限体系</span></div>
            <div><el-icon><Histogram /></el-icon><span>运行状态洞察</span></div>
          </div>
        </div>
      </section>
      <section class="login-section">
        <div class="login-box">
          <div class="login-heading">
            <p class="eyebrow">WELCOME BACK</p>
            <h2>登录管理后台</h2>
            <p>请输入你的账号信息继续。</p>
          </div>
          <el-form ref="loginFormRef" :model="loginForm" :rules="rules" @keyup.enter="handleLogin">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="用户名" :prefix-icon="User" size="large" autocomplete="username" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password size="large" autocomplete="current-password" />
            </el-form-item>
            <div class="login-options">
              <el-checkbox v-model="loginForm.rememberMe">记住我</el-checkbox>
              <a href="#" @click.prevent>忘记密码？</a>
            </div>
            <el-button :loading="loading" type="primary" size="large" class="login-button" @click="handleLogin">
              {{ loading ? '登录中...' : '登录' }}
            </el-button>
          </el-form>
          <p class="login-footer">Copyright © 2024 Aurora Admin</p>
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
const isDark = computed(() => settingsStore.theme === 'dark')
const toggleTheme = () => settingsStore.saveSettings({ theme: isDark.value ? 'light' : 'dark' })
const handleLogin = async () => {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate()
  loading.value = true
  userStore.login(loginForm).then(() => { router.push('/'); ElMessage.success('登录成功') }).catch(() => {}).finally(() => { loading.value = false })
}
</script>

<style lang="scss" scoped>
.login-container { min-height: 100vh; background: var(--el-bg-color-page); color: var(--el-text-color-primary); }
.split-screen { min-height: 100vh; display: grid; grid-template-columns: minmax(0, 1.15fr) minmax(420px, .85fr); }
.brand-section { position: relative; overflow: hidden; display: grid; place-items: center; padding: 48px; color: #eff6ff; background: #172554; }
.brand-content { position: relative; z-index: 1; max-width: 500px; }
.brand-logo { width: 78px; height: 78px; display: grid; place-items: center; margin-bottom: 38px; color: #fff; border: 1px solid rgba(255,255,255,.22); border-radius: 20px; background: rgba(255,255,255,.10); }
.eyebrow { margin: 0 0 10px; color: var(--el-color-primary); font-size: 12px; font-weight: 700; letter-spacing: .12em; }
.brand-section .eyebrow { color: #bfdbfe; }
h1, h2 { margin: 0; letter-spacing: -.03em; }
h1 { font-size: clamp(36px, 4vw, 56px); }
.brand-description { max-width: 360px; margin: 18px 0 42px; color: #cbd5e1; font-size: 17px; line-height: 1.7; }
.feature-list { display: grid; gap: 14px; }
.feature-list div { display: flex; align-items: center; gap: 12px; color: #dbeafe; }
.feature-list .el-icon { color: #93c5fd; }
.brand-orb { position: absolute; width: 440px; height: 440px; border-radius: 50%; filter: blur(8px); opacity: .25; background: var(--el-color-primary); }
.orb-one { top: -220px; right: -120px; }.orb-two { bottom: -280px; left: -140px; width: 520px; height: 520px; background: #38bdf8; }
.brand-grid { position: absolute; inset: 0; opacity: .12; background-image: linear-gradient(#bfdbfe 1px, transparent 1px), linear-gradient(90deg, #bfdbfe 1px, transparent 1px); background-size: 36px 36px; mask-image: linear-gradient(to bottom, transparent, black 35%, transparent); }
.login-section { display: grid; place-items: center; padding: 48px; background: var(--el-bg-color); }
.login-box { width: min(100%, 380px); }.login-heading { margin-bottom: 32px; }.login-heading h2 { font-size: 30px; }.login-heading > p:last-child { margin: 10px 0 0; color: var(--el-text-color-secondary); }
:deep(.el-input__wrapper) { min-height: 48px; padding: 0 14px; border: 1px solid var(--el-border-color); border-radius: 9px; box-shadow: none; transition: border-color .2s ease, box-shadow .2s ease; }
:deep(.el-input__wrapper.is-focus) { border-color: var(--el-color-primary); box-shadow: 0 0 0 3px var(--el-color-primary-light-8); }
.login-options { display: flex; justify-content: space-between; align-items: center; margin: 2px 0 24px; font-size: 13px; }.login-options a { color: var(--el-color-primary); text-decoration: none; }
.login-button { width: 100%; min-height: 48px; font-weight: 600; }.login-footer { margin: 32px 0 0; color: var(--el-text-color-placeholder); font-size: 12px; text-align: center; }
.theme-toggle { position: fixed; z-index: 5; top: 20px; right: 20px; display: grid; width: 36px; height: 36px; place-items: center; color: var(--el-text-color-secondary); border: 1px solid var(--el-border-color); border-radius: 9px; background: var(--el-bg-color); cursor: pointer; }.theme-toggle:hover { color: var(--el-color-primary); border-color: var(--el-color-primary); }
.dark .brand-section { background: #0f172a; }.dark .login-section { background: var(--el-bg-color-page); }
@media (max-width: 900px) { .split-screen { grid-template-columns: 1fr; }.brand-section { display: none; }.login-section { min-height: 100vh; }.theme-toggle { background: var(--el-bg-color); } }
@media (max-width: 480px) { .login-section { padding: 28px 24px; }.login-heading h2 { font-size: 26px; } }
</style>
