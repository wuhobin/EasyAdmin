<template>
  <div
    v-if="isLocked"
    ref="lockDialogRef"
    class="lock-screen"
    role="dialog"
    aria-modal="true"
    aria-labelledby="lock-screen-title"
    @keydown="trapFocus"
  >
    <div class="background-overlay" aria-hidden="true"></div>
    <div class="lock-box">
      <div class="time-display">
        <div class="time">{{ currentTime }}</div>
        <div class="date">{{ currentDate }}</div>
      </div>
      
      <div class="user-info">
        <div class="avatar-wrapper">
          <el-avatar 
            :size="90" 
            :src="userStore.user?.avatar || undefined"
          />
          <div class="avatar-border"></div>
        </div>
        <h2 id="lock-screen-title" class="welcome">欢迎回来，{{ userStore.user?.nickname || '用户' }}</h2>
      </div>

      <div class="input-box">
        <el-input
          v-model="password"
          ref="passwordInputRef"
          name="lock-password"
          aria-label="登录密码"
          type="password"
          autocomplete="current-password"
          placeholder="请输入密码解锁"
          prefix-icon="Lock"
          @keyup.enter="handleUnlock"
          class="custom-input"
        >
          <template #append>
            <el-button 
              type="primary" 
              :loading="loading" 
              @click="handleUnlock"
              class="unlock-button"
            >
              <el-icon v-if="!loading"><Unlock /></el-icon>
              <span>解锁</span>
            </el-button>
          </template>
        </el-input>
        <div v-if="errorMessage" class="error-message" role="alert" aria-live="assertive">
          {{ errorMessage }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '@/store/modules/user'
import { ElMessage } from 'element-plus'
import { verifyPassword } from '@/api/system/user'

const userStore = useUserStore()
const password = ref('')
const isLocked = ref(sessionStorage.getItem('isLocked') === 'true')
const loading = ref(false)

// 时间相关
const currentTime = ref('')
const currentDate = ref('')
let timer: ReturnType<typeof setInterval> | undefined
const lockDialogRef = ref<HTMLElement>()
const passwordInputRef = ref()

const timeFormatter = new Intl.DateTimeFormat('zh-CN', {
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hour12: false
})
const dateFormatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
})

const updateTime = () => {
  const now = new Date()
  currentTime.value = timeFormatter.format(now)
  currentDate.value = dateFormatter.format(now)
}

const setPageInert = (locked: boolean) => {
  const page = document.querySelector<HTMLElement>('.layout-container')
  if (locked) page?.setAttribute('inert', '')
  else page?.removeAttribute('inert')
}

const trapFocus = (event: KeyboardEvent) => {
  if (event.key !== 'Tab') return
  const focusable = lockDialogRef.value?.querySelectorAll<HTMLElement>('input, button, [href], [tabindex]:not([tabindex="-1"])')
  if (!focusable?.length) return
  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  setPageInert(false)
})

watch(isLocked, async (locked) => {
  setPageInert(locked)
  if (locked) {
    await nextTick()
    passwordInputRef.value?.focus()
  }
}, { immediate: true })

const errorMessage = ref('')

const handleUnlock = async () => {
  if (!password.value) {
    errorMessage.value = '请输入密码'
    return
  }
  
  loading.value = true
  errorMessage.value = ''
  
  try {
    const {data} = await verifyPassword(password.value)
    if (data) {
      isLocked.value = false
      sessionStorage.removeItem('isLocked')
      password.value = ''
      ElMessage({
        message: '解锁成功',
        type: 'success',
        customClass: 'lock-screen-message'
      })
    } else {
      errorMessage.value = '密码错误'
      // 密码错误时添加抖动效果
      const inputEl = document.querySelector('.custom-input')
      inputEl?.classList.add('shake')
      setTimeout(() => {
        inputEl?.classList.remove('shake')
      }, 500)
    }
  } catch (error) {
    errorMessage.value = '验证失败，请重试'
  } finally {
    loading.value = false
  }
}

const lock = () => {
  isLocked.value = true
  sessionStorage.setItem('isLocked', 'true')
  password.value = ''
}

defineExpose({
  lock
})
</script>

<style scoped>
.lock-screen {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: url('/src/assets/lock-bg.jpg');
  background-size: cover;
  background-position: center;
  z-index: 2000;
  display: flex;
  justify-content: center;
  align-items: center;
}

.background-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    135deg,
    rgba(0, 0, 0, 0.7) 0%,
    rgba(0, 0, 0, 0.5) 100%
  );
  backdrop-filter: blur(10px);
}

.lock-box {
  position: relative;
  text-align: center;
  padding: 40px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.1);
  animation: fadeIn 0.5s ease;
  min-width: 400px;
}

.time-display {
  margin-bottom: 40px;
  color: white;
}

.time {
  font-size: 56px;
  font-weight: 200;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  letter-spacing: 2px;
}

.date {
  font-size: 16px;
  opacity: 0.9;
  margin-top: 8px;
  letter-spacing: 1px;
}

.user-info {
  margin-bottom: 40px;
}

.avatar-wrapper {
  position: relative;
  display: inline-block;
  padding: 4px;
}

.avatar-border {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border: 3px solid rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  animation: rotate 8s linear infinite;
}

.welcome {
  color: white;
  margin: 20px 0;
  font-size: 24px;
  font-weight: 300;
  letter-spacing: 1px;
}

.input-box {
  width: 340px;
  margin: 0 auto;
}

:deep(.el-input-group__append) {
  padding: 0;
  background: transparent;
}

:deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.15);
  box-shadow: none !important;
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: border-color 0.3s, background-color 0.3s, box-shadow 0.3s;
}

:deep(.el-input__wrapper:hover) {
  border-color: rgba(255, 255, 255, 0.3);
}

:deep(.el-input__inner) {
  color: white;
  height: 45px;
  font-size: 15px;
}

:deep(.el-input__prefix-inner) {
  color: rgba(255, 255, 255, 0.7);
}

:deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.6);
}

.unlock-button {
  height: 45px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 头像悬浮效果 */
.el-avatar {
  border: 3px solid rgba(255, 255, 255, 0.2);
  transition: border-color 0.3s ease, transform 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.avatar-wrapper:hover .el-avatar {
  transform: scale(1.05);
  border-color: rgba(255, 255, 255, 0.4);
}

/* 输入框聚焦效果 */
:deep(.el-input__wrapper:focus-within) {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 1px var(--el-color-primary) !important;
}

/* 解锁按钮悬浮效果 */
:deep(.unlock-button:not(.is-loading):hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

/* 密码错误抖动效果 */
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-4px); }
  20%, 40%, 60%, 80% { transform: translateX(4px); }
}

.shake {
  animation: shake 0.5s ease-in-out;
}

/* 暗色主题适配 */
@media (prefers-color-scheme: dark) {
  .lock-box {
    background: rgba(0, 0, 0, 0.3);
  }
}

@media (prefers-reduced-motion: reduce) {
  .lock-box,
  .el-avatar,
  :deep(.el-input__wrapper) { animation: none; transition: none; }
}

/* 添加错误消息样式 */
.error-message {
  color: #ff4949;
  font-size: 14px;
  margin-top: 8px;
  text-align: left;
  padding: 0 4px;
  min-height: 20px;
}
</style>

<style>
/* 全局样式，确保消息能显示在锁屏之上 */
.lock-screen-message {
  z-index: 2100 !important;
}

.el-message {
  z-index: 2100 !important;
}
</style>
