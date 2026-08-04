<template>
  <div v-show="showWatermark" class="watermark-container">
    <canvas ref="watermarkRef" class="watermark-canvas" />
    <div ref="wrapperRef" class="watermark-wrapper" />
  </div>
</template>

<script setup lang="ts">
import { useSettingsStore, useUserStore } from '@/store'
import { usePublicConfigStore } from '@/store/modules/publicConfig'

const settingsStore = useSettingsStore()
const userStore = useUserStore()
const publicConfigStore = usePublicConfigStore()
const watermarkRef = ref<HTMLCanvasElement | null>(null)
const wrapperRef = ref<HTMLDivElement | null>(null)

const systemConfig = computed(() => publicConfigStore.system)
const showWatermark = computed(() =>
  systemConfig.value.watermarkEnabled || settingsStore.watermark
)

function currentUserName() {
  return userStore.user.nickname || userStore.user.email || 'Nexora User'
}

function watermarkLines() {
  switch (systemConfig.value.watermarkType) {
    case 'username':
      return [currentUserName()]
    case 'sitename':
      return [systemConfig.value.siteName]
    case 'custom':
      return [systemConfig.value.watermarkCustomText || systemConfig.value.siteName]
    default:
      return [currentUserName(), new Intl.DateTimeFormat('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
      }).format(new Date())]
  }
}

function createWatermark() {
  const canvas = watermarkRef.value
  const wrapper = wrapperRef.value
  if (!canvas || !wrapper || !showWatermark.value) return
  const context = canvas.getContext('2d')
  if (!context) return

  canvas.width = 360
  canvas.height = 190
  context.clearRect(0, 0, canvas.width, canvas.height)
  context.save()
  context.translate(30, 120)
  context.rotate(-15 * Math.PI / 180)
  const color = settingsStore.theme === 'dark' ? '255, 255, 255' : '15, 23, 42'
  context.fillStyle = `rgba(${color}, ${systemConfig.value.watermarkOpacity})`
  context.font = '14px Inter, "Microsoft YaHei", sans-serif'
  watermarkLines().forEach((line, index) => context.fillText(line, 0, index * 22))
  context.restore()
  wrapper.style.backgroundImage = `url(${canvas.toDataURL('image/png')})`
}

watch(
  [
    showWatermark,
    () => settingsStore.theme,
    () => systemConfig.value.watermarkType,
    () => systemConfig.value.watermarkCustomText,
    () => systemConfig.value.watermarkOpacity,
    () => systemConfig.value.siteName,
    () => userStore.user.nickname,
    () => userStore.user.email
  ],
  () => nextTick(createWatermark),
  { immediate: true }
)

let timer: number | undefined
onMounted(() => {
  createWatermark()
  timer = window.setInterval(createWatermark, 60_000)
})

onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<style scoped>
.watermark-container {
  position: fixed;
  z-index: 3000;
  inset: 0;
  pointer-events: none;
}

.watermark-canvas {
  display: none;
}

.watermark-wrapper {
  width: 100%;
  height: 100%;
  background-repeat: repeat;
  pointer-events: none;
}
</style>
