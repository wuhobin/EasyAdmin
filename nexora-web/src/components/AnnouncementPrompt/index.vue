<template>
  <el-dialog v-model="visible" title="系统公告" width="720px" class="announcement-dialog" :show-close="false" :close-on-click-modal="false" :close-on-press-escape="false" destroy-on-close>
    <p class="announcement-intro">以下公告需要确认后关闭。</p>
    <el-collapse v-model="expanded" class="announcement-list">
      <el-collapse-item v-for="item in announcements" :key="item.noticeId || item.id" :name="item.noticeId || item.id">
        <template #title><span class="announcement-title">{{ item.title }}</span><span class="announcement-time">{{ item.publishTime || '' }}</span></template>
        <iframe v-if="item.contentFormat === 'html'" class="announcement-frame" sandbox="allow-popups" :srcdoc="item.content" title="公告 HTML 内容" />
        <pre v-else class="announcement-text">{{ item.content }}</pre>
      </el-collapse-item>
    </el-collapse>
    <template #footer><el-button type="primary" @click="acknowledge">我知道了</el-button></template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { acknowledgeAnnouncementsApi, getPendingAnnouncementsApi, type NoticeItem } from '@/api/system/notice'

const visible = ref(false)
const announcements = ref<NoticeItem[]>([])
const expanded = ref<Array<number>>([])

async function load() {
  try {
    const { data } = await getPendingAnnouncementsApi()
    if (!data?.length) return
    announcements.value = data
    expanded.value = [data[0].noticeId || data[0].id]
    visible.value = true
  } catch {
    // 公告弹窗失败不阻塞登录后的页面使用。
  }
}

async function acknowledge() {
  await acknowledgeAnnouncementsApi(announcements.value.map(item => item.noticeId || item.id))
  visible.value = false
  window.dispatchEvent(new Event('nexora:notice-read-changed'))
}

onMounted(() => { void load() })
</script>

<style scoped>
.announcement-intro { margin: 0 0 12px; color: var(--el-text-color-secondary); }
.announcement-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.announcement-time { margin-left: auto; margin-right: 18px; color: var(--el-text-color-secondary); font-size: 12px; }
.announcement-frame { display: block; width: 100%; height: min(48vh, 480px); border: 1px solid var(--el-border-color-lighter); background: #fff; }
.announcement-text { max-height: 48vh; overflow: auto; margin: 0; white-space: pre-wrap; word-break: break-word; line-height: 1.7; font: inherit; }
@media (max-width: 768px) {
  .announcement-dialog { width: calc(100vw - 20px) !important; }
  .announcement-frame { height: 58vh; }
}
</style>
