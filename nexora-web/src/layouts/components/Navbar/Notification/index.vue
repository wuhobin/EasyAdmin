<template>
  <div class="notification-container" v-click-outside="closeNotification">
    <el-badge :value="unreadCount" :max="99" :hidden="!unreadCount" class="notification-badge">
      <button class="notification-trigger" type="button" aria-label="打开通知中心" :aria-expanded="isOpen" @click="toggleNotification">
        <el-icon class="notification-icon"><Bell /></el-icon>
      </button>
    </el-badge>
    <span class="sr-only" aria-live="polite">{{ unreadCount }} 条未读消息</span>

    <transition name="dropdown">
      <div v-show="isOpen" class="notification-dropdown">
        <div class="dropdown-header"><span>通知中心</span><el-button type="primary" link :disabled="!unreadCount" @click="markAllRead">全部已读</el-button></div>
        <el-tabs v-model="activeTab" class="notification-tabs" @tab-change="handleTabChange">
          <el-tab-pane :label="`未读消息${unreadCount ? ` (${unreadCount})` : ''}`" name="unread">
            <el-scrollbar height="320px" @scroll="handleScroll"><NoticeList :items="unreadItems" empty-text="暂无未读消息" @select="openNotice" /></el-scrollbar>
          </el-tab-pane>
          <el-tab-pane label="全部消息" name="all">
            <el-scrollbar height="320px" @scroll="handleScroll"><NoticeList :items="allItems" empty-text="暂无消息" @select="openNotice" /></el-scrollbar>
          </el-tab-pane>
        </el-tabs>
      </div>
    </transition>

    <el-dialog v-model="detailVisible" title="通知详情" width="680px" append-to-body destroy-on-close>
      <template v-if="detail">
        <div class="detail-meta"><el-tag :type="detail.noticeType === 2 ? 'warning' : 'primary'">{{ detail.noticeType === 2 ? '公告' : '通知' }}</el-tag><span>{{ detail.publishTime || '' }}</span></div>
        <h3>{{ detail.title }}</h3>
        <iframe v-if="detail.contentFormat === 'html'" class="notice-html-frame" sandbox="allow-popups" :srcdoc="detail.content" title="通知 HTML 内容" />
        <pre v-else class="notice-text">{{ detail.content }}</pre>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { defineComponent, h, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'
import { getMyNoticeDetailApi, getMyNoticeListApi, getUnreadNoticeCountApi, issueNotificationWsTicketApi, markAllNoticeReadApi, markNoticeReadApi, type NoticeItem } from '@/api/system/notice'
import { getToken } from '@/utils/auth'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const isOpen = ref(false)
const activeTab = ref<'unread' | 'all'>('unread')
const unreadCount = ref(0)
const unreadItems = ref<NoticeItem[]>([])
const allItems = ref<NoticeItem[]>([])
const page = ref(1)
const allLoaded = ref(false)
const loading = ref(false)
const detailVisible = ref(false)
const detail = ref<NoticeItem | null>(null)
let socket: WebSocket | null = null
let pingTimer: number | undefined
let reconnectTimer: number | undefined
let reconnectAttempt = 0
let disposed = false
let lastPongAt = 0

function formatTime(value?: string) { return value ? dayjs(value).fromNow() : '' }
function summary(item: NoticeItem) { return item.contentFormat === 'text' ? (item.contentPreview || item.content || '').replace(/\s+/g, ' ').slice(0, 100) : 'HTML 内容，点击查看详情' }
function iconType(item: NoticeItem) { return item.noticeType === 2 ? 'warning' : 'info' }

const NoticeList = defineComponent({
  props: { items: { type: Array, required: true }, emptyText: { type: String, required: true } },
  emits: ['select'],
  setup(props, { emit }) {
    return () => h('div', { class: 'notice-list' }, props.items.length ? (props.items as NoticeItem[]).map(item => h('button', { key: item.id, class: ['notification-item', { unread: item.isRead !== 1 }], type: 'button', onClick: () => emit('select', item) }, [
    h('div', { class: ['item-mark', iconType(item)] }, item.noticeType === 2 ? '公' : '通'),
    h('div', { class: 'notification-content' }, [h('div', { class: 'notification-title' }, item.title), h('div', { class: 'notification-message' }, summary(item)), h('div', { class: 'notification-time' }, formatTime(item.publishTime))])
    ])) : [h('div', { class: 'empty-text' }, props.emptyText)])
  }
})

async function refreshCount() { try { const { data } = await getUnreadNoticeCountApi(); unreadCount.value = data.unreadCount || 0 } catch { /* request layer reports auth errors */ } }
async function loadPage(reset = false) {
  if (loading.value || allLoaded.value && !reset) return
  if (reset) { page.value = 1; allLoaded.value = false; if (activeTab.value === 'unread') unreadItems.value = []; else allItems.value = [] }
  loading.value = true
  try {
    const { data } = await getMyNoticeListApi({ unreadOnly: activeTab.value === 'unread', pageNum: page.value, pageSize: 20 })
    const target = activeTab.value === 'unread' ? unreadItems : allItems
    target.value = page.value === 1 ? data.records || [] : [...target.value, ...(data.records || [])]
    allLoaded.value = target.value.length >= (data.total || 0)
    page.value += 1
  } finally { loading.value = false }
}
function toggleNotification() { isOpen.value = !isOpen.value; if (isOpen.value && !((activeTab.value === 'unread' ? unreadItems : allItems).value.length)) void loadPage(true) }
function closeNotification() { isOpen.value = false }
function handleTabChange() { void loadPage(true) }
function handleScroll(position: { scrollTop: number }) { if (position.scrollTop >= 240) void loadPage() }

async function openNotice(item: NoticeItem) {
  const noticeId = item.noticeId || item.id
  try {
    const { data } = await getMyNoticeDetailApi(noticeId)
    detail.value = data
    detailVisible.value = true
    if (item.isRead !== 1) {
      await markNoticeReadApi(noticeId)
      item.isRead = 1
      unreadItems.value = unreadItems.value.filter(entry => (entry.noticeId || entry.id) !== noticeId)
      allItems.value.forEach(entry => {
        if ((entry.noticeId || entry.id) === noticeId) entry.isRead = 1
      })
      unreadCount.value = Math.max(0, unreadCount.value - 1)
      if (activeTab.value === 'unread') await loadPage(true)
    }
  } catch { /* request layer handles failures */ }
}
async function markAllRead() {
  await markAllNoticeReadApi()
  unreadItems.value = []
  allItems.value.forEach(item => { item.isRead = 1 })
  unreadCount.value = 0
}

function buildWebSocketUrl(ticket: string) {
  const baseApi = String(import.meta.env.VITE_APP_BASE_API || '/api')
  const base = /^https?:\/\//i.test(baseApi) ? new URL(baseApi) : new URL(baseApi.startsWith('/') ? baseApi : `/${baseApi}`, window.location.origin)
  base.protocol = base.protocol === 'https:' ? 'wss:' : 'ws:'
  base.pathname = `${base.pathname.replace(/\/$/, '')}/ws/notification`
  base.search = new URLSearchParams({ ticket }).toString()
  return base.toString()
}
async function connect() {
  if (disposed || !getToken()) return
  try {
    const { data } = await issueNotificationWsTicketApi()
    if (disposed) return
    socket = new WebSocket(buildWebSocketUrl(data.ticket))
    socket.onmessage = event => {
      try {
        const message = JSON.parse(event.data)
        if (message.event === 'pong') lastPongAt = Date.now()
        if (message.event === 'notice-published') { unreadCount.value = message.unreadCount ?? unreadCount.value + 1; if (message.noticeType === 1) ElMessage.info(message.title); void loadPage(true) }
      } catch { /* ignore malformed events */ }
    }
    socket.onclose = () => { window.clearInterval(pingTimer); pingTimer = undefined; socket = null; scheduleReconnect() }
    socket.onerror = () => { socket?.close() }
    socket.onopen = () => {
      reconnectAttempt = 0
      lastPongAt = Date.now()
      window.clearInterval(pingTimer)
      pingTimer = window.setInterval(() => {
        const current = socket
        if (!current || current.readyState !== WebSocket.OPEN) return
        if (Date.now() - lastPongAt >= 90_000) {
          current.close(4000, 'pong timeout')
          return
        }
        current.send(JSON.stringify({ type: 'ping' }))
      }, 30_000)
      void refreshCount()
      void loadPage(true)
    }
  } catch { scheduleReconnect() }
}
function scheduleReconnect() { if (disposed || reconnectTimer || !getToken()) return; const delays = [1000, 2000, 5000, 10000, 30000]; reconnectTimer = window.setTimeout(() => { reconnectTimer = undefined; reconnectAttempt = Math.min(reconnectAttempt + 1, delays.length - 1); void connect() }, delays[reconnectAttempt]) }
function handleNoticeReadChanged() {
  void refreshCount()
  if (isOpen.value) void loadPage(true)
}
onMounted(() => {
  window.addEventListener('nexora:notice-read-changed', handleNoticeReadChanged)
  void refreshCount()
  void connect()
})
onBeforeUnmount(() => {
  disposed = true
  window.removeEventListener('nexora:notice-read-changed', handleNoticeReadChanged)
  window.clearInterval(pingTimer)
  window.clearTimeout(reconnectTimer)
  socket?.close()
})

const vClickOutside = { mounted(el: HTMLElement, binding: any) { const handler = (event: Event) => { if (!el.contains(event.target as Node)) binding.value() }; (el as any).__notificationOutside = handler; document.addEventListener('click', handler) }, unmounted(el: HTMLElement) { document.removeEventListener('click', (el as any).__notificationOutside) } }
</script>

<style lang="scss" scoped>
.notification-container { position: relative; display: flex; align-items: center; margin-right: 10px; }
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0; }
.notification-trigger { display: inline-grid; padding: 0; place-items: center; border: 0; color: inherit; background: transparent; cursor: pointer; }
.notification-icon { font-size: 20px; padding: 8px; border-radius: 50%; color: var(--el-text-color-regular); }
.notification-icon:hover { color: var(--el-color-primary); background: var(--el-fill-color-light); }
.notification-dropdown { position: absolute; top: calc(100% + 8px); right: 0; z-index: 2000; width: 380px; overflow: hidden; background: var(--el-bg-color-overlay); border: 1px solid var(--el-border-color-lighter); border-radius: 12px; box-shadow: var(--el-box-shadow-light); }
.dropdown-header { display: flex; justify-content: space-between; align-items: center; padding: 15px 16px; border-bottom: 1px solid var(--el-border-color-lighter); font-weight: 600; }
.notification-tabs :deep(.el-tabs__header) { margin: 0; padding: 0 12px; }
.notification-tabs :deep(.el-tabs__content) { padding: 10px 12px 12px; }
.notice-list { min-height: 290px; }
.notification-item { display: flex; width: 100%; gap: 10px; padding: 11px 10px; margin-bottom: 6px; border: 1px solid transparent; border-radius: 8px; background: transparent; color: inherit; text-align: left; cursor: pointer; }
.notification-item:hover, .notification-item.unread { background: var(--el-fill-color-light); border-color: var(--el-border-color-lighter); }
.item-mark { flex: 0 0 28px; height: 28px; border-radius: 8px; display: grid; place-items: center; font-size: 12px; font-weight: 600; }
.item-mark.info { color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
.item-mark.warning { color: var(--el-color-warning); background: var(--el-color-warning-light-9); }
.notification-content { min-width: 0; flex: 1; }
.notification-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 500; }
.notification-message { overflow: hidden; color: var(--el-text-color-secondary); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.notification-time { color: var(--el-text-color-secondary); font-size: 11px; }
.empty-text { display: grid; min-height: 290px; place-items: center; color: var(--el-text-color-secondary); }
.detail-meta { display: flex; gap: 10px; align-items: center; color: var(--el-text-color-secondary); font-size: 12px; }
.notice-html-frame { width: 100%; height: 55vh; border: 1px solid var(--el-border-color-lighter); background: #fff; }
.notice-text { max-height: 55vh; overflow: auto; white-space: pre-wrap; word-break: break-word; line-height: 1.7; font: inherit; }
@media (max-width: 768px) { .notification-dropdown { right: -44px; width: min(380px, calc(100vw - 24px)); } }
</style>
