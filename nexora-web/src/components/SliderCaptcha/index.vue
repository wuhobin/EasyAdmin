<template>
  <el-dialog
    v-model="visible"
    class="image-captcha-dialog"
    width="360px"
    :close-on-click-modal="false"
    :close-on-press-escape="!matching"
    :show-close="!matching"
    destroy-on-close
    @closed="resetChallenge"
  >
    <template #header>
      <div class="captcha-heading">
        <span class="captcha-heading-mark" aria-hidden="true"></span>
        <div>
          <h2>安全验证</h2>
          <p>拖动拼图，确认本次注册由你发起</p>
        </div>
      </div>
    </template>

    <div class="slider-captcha" :aria-busy="loading || matching">
      <div v-if="loadError" class="captcha-load-error" role="alert">
        <span>{{ loadError }}</span>
        <el-button type="primary" plain @click="loadChallenge">重新加载</el-button>
      </div>

      <template v-else>
        <div class="captcha-toolbar">
          <span>{{ imagesReady ? '将拼图拖到缺口位置' : '正在准备验证图片' }}</span>
          <button
            class="captcha-refresh"
            type="button"
            aria-label="刷新验证码"
            :disabled="loading || matching"
            @click="loadChallenge"
          >
            <el-icon><RefreshRight /></el-icon>
          </button>
        </div>

        <div class="captcha-stage">
          <template v-if="captcha">
            <img
              ref="backgroundImageRef"
              class="captcha-background"
              :src="captcha.backgroundImage"
              alt=""
              draggable="false"
              @load="handleImageLoad"
              @error="handleImageError"
            />
            <div class="captcha-piece" :style="{ transform: `translate3d(${sliderLeft}px, 0, 0)` }">
              <img
                ref="templateImageRef"
                :src="captcha.templateImage"
                alt=""
                draggable="false"
                @load="handleImageLoad"
                @error="handleImageError"
              />
            </div>
          </template>
          <div v-if="loading || !imagesReady" class="captcha-stage-loading">
            <span class="captcha-loading-pulse" aria-hidden="true"></span>
            <span>加载验证图片…</span>
          </div>
        </div>

        <div
          ref="sliderTrackRef"
          class="captcha-track"
          :class="{ success: status === 'success', error: status === 'error' }"
        >
          <div class="captcha-track-fill" :style="{ width: `${sliderLeft + 29}px` }"></div>
          <span class="captcha-track-label">{{ trackLabel }}</span>
          <button
            ref="sliderThumbRef"
            class="captcha-thumb"
            type="button"
            :style="{ transform: `translate3d(${sliderLeft}px, 0, 0)` }"
            :disabled="!imagesReady || loading || matching || status === 'success' || status === 'error'"
            aria-label="按住并向右拖动滑块完成拼图"
            @pointerdown.prevent="handlePointerDown"
            @pointermove.prevent="handlePointerMove"
            @pointerup.prevent="handlePointerUp"
            @pointercancel.prevent="handlePointerCancel"
          >
            <span aria-hidden="true">{{ status === 'success' ? '✓' : '››' }}</span>
          </button>
        </div>

        <p class="captcha-status" :class="status" aria-live="polite">{{ statusText }}</p>
      </template>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { RefreshRight } from '@element-plus/icons-vue'
import {
  generateImageCaptchaApi,
  matchImageCaptchaApi,
  type ImageCaptchaResult,
  type ImageCaptchaTrack,
  type ImageCaptchaTrackPoint
} from '@/api/system/auth'

type CaptchaStatus = 'idle' | 'dragging' | 'matching' | 'success' | 'error'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: [captchaId: string]
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const captcha = ref<ImageCaptchaResult>()
const backgroundImageRef = ref<HTMLImageElement>()
const templateImageRef = ref<HTMLImageElement>()
const sliderTrackRef = ref<HTMLElement>()
const sliderThumbRef = ref<HTMLButtonElement>()
const loading = ref(false)
const imagesReady = ref(false)
const loadError = ref('')
const status = ref<CaptchaStatus>('idle')
const sliderLeft = ref(0)
const dragLimit = ref(0)

let requestVersion = 0
let reloadTimer: number | undefined
let activePointerId: number | undefined
let dragStartX = 0
let dragStartY = 0
let dragStartTime = 0
let trackList: ImageCaptchaTrackPoint[] = []
let renderedDimensions: Pick<
  ImageCaptchaTrack,
  'bgImageWidth' | 'bgImageHeight' | 'templateImageWidth' | 'templateImageHeight'
> | undefined

const matching = computed(() => status.value === 'matching')
const trackLabel = computed(() => {
  if (status.value === 'success') return '验证通过'
  if (status.value === 'error') return '位置不正确，正在刷新'
  if (status.value === 'matching') return '正在验证轨迹'
  return '按住滑块向右拖动'
})
const statusText = computed(() => {
  if (status.value === 'success') return '图片验证已通过'
  if (status.value === 'error') return '验证未通过，正在更换图片'
  if (status.value === 'matching') return '正在核验你的操作轨迹'
  if (status.value === 'dragging') return '松开滑块即可验证'
  return imagesReady.value ? '验证成功后会继续创建账号' : ''
})

const clearReloadTimer = () => {
  if (reloadTimer !== undefined) {
    window.clearTimeout(reloadTimer)
    reloadTimer = undefined
  }
}

const releasePointer = () => {
  if (activePointerId !== undefined && sliderThumbRef.value?.hasPointerCapture(activePointerId)) {
    sliderThumbRef.value.releasePointerCapture(activePointerId)
  }
  activePointerId = undefined
}

const resetDrag = () => {
  releasePointer()
  status.value = 'idle'
  sliderLeft.value = 0
  dragLimit.value = 0
  dragStartX = 0
  dragStartY = 0
  dragStartTime = 0
  trackList = []
  renderedDimensions = undefined
}

const resetChallenge = () => {
  requestVersion += 1
  clearReloadTimer()
  resetDrag()
  captcha.value = undefined
  imagesReady.value = false
  loadError.value = ''
  loading.value = false
}

const loadChallenge = async () => {
  const currentVersion = ++requestVersion
  clearReloadTimer()
  resetDrag()
  captcha.value = undefined
  imagesReady.value = false
  loadError.value = ''
  loading.value = true

  try {
    const { data } = await generateImageCaptchaApi()
    if (currentVersion !== requestVersion || !props.modelValue) return
    if (!data || data.type.toUpperCase() !== 'SLIDER') {
      loadError.value = '当前页面仅支持滑块验证，请联系管理员'
      return
    }
    captcha.value = data
  } catch {
    if (currentVersion === requestVersion) {
      loadError.value = '验证图片加载失败，请稍后重试'
    }
  } finally {
    if (currentVersion === requestVersion) {
      loading.value = false
    }
  }
}

const measureImages = async () => {
  await nextTick()
  const background = backgroundImageRef.value
  const template = templateImageRef.value
  const track = sliderTrackRef.value
  const thumb = sliderThumbRef.value
  if (!background?.complete || !template?.complete
    || background.naturalWidth === 0 || template.naturalWidth === 0
    || !track || !thumb) {
    return
  }

  const backgroundRect = background.getBoundingClientRect()
  const templateRect = template.getBoundingClientRect()
  const trackRect = track.getBoundingClientRect()
  const thumbRect = thumb.getBoundingClientRect()
  renderedDimensions = {
    bgImageWidth: Math.round(backgroundRect.width),
    bgImageHeight: Math.round(backgroundRect.height),
    templateImageWidth: Math.round(templateRect.width),
    templateImageHeight: Math.round(templateRect.height)
  }
  dragLimit.value = Math.max(0, Math.min(
    backgroundRect.width - templateRect.width + 5,
    trackRect.width - thumbRect.width
  ))
  imagesReady.value = true
}

const handleImageLoad = () => {
  void measureImages()
}

const handleImageError = () => {
  imagesReady.value = false
  loadError.value = '验证图片加载失败，请刷新后重试'
}

const appendTrackPoint = (event: PointerEvent, type: ImageCaptchaTrackPoint['type']) => {
  trackList.push({
    x: type === 'DOWN' ? 0 : sliderLeft.value,
    y: event.pageY - dragStartY,
    t: Math.max(0, Date.now() - dragStartTime),
    type
  })
}

const handlePointerDown = (event: PointerEvent) => {
  if (!imagesReady.value || status.value !== 'idle' || !renderedDimensions || !event.isPrimary) return
  activePointerId = event.pointerId
  dragStartX = event.pageX
  dragStartY = event.pageY
  dragStartTime = Date.now()
  trackList = []
  sliderThumbRef.value?.setPointerCapture(event.pointerId)
  appendTrackPoint(event, 'DOWN')
  status.value = 'dragging'
}

const handlePointerMove = (event: PointerEvent) => {
  if (activePointerId !== event.pointerId || status.value !== 'dragging') return
  sliderLeft.value = Math.min(Math.max(0, event.pageX - dragStartX), dragLimit.value)
  appendTrackPoint(event, 'MOVE')
}

const scheduleReload = () => {
  clearReloadTimer()
  reloadTimer = window.setTimeout(() => {
    if (props.modelValue) void loadChallenge()
  }, 800)
}

const submitTrack = async (event: PointerEvent) => {
  const currentCaptcha = captcha.value
  const dimensions = renderedDimensions
  if (!currentCaptcha || !dimensions) return

  const stopTime = Date.now()
  sliderLeft.value = Math.min(Math.max(0, event.pageX - dragStartX), dragLimit.value)
  appendTrackPoint(event, 'UP')
  if (stopTime - dragStartTime < 300 || trackList.length < 10) {
    status.value = 'error'
    scheduleReload()
    return
  }
  status.value = 'matching'
  const track: ImageCaptchaTrack = {
    ...dimensions,
    startTime: dragStartTime,
    stopTime,
    left: Math.round(sliderLeft.value),
    top: 0,
    trackList: [...trackList],
    data: currentCaptcha.data
  }

  try {
    const { data: matched } = await matchImageCaptchaApi(currentCaptcha.id, track)
    if (!props.modelValue || captcha.value?.id !== currentCaptcha.id) return
    if (matched === true) {
      status.value = 'success'
      emit('success', currentCaptcha.id)
      return
    }
  } catch {
    if (!props.modelValue || captcha.value?.id !== currentCaptcha.id) return
  }
  status.value = 'error'
  scheduleReload()
}

const handlePointerUp = (event: PointerEvent) => {
  if (activePointerId !== event.pointerId || status.value !== 'dragging') return
  releasePointer()
  void submitTrack(event)
}

const handlePointerCancel = (event: PointerEvent) => {
  if (activePointerId !== event.pointerId) return
  releasePointer()
  sliderLeft.value = 0
  trackList = []
  status.value = 'idle'
}

watch(
  () => props.modelValue,
  (isVisible) => {
    if (isVisible) {
      void loadChallenge()
    } else {
      resetChallenge()
    }
  },
  { immediate: true }
)

onBeforeUnmount(resetChallenge)
</script>

<style scoped>
:global(.image-captcha-dialog) {
  max-width: calc(100vw - 32px);
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 18px;
  background: var(--el-bg-color-overlay);
  box-shadow: 0 24px 70px rgb(15 23 42 / 22%);
}

:global(.image-captcha-dialog .el-dialog__header) {
  margin: 0;
  padding: 22px 24px 16px;
}

:global(.image-captcha-dialog .el-dialog__body) {
  padding: 0 24px 24px;
}

.captcha-heading {
  display: flex;
  align-items: center;
  gap: 12px;
}

.captcha-heading-mark {
  width: 10px;
  height: 34px;
  border-radius: 999px;
  background: linear-gradient(180deg, var(--el-color-primary), var(--el-color-primary-light-5));
  box-shadow: 0 0 0 5px var(--el-color-primary-light-9);
}

.captcha-heading h2,
.captcha-heading p {
  margin: 0;
}

.captcha-heading h2 {
  color: var(--el-text-color-primary);
  font-size: 17px;
  font-weight: 700;
  letter-spacing: .02em;
}

.captcha-heading p {
  margin-top: 3px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.slider-captcha {
  width: 100%;
}

.captcha-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: min(300px, 100%);
  margin: 0 auto 9px;
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.captcha-refresh {
  display: grid;
  width: 28px;
  height: 28px;
  padding: 0;
  place-items: center;
  border: 0;
  border-radius: 8px;
  color: var(--el-text-color-secondary);
  background: transparent;
  cursor: pointer;
  transition: color .18s ease, background-color .18s ease, transform .18s ease;
}

.captcha-refresh:hover:not(:disabled) {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  transform: rotate(18deg);
}

.captcha-refresh:focus-visible,
.captcha-thumb:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}

.captcha-refresh:disabled {
  cursor: not-allowed;
  opacity: .45;
}

.captcha-stage {
  position: relative;
  width: min(300px, 100%);
  aspect-ratio: 5 / 3;
  margin: 0 auto 14px;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 10px;
  background: var(--el-fill-color-light);
  box-shadow: inset 0 0 0 1px rgb(255 255 255 / 18%);
  user-select: none;
}

.captcha-background {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: fill;
  pointer-events: none;
}

.captcha-piece {
  position: absolute;
  inset: 0 auto 0 0;
  height: 100%;
  will-change: transform;
  pointer-events: none;
}

.captcha-piece img {
  display: block;
  width: auto;
  height: 100%;
}

.captcha-stage-loading {
  position: absolute;
  inset: 0;
  display: grid;
  gap: 10px;
  place-content: center;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
  font-size: 13px;
}

.captcha-loading-pulse {
  width: 34px;
  height: 34px;
  margin: 0 auto;
  border: 3px solid var(--el-color-primary-light-8);
  border-top-color: var(--el-color-primary);
  border-radius: 50%;
  animation: captcha-spin .8s linear infinite;
}

.captcha-track {
  position: relative;
  width: min(300px, 100%);
  height: 42px;
  margin: 0 auto;
  overflow: hidden;
  border: 1px solid var(--el-border-color);
  border-radius: 9px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  transition: border-color .18s ease, background-color .18s ease;
  touch-action: none;
  user-select: none;
}

.captcha-track-fill {
  position: absolute;
  inset: 0 auto 0 0;
  min-width: 29px;
  border-right: 1px solid var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
}

.captcha-track-label {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  padding-left: 44px;
  font-size: 13px;
  pointer-events: none;
}

.captcha-thumb {
  position: absolute;
  inset: -1px auto -1px -1px;
  display: grid;
  width: 58px;
  padding: 0;
  place-items: center;
  border: 1px solid var(--el-color-primary-light-5);
  border-radius: 9px;
  color: var(--el-color-primary);
  background: var(--el-bg-color);
  box-shadow: 2px 0 10px rgb(15 23 42 / 10%);
  cursor: grab;
  font: 700 17px/1 ui-monospace, SFMono-Regular, Consolas, monospace;
  letter-spacing: -3px;
  touch-action: none;
  will-change: transform;
}

.captcha-thumb:active {
  cursor: grabbing;
}

.captcha-thumb:disabled {
  cursor: not-allowed;
}

.captcha-track.success {
  border-color: var(--el-color-success-light-5);
  background: var(--el-color-success-light-9);
}

.captcha-track.success .captcha-track-fill {
  border-right-color: var(--el-color-success-light-5);
  background: var(--el-color-success-light-9);
}

.captcha-track.success .captcha-thumb {
  border-color: var(--el-color-success);
  color: white;
  background: var(--el-color-success);
}

.captcha-track.error {
  border-color: var(--el-color-danger-light-5);
}

.captcha-status {
  min-height: 18px;
  margin: 9px 0 0;
  color: var(--el-text-color-secondary);
  text-align: center;
  font-size: 12px;
}

.captcha-status.success {
  color: var(--el-color-success);
}

.captcha-status.error {
  color: var(--el-color-danger);
}

.captcha-load-error {
  display: grid;
  min-height: 246px;
  gap: 14px;
  place-content: center;
  justify-items: center;
  color: var(--el-text-color-regular);
  text-align: center;
  font-size: 13px;
}

@keyframes captcha-spin {
  to { transform: rotate(360deg); }
}

@media (prefers-reduced-motion: reduce) {
  .captcha-refresh,
  .captcha-track,
  .captcha-loading-pulse {
    transition: none;
    animation: none;
  }
}
</style>
