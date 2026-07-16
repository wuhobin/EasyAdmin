<template>
  <div class="login-container" :class="{ 'dark': isDark }">
    <!-- 3D动态背景 -->
    <div class="geometric-background">
      <div class="gradient-sphere"></div>
      <div class="geometric-box" v-for="n in 5" :key="n"></div>
    </div>
    
    <!-- 右上角操作按钮 -->
    <div class="action-buttons">
      <el-button class="glass-button" circle @click="toggleTheme">
        <el-icon><component :is="isDark ? 'Sunny' : 'Moon'" /></el-icon>
      </el-button>
    </div>

    <!-- 分屏布局 -->
    <div class="split-screen">
      <!-- 左侧品牌展示区 -->
      <div class="brand-section">
        <!-- 添加动态背景图形 -->
        <div class="brand-background">
          <div class="circle-container">
            <div class="circle"></div>
            <div class="circle"></div>
            <div class="circle"></div>
          </div>
          <div class="grid-lines"></div>
        </div>

        <div class="brand-content">
          <div class="logo-wrapper">
            <Logo :size="80" class="floating-logo" :color="logoColor" />
            <div class="logo-glow"></div>
          </div>
          
          <h1 class="brand-title">{{ settings.title }}</h1>
          <p class="brand-description">基于 Vue3 + TypeScript 开箱即用的企业级中后台解决方案</p>
          
          <!-- 修改特性列表的展示方式 -->
          <div class="feature-list">
            <div class="feature-item">
              <div class="feature-icon">
                <el-icon><Monitor /></el-icon>
              </div>
              <div class="feature-info">
                <h3>简洁优雅</h3>
                <p>清晰的界面设计</p>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-icon">
                <el-icon><Lock /></el-icon>
              </div>
              <div class="feature-info">
                <h3>安全可靠</h3>
                <p>内置权限管理系统</p>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-icon">
                <el-icon><Histogram /></el-icon>
              </div>
              <div class="feature-info">
                <h3>高性能</h3>
                <p>优化的代码结构</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧登录区 -->
      <div class="login-section">
        <div class="login-box">
          <div class="login-title">
            <el-icon><User /></el-icon>
            <span>账号登录</span>
          </div>

          <!-- 登录表单 -->
          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="rules"
            @keyup.enter="handleLogin"
          >
            <el-form-item prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名"
                prefix-icon="User"
                class="login-input"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                prefix-icon="Lock"
                show-password
                class="login-input"
              />
            </el-form-item>
            <div class="login-options">
              <el-checkbox v-model="loginForm.rememberMe">记住我</el-checkbox>
              <a href="#" class="forget-password">忘记密码？</a>
            </div>
            <el-form-item>
              <el-button
                :loading="loading"
                type="primary"
                class="login-button"
                @click="handleLogin"
              >
                {{ loading ? '登录中...' : '登录' }}
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>

    <!-- 页脚版权信息 -->
    <footer class="footer">
      <div class="footer-content">
        <p>Copyright © 2024 Aurora-Admin</p>
        <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener">
          湘ICP备2022002110号-1
        </a>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import router from "@/router";
import type { FormInstance } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import { useSettingsStore } from '@/store/modules/settings'
import type { LoginParams } from '@/api/system/auth'
import Logo from '@/layouts/components/Sidebar/Logo.vue'
import settings from '@/config/settings'
const userStore = useUserStore()
const settingsStore = useSettingsStore()
const loginFormRef = ref<FormInstance>()
const loading = ref(false)
const loginForm = reactive<LoginParams>({
  username: '',
  password: '',
  rememberMe: false,
  source: 'ADMIN'
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

const isDark = computed(() => settingsStore.theme === 'dark')

const toggleTheme = () => {
  const newTheme = isDark.value ? 'light' : 'dark'
  settingsStore.saveSettings({ theme: newTheme })
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate()
  loading.value = true
  userStore.login(loginForm).then(() => {
    router.push('/');
    ElMessage.success('登录成功')
  }).catch(() => {}).finally(() => {
    loading.value = false;
  });

}

// 添加 logo 颜色计算
const logoColor = computed(() => {
  return isDark.value ? '#4ecdc4' : '#ff6b6b'
})
</script>

<style lang="scss" scoped>
.login-container {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background: var(--el-bg-color);
}

/* 3D几何背景 */
.geometric-background {
  position: fixed;
  width: 100%;
  height: 100%;
  perspective: 1000px;
  z-index: 0;
}

.gradient-sphere {
  position: absolute;
  width: 600px;
  height: 600px;
  left: -200px;
  top: -200px;
  border-radius: 50%;
  background: linear-gradient(45deg, #ff6b6b, #4ecdc4);
  filter: blur(80px);
  opacity: 0.5;
  animation: float 10s ease-in-out infinite;
}

.geometric-box {
  position: absolute;
  width: 100px;
  height: 100px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(5px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 15px;
  transform-style: preserve-3d;
  animation: rotate3d 20s linear infinite;
}

.geometric-box:nth-child(1) { top: 10%; left: 10%; }
.geometric-box:nth-child(2) { top: 20%; right: 15%; }
.geometric-box:nth-child(3) { bottom: 15%; left: 20%; }
.geometric-box:nth-child(4) { bottom: 25%; right: 10%; }
.geometric-box:nth-child(5) { top: 50%; left: 50%; }

@keyframes rotate3d {
  0% { transform: rotate3d(1, 1, 1, 0deg); }
  100% { transform: rotate3d(1, 1, 1, 360deg); }
}

/* 分屏布局 */
.split-screen {
  display: flex;
  min-height: 100vh;
  position: relative;
  z-index: 1;
  background: linear-gradient(to right, 
    rgba(240, 244, 248, 0.95), 
    rgba(255, 255, 255, 0.98)
  );
}

/* 左侧品牌区样式优化 */
.brand-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  position: relative;
  background: linear-gradient(135deg, 
    rgba(64, 158, 255, 0.05), 
    rgba(83, 82, 237, 0.05)
  );
  backdrop-filter: blur(10px);
  border-right: 1px solid rgba(255, 255, 255, 0.5);
  overflow: hidden;
}

/* 添加动态背景 */
.brand-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
}

.circle-container {
  position: absolute;
  width: 100%;
  height: 100%;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: linear-gradient(45deg, rgba(64, 158, 255, 0.1), rgba(83, 82, 237, 0.1));
  animation: circleFloat 20s infinite ease-in-out;
}

.circle:nth-child(1) {
  width: 300px;
  height: 300px;
  top: -150px;
  left: -150px;
  animation-delay: 0s;
}

.circle:nth-child(2) {
  width: 400px;
  height: 400px;
  bottom: -200px;
  right: -200px;
  animation-delay: -5s;
}

.circle:nth-child(3) {
  width: 200px;
  height: 200px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation-delay: -10s;
}

@keyframes circleFloat {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  25% {
    transform: translate(50px, -30px) scale(1.1);
  }
  50% {
    transform: translate(0, 50px) scale(0.9);
  }
  75% {
    transform: translate(-50px, -20px) scale(1.05);
  }
}

.grid-lines {
  position: absolute;
  width: 200%;
  height: 200%;
  top: -50%;
  left: -50%;
  background-image: 
    linear-gradient(rgba(64, 158, 255, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(64, 158, 255, 0.05) 1px, transparent 1px);
  background-size: 30px 30px;
  transform: perspective(500px) rotateX(60deg);
  animation: gridMove 20s linear infinite;
}

@keyframes gridMove {
  0% {
    transform: perspective(500px) rotateX(60deg) translateY(0);
  }
  100% {
    transform: perspective(500px) rotateX(60deg) translateY(30px);
  }
}

/* Logo样式优化 */
.logo-wrapper {
  position: relative;
  margin-bottom: 20px;
}

.logo-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100px;
  height: 100px;
  background: radial-gradient(circle, rgba(64, 158, 255, 0.2), transparent 70%);
  animation: glowPulse 2s infinite ease-in-out;
}

@keyframes glowPulse {
  0%, 100% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 0.5;
  }
  50% {
    transform: translate(-50%, -50%) scale(1.5);
    opacity: 0.2;
  }
}

/* 品牌内容样式优化 */
.brand-content {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 600px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.1),
    inset 0 0 32px rgba(255, 255, 255, 0.2);
  transition: transform 0.3s ease;
}

.brand-content:hover {
  transform: translateY(-5px);
}

/* 特性列表样式优化 */
.feature-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-top: 40px;
}

.feature-item {
  padding: 20px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
  cursor: pointer;
}

.feature-item:hover {
  transform: translateY(-5px);
  background: rgba(255, 255, 255, 0.15);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.feature-icon {
  width: 50px;
  height: 50px;
  margin: 0 auto 15px;
  background: rgba(64, 158, 255, 0.1);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: var(--el-color-primary);
  transition: all 0.3s ease;
}

.feature-item:hover .feature-icon {
  transform: scale(1.1);
  background: rgba(64, 158, 255, 0.2);
}

.feature-info h3 {
  font-size: 16px;
  margin: 0 0 8px;
  color: var(--el-text-color-primary);
}

.feature-info p {
  font-size: 14px;
  margin: 0;
  color: var(--el-text-color-secondary);
  opacity: 0.8;
}

/* 深色模式适配 */
.dark .brand-section {
  background: linear-gradient(135deg, 
    rgba(64, 158, 255, 0.05), 
    rgba(83, 82, 237, 0.05)
  );
}

.dark .grid-lines {
  background-image: 
    linear-gradient(rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px);
}

.dark .feature-item {
  background: rgba(0, 0, 0, 0.2);
}

.dark .feature-item:hover {
  background: rgba(0, 0, 0, 0.3);
}

.dark .feature-icon {
  background: rgba(64, 158, 255, 0.15);
}

.dark .feature-item:hover .feature-icon {
  background: rgba(64, 158, 255, 0.25);
}

/* 右侧登录区域整体优化 */
.login-section {
  width: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: linear-gradient(145deg,
    rgba(255, 255, 255, 0.6),
    rgba(255, 255, 255, 0.9)
  );
  position: relative;
  overflow: hidden;
  box-shadow: -10px 0 20px rgba(0, 0, 0, 0.03);

  .dark & {
    background: linear-gradient(145deg,
      rgba(30, 35, 45, 0.8),
      rgba(20, 25, 35, 0.9)
    );
  }
}

/* 添加动态光效背景 */
.login-section::before,
.login-section::after {
  content: '';
  position: absolute;
  width: 500px;
  height: 500px;
  border-radius: 50%;
  background: linear-gradient(
    45deg,
    rgba(64, 158, 255, 0.15),
    rgba(83, 82, 237, 0.15)
  );
  animation: rotate 10s linear infinite;
}

.login-section::before {
  top: -250px;
  right: -250px;
}

.login-section::after {
  bottom: -250px;
  left: -250px;
  animation-delay: -5s;
}

@keyframes rotate {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

/* 登录框样式优化 */
.login-box {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 400px;
  padding: 35px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 
    0 15px 35px rgba(0, 0, 0, 0.1),
    0 3px 10px rgba(0, 0, 0, 0.05),
    inset 0 0 30px rgba(255, 255, 255, 0.5);
  transition: all 0.3s ease;

  .dark & {
    background: rgba(30, 35, 45, 0.9);
    border-color: rgba(255, 255, 255, 0.1);
    box-shadow: 
      0 15px 35px rgba(0, 0, 0, 0.2),
      0 3px 10px rgba(0, 0, 0, 0.1),
      inset 0 0 30px rgba(255, 255, 255, 0.05);
  }
}

/* 登录标题 */
.login-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 35px;
  color: var(--el-text-color-primary);
  font-size: 20px;
  font-weight: 600;
}

.login-title .el-icon {
  color: var(--el-color-primary);
  font-size: 22px;
}
/* 输入框样式优化 */
:deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  height: 50px;
  padding: 0 20px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(0, 0, 0, 0.1) !important;
  box-shadow: 
    0 2px 6px rgba(0, 0, 0, 0.05),
    inset 0 1px 2px rgba(0, 0, 0, 0.05);
  transform: none !important;
}

:deep(.el-input__wrapper:hover) {
  border-color: var(--el-color-primary) !important;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 
    0 4px 12px rgba(0, 0, 0, 0.08),
    inset 0 1px 2px rgba(0, 0, 0, 0.05);
}

:deep(.el-input__wrapper.is-focus) {
  border-color: var(--el-color-primary) !important;
  border-width: 2px !important;
  background: white;
  box-shadow: 
    0 4px 12px rgba(64, 158, 255, 0.1),
    inset 0 1px 2px rgba(0, 0, 0, 0.05);
  animation: none;
}

/* 深色模式下的输入框样式 */
.dark {
  :deep(.el-input__wrapper) {
    background: rgba(0, 0, 0, 0.2);
    border-color: rgba(255, 255, 255, 0.1) !important;
    box-shadow: 
      0 2px 6px rgba(0, 0, 0, 0.2),
      inset 0 1px 2px rgba(0, 0, 0, 0.2);

    &:hover {
      background: rgba(0, 0, 0, 0.25);
      border-color: var(--el-color-primary) !important;
    }

    &.is-focus {
      background: rgba(0, 0, 0, 0.3);
      border-color: var(--el-color-primary) !important;
      box-shadow: 
        0 4px 12px rgba(64, 158, 255, 0.15),
        inset 0 1px 2px rgba(0, 0, 0, 0.2);
    }

    .el-input__inner {
      color: var(--el-text-color-primary);
      
      &::placeholder {
        color: var(--el-text-color-placeholder);
      }
    }
  }
}

/* 输入框图标样式优化 */
:deep(.el-input__prefix-inner) {
  font-size: 18px;
  color: var(--el-text-color-secondary);
  margin-right: 8px;
}

/* 输入框文本样式 */
:deep(.el-input__inner) {
  font-size: 15px;
  color: var(--el-text-color-primary);
  &::placeholder {
    color: var(--el-text-color-placeholder);
  }
}

/* 登录按钮样式优化 */
.login-button {
  width: 100%;
  height: 50px;
  background: linear-gradient(45deg, #409eff, #5352ed) !important;
  border: none !important;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 2px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 14px !important;
  position: relative;
  overflow: hidden;
}

.login-button::after {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.3) 0%, transparent 70%);
  transform: scale(0);
  opacity: 0;
  transition: transform 0.6s, opacity 0.6s;
}

.login-button:hover::after {
  transform: scale(1);
  opacity: 1;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(83, 82, 237, 0.3);
}

/* 记住我选项美化 */
.login-options {
  margin: 20px 0 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

:deep(.el-checkbox__inner) {
  border-radius: 6px;
  transition: all 0.3s ease;
}

.forget-password {
  color: var(--el-color-primary);
  font-size: 14px;
  text-decoration: none;
  position: relative;
  transition: all 0.3s ease;
}

.forget-password::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 100%;
  height: 1px;
  background: var(--el-color-primary);
  transform: scaleX(0);
  transition: transform 0.3s ease;
}

.forget-password:hover::after {
  transform: scaleX(1);
}

/* 深色模式适配优化 */
.dark {
  .login-section {
    background: linear-gradient(145deg,
      rgba(30, 35, 45, 0.8),
      rgba(20, 25, 35, 0.9)
    );
  }

  .login-box {
    background: rgba(30, 35, 45, 0.9);
    border-color: rgba(255, 255, 255, 0.1);
    box-shadow: 
      0 15px 35px rgba(0, 0, 0, 0.2),
      0 3px 10px rgba(0, 0, 0, 0.1),
      inset 0 0 30px rgba(255, 255, 255, 0.05);
  }

  :deep(.el-input__wrapper) {
    background: rgba(0, 0, 0, 0.2);
    box-shadow: 
      0 4px 12px rgba(0, 0, 0, 0.1),
      inset 0 2px 4px rgba(0, 0, 0, 0.2);
  }

  :deep(.el-input__wrapper:hover),
  :deep(.el-input__wrapper.is-focus) {
    background: rgba(0, 0, 0, 0.3);
  }
}

/* 添加输入框聚焦动画 */
@keyframes inputFocus {
  0% { transform: scale(1); }
  50% { transform: scale(1.02); }
  100% { transform: scale(1); }
}

:deep(.el-input__wrapper.is-focus) {
  animation: inputFocus 0.3s ease-out;
}

/* 修改右上角操作按钮样式 */
.action-buttons {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 10;
  display: flex;
  gap: 12px;
}

.glass-button {
  background: rgba(255, 255, 255, 0.2) !important;
  border: none !important;
  backdrop-filter: blur(10px);
  color: var(--el-text-color-primary) !important;
  transition: all 0.3s;
}

.glass-button:hover {
  background: rgba(255, 255, 255, 0.3) !important;
  transform: translateY(-2px);
}

/* 优化表单样式 */
.login-input {
  height: 44px;
}

:deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.8);
  border-radius: 8px;
  box-shadow: none;
  border: 2px solid transparent;
  transition: all 0.3s;
}

:deep(.el-input__wrapper:hover),
:deep(.el-input__wrapper.is-focus) {
  border-color: var(--el-color-primary);
  background: rgba(255, 255, 255, 0.95);
}

/* 登录选项样式 */
.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.forget-password {
  color: var(--el-color-primary);
  text-decoration: none;
  transition: opacity 0.3s;
}

.forget-password:hover {
  opacity: 0.8;
}

/* 登录按钮样式 */
.login-button {
  width: 100%;
  height: 44px;
  background: linear-gradient(45deg, #ff6b6b, #4ecdc4) !important;
  border: none !important;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 1px;
  transition: all 0.3s;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(255, 107, 107, 0.3);
}

/* 页脚样式 */
.footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20px;
  text-align: center;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.1), transparent);
  z-index: 10;
}

.footer-content {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.footer-content a {
  color: var(--el-text-color-secondary);
  text-decoration: none;
  transition: opacity 0.3s;
}

.footer-content a:hover {
  opacity: 0.8;
}

/* 深色模式额外适配 */
.dark {
  .glass-button {
    background: rgba(0, 0, 0, 0.2) !important;
    color: white !important;
  }
  
  .glass-button:hover {
    background: rgba(0, 0, 0, 0.3) !important;
  }

  :deep(.el-input__wrapper) {
    background: rgba(0, 0, 0, 0.2);
    color: white;
  }

  :deep(.el-input__wrapper:hover),
  :deep(.el-input__wrapper.is-focus) {
    background: rgba(0, 0, 0, 0.3);
  }

}

/* 添加浮动动画 */
@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-20px);
  }
}

.floating-logo {
  animation: float 6s ease-in-out infinite;
}

.brand-title {
  background: linear-gradient(45deg, #ff6b6b, #4ecdc4);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 12px;
}

.brand-description {
  color: var(--el-text-color-secondary);
  font-size: 16px;
  margin-bottom: 40px;
}
</style>
