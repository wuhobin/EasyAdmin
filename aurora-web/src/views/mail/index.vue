<template>
  <div class="mail-page">
    <header class="mail-header">
      <div>
        <p class="mail-eyebrow">UNIFIED INBOX</p>
        <h1>聚合邮箱</h1>
        <p class="mail-subtitle">邮件实时来自邮箱服务器，系统不保存历史正文。</p>
      </div>
      <div class="mail-header-actions">
        <span class="refresh-status">
          <i :class="['status-pulse', { active: autoRefresh }]" />
          {{ autoRefresh ? '每 15 秒自动刷新' : '自动刷新已暂停' }}
        </span>
        <el-switch v-model="autoRefresh" aria-label="自动刷新" />
        <el-button :icon="Refresh" :loading="mailLoading" @click="loadMessages(false)">刷新</el-button>
        <el-button v-permission="['mail:account:add']" type="primary" :icon="Plus" @click="openCreateDialog">
          添加邮箱
        </el-button>
      </div>
    </header>

    <main class="mail-workspace">
      <aside class="account-panel">
        <div class="panel-label">邮箱账户</div>
        <button
          :class="['account-item', { active: selectedAccountId === undefined }]"
          type="button"
          @click="selectAccount(undefined)"
        >
          <span class="account-avatar all"><Collection /></span>
          <span class="account-copy">
            <strong>全部收件箱</strong>
            <small>{{ accounts.filter((item) => item.enabled === 1).length }} 个启用账户</small>
          </span>
          <span class="message-count">{{ messages.length }}</span>
        </button>

        <div v-if="accounts.length" class="account-list">
          <button
            v-for="account in accounts"
            :key="account.id"
            :class="['account-item', { active: selectedAccountId === account.id }]"
            type="button"
            @click="selectAccount(account.id)"
          >
            <span :class="['account-avatar', providerClass(account.provider)]">
              {{ providerMark(account.provider) }}
            </span>
            <span class="account-copy">
              <strong>{{ account.accountName }}</strong>
              <small>{{ account.email }}</small>
            </span>
            <span :class="['connection-dot', { error: account.lastError, disabled: account.enabled !== 1 }]" />
          </button>
        </div>

        <el-empty v-else description="还没有邮箱账户" :image-size="72" />

        <div class="account-tools">
          <el-dropdown v-if="currentAccount" trigger="click" @command="handleAccountCommand">
            <el-button plain>
              管理当前邮箱<el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-permission="['mail:account:test']" command="test">测试连接</el-dropdown-item>
                <el-dropdown-item v-permission="['mail:account:update']" command="edit">编辑账户</el-dropdown-item>
                <el-dropdown-item v-permission="['mail:account:delete']" command="delete" divided>删除账户</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </aside>

      <section class="message-panel">
        <div class="message-toolbar">
          <div>
            <strong>{{ currentAccount?.accountName || '全部收件箱' }}</strong>
            <span>{{ messages.length }} 封最新邮件</span>
          </div>
          <el-segmented v-model="mailLimit" :options="limitOptions" size="small" @change="loadMessages(false)" />
        </div>

        <div v-loading="mailLoading" class="message-list">
          <button
            v-for="message in messages"
            :key="messageKey(message)"
            :class="['message-item', { active: selectedMessageKey === messageKey(message), unread: !message.read }]"
            type="button"
            @click="openMessage(message)"
          >
            <span :class="['sender-avatar', providerClass(message.provider)]">
              {{ senderMark(message) }}
            </span>
            <span class="message-copy">
              <span class="message-row">
                <strong>{{ message.fromName || message.fromAddress }}</strong>
                <time>{{ formatMessageTime(message.receivedTime) }}</time>
              </span>
              <span class="subject-row">
                <b>{{ message.subject }}</b>
                <el-icon v-if="message.hasAttachment"><Paperclip /></el-icon>
              </span>
              <small>{{ message.accountName }} · {{ message.fromAddress }}</small>
            </span>
          </button>

          <el-empty v-if="!mailLoading && !messages.length" description="没有读取到邮件" />
          <div v-if="hasMore" class="load-more">
            <el-button :loading="loadingMore" @click="loadMore">加载更多</el-button>
          </div>
        </div>
      </section>

      <article class="reader-panel">
        <div v-if="detailLoading" class="reader-loading">
          <el-skeleton :rows="8" animated />
        </div>

        <template v-else-if="mailDetail">
          <header class="reader-header">
            <div class="reader-meta">
              <span class="reader-badge">{{ providerLabel(messageProvider) }}</span>
              <time>{{ mailDetail.receivedTime || '-' }}</time>
            </div>
            <h2>{{ mailDetail.subject }}</h2>
            <div class="sender-line">
              <span :class="['sender-avatar large', providerClass(messageProvider)]">
                {{ mailDetail.fromName?.slice(0, 1).toUpperCase() || '@' }}
              </span>
              <div>
                <strong>{{ mailDetail.fromName || mailDetail.fromAddress }}</strong>
                <p>{{ mailDetail.fromAddress }}</p>
                <small>发送至 {{ mailDetail.recipients?.join(', ') || '-' }}</small>
              </div>
            </div>
          </header>

          <div v-if="mailDetail.attachments?.length" class="attachment-strip">
            <button
              v-for="attachment in mailDetail.attachments"
              :key="attachment.partId"
              v-permission="['mail:inbox:download']"
              type="button"
              @click="downloadAttachment(attachment)"
            >
              <el-icon><Document /></el-icon>
              <span><strong>{{ attachment.fileName }}</strong><small>{{ formatFileSize(attachment.size) }}</small></span>
              <el-icon><Download /></el-icon>
            </button>
          </div>

          <div class="reader-body">
            <iframe
              v-if="mailDetail.bodyHtml"
              class="mail-frame"
              sandbox="allow-popups allow-popups-to-escape-sandbox"
              title="邮件正文"
              :srcdoc="mailDocument"
            />
            <pre v-else class="plain-body">{{ mailDetail.bodyText || '（邮件没有可显示的正文）' }}</pre>
          </div>
        </template>

        <div v-else class="reader-empty">
          <div class="empty-envelope"><Message /></div>
          <h2>选择一封邮件开始阅读</h2>
          <p>正文和附件会在点击后直接从对应邮箱读取。</p>
        </div>
      </article>
    </main>

    <MailAccountDialog
      v-model="accountDialogVisible"
      :account="editingAccount"
      :provider-options="providerOptions"
      :provider-options-loading="providerOptionsLoading"
      @saved="handleAccountSaved"
    />
  </div>
</template>

<script setup lang="ts">
import {
  ArrowDown,
  Collection,
  Document,
  Download,
  Message,
  Paperclip,
  Plus,
  Refresh
} from '@element-plus/icons-vue'
import axios from 'axios'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import dayjs from 'dayjs'
import {
  deleteMailAccountApi,
  downloadMailAttachmentApi,
  getLatestMailsApi,
  getMailAccountsApi,
  getMailDetailApi,
  testMailAccountApi,
  type MailAccount,
  type MailAttachment,
  type MailMessageDetail,
  type MailMessageSummary,
  type MailProvider
} from '@/api/mail'
import MailAccountDialog from '@/components/MailAccountDialog/index.vue'
import { useMailProviderOptions } from '@/composables/useMailProviderOptions'

const {
  providerOptions,
  providerOptionsLoading,
  loadProviderOptions,
  providerLabel
} = useMailProviderOptions()
const limitOptions = [20, 30, 50]
const accounts = ref<MailAccount[]>([])
const messages = ref<MailMessageSummary[]>([])
const mailDetail = ref<MailMessageDetail>()
const selectedAccountId = ref<number>()
const selectedMessageKey = ref('')
const mailLimit = ref(30)
const mailLoading = ref(false)
const detailLoading = ref(false)
const autoRefresh = ref(true)
const initializedMessages = ref(false)
const knownMessageKeys = new Set<string>()
const mailCursor = ref<string>()
const hasMore = ref(false)
const loadingMore = ref(false)
let refreshTimer: number | undefined
let listController: AbortController | undefined
let detailController: AbortController | undefined
let listRequestId = 0
let detailRequestId = 0

const accountDialogVisible = ref(false)
const editingAccount = ref<MailAccount>()

const currentAccount = computed(() => accounts.value.find((item) => item.id === selectedAccountId.value))
const selectedMessage = computed(() => messages.value.find((item) => messageKey(item) === selectedMessageKey.value))
const messageProvider = computed(() => selectedMessage.value?.provider || 'QQ')
const mailDocument = computed(() => `<!doctype html><html><head><meta charset="utf-8"></head><body>${mailDetail.value?.bodyHtml || ''}</body></html>`)

const messageKey = (message: MailMessageSummary) => `${message.accountId}:${message.uidValidity}:${message.uid}`

const loadAccounts = async () => {
  const { data } = await getMailAccountsApi()
  accounts.value = data
}

const loadMessages = async (silent = true, append = false) => {
  if (append && (!hasMore.value || listController)) return
  if (silent && listController) return
  if (!append) {
    listController?.abort()
    loadingMore.value = false
  }
  const controller = new AbortController()
  listController = controller
  const requestId = ++listRequestId
  const requestedAccountId = selectedAccountId.value
  if (append) loadingMore.value = true
  else if (!silent) mailLoading.value = true
  try {
    const { data } = await getLatestMailsApi(
      requestedAccountId,
      mailLimit.value,
      append ? mailCursor.value : undefined,
      controller.signal
    )
    if (requestId !== listRequestId || requestedAccountId !== selectedAccountId.value) return
    if (!append && initializedMessages.value) {
      const newMessages = data.items.filter((item) => !knownMessageKeys.has(messageKey(item)))
      if (newMessages.length) {
        ElNotification({
          title: `收到 ${newMessages.length} 封新邮件`,
          message: newMessages[0].subject,
          type: 'success',
          position: 'bottom-right'
        })
      }
    }
    if (!append) knownMessageKeys.clear()
    data.items.forEach((item) => knownMessageKeys.add(messageKey(item)))
    initializedMessages.value = true
    messages.value = append
      ? [...messages.value, ...data.items.filter((item) => !messages.value.some((existing) => messageKey(existing) === messageKey(item)))]
      : data.items
    mailCursor.value = data.nextCursor
    hasMore.value = data.hasMore
    if (!append && selectedMessageKey.value && !data.items.some((item) => messageKey(item) === selectedMessageKey.value)) {
      selectedMessageKey.value = ''
      mailDetail.value = undefined
    }
  } catch (error) {
    if (!axios.isCancel(error)) throw error
  } finally {
    if (listController === controller) {
      listController = undefined
      if (append) loadingMore.value = false
      else if (!silent) mailLoading.value = false
    }
  }
}

const loadMore = () => loadMessages(false, true)

const selectAccount = async (id?: number) => {
  selectedAccountId.value = id
  selectedMessageKey.value = ''
  mailDetail.value = undefined
  initializedMessages.value = false
  knownMessageKeys.clear()
  mailCursor.value = undefined
  hasMore.value = false
  await loadMessages(false, false)
}

const openMessage = async (message: MailMessageSummary) => {
  detailController?.abort()
  const controller = new AbortController()
  detailController = controller
  const requestId = ++detailRequestId
  selectedMessageKey.value = messageKey(message)
  detailLoading.value = true
  try {
    const { data } = await getMailDetailApi(message, controller.signal)
    if (requestId !== detailRequestId || selectedMessageKey.value !== messageKey(message)) return
    mailDetail.value = data
  } catch (error) {
    if (!axios.isCancel(error)) throw error
  } finally {
    if (detailController === controller) {
      detailController = undefined
      detailLoading.value = false
    }
  }
}

const openCreateDialog = () => {
  editingAccount.value = undefined
  accountDialogVisible.value = true
}

const openEditDialog = (account: MailAccount) => {
  editingAccount.value = account
  accountDialogVisible.value = true
}

const handleAccountSaved = async () => {
  await loadAccounts()
  await loadMessages(false, false)
}

const handleAccountCommand = async (command: string) => {
  const account = currentAccount.value
  if (!account) return
  if (command === 'edit') openEditDialog(account)
  if (command === 'test') {
    await testMailAccountApi(account.id)
    ElMessage.success('邮箱连接成功')
    await loadAccounts()
  }
  if (command === 'delete') {
    await ElMessageBox.confirm(`确定删除邮箱“${account.accountName}”吗？`, '删除确认', { type: 'warning' })
    await deleteMailAccountApi(account.id)
    ElMessage.success('邮箱账户已删除')
    selectedAccountId.value = undefined
    await loadAccounts()
    await loadMessages(false)
  }
}

const downloadAttachment = async (attachment: MailAttachment) => {
  if (!mailDetail.value) return
  const data = await downloadMailAttachmentApi(mailDetail.value, attachment.partId)
  const blob = data instanceof Blob ? data : new Blob([data], { type: attachment.contentType })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  try {
    link.href = url
    link.download = attachment.fileName
    document.body.appendChild(link)
    link.click()
  } finally {
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  }
}

const providerMark = (provider: MailProvider) => provider === 'QQ' ? 'Q' : provider === 'NETEASE_163' ? '163' : provider === 'NETEASE_126' ? '126' : 'Y'
const providerClass = (provider: MailProvider) => provider.toLowerCase().replace('_', '-')
const senderMark = (message: MailMessageSummary) => (message.fromName || message.fromAddress || '@').slice(0, 1).toUpperCase()

const formatMessageTime = (value?: string) => {
  if (!value) return '-'
  const time = dayjs(value)
  return time.isSame(dayjs(), 'day') ? time.format('HH:mm') : time.format('MM-DD')
}

const formatFileSize = (size: number) => {
  if (!size) return '未知大小'
  const units = ['B', 'KB', 'MB', 'GB']
  const index = Math.min(Math.floor(Math.log(size) / Math.log(1024)), units.length - 1)
  return `${(size / Math.pow(1024, index)).toFixed(index === 0 ? 0 : 1)} ${units[index]}`
}

const clearRefreshTimer = () => window.clearTimeout(refreshTimer)

const scheduleRefresh = () => {
  clearRefreshTimer()
  if (!autoRefresh.value || document.visibilityState !== 'visible') return
  refreshTimer = window.setTimeout(async () => {
    try {
      await loadMessages(true, false)
    } catch {
      // Request errors are reported by the shared request interceptor.
    } finally {
      scheduleRefresh()
    }
  }, 15_000)
}

const handleVisibilityChange = () => scheduleRefresh()

watch(autoRefresh, scheduleRefresh)

onMounted(async () => {
  await Promise.all([loadProviderOptions(), loadAccounts()])
  await loadMessages(false, false)
  document.addEventListener('visibilitychange', handleVisibilityChange)
  scheduleRefresh()
})

onUnmounted(() => {
  clearRefreshTimer()
  listController?.abort()
  detailController?.abort()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<style scoped>
.mail-page {
  --ink: #20302d;
  --muted: #74847f;
  --line: #dfe8e5;
  --paper: #f6f8f7;
  --accent: #0d7a6b;
  min-height: calc(100vh - 84px);
  padding: 22px;
  color: var(--ink);
  background:
    radial-gradient(circle at 8% 4%, rgba(13, 122, 107, 0.08), transparent 24%),
    linear-gradient(145deg, #f8faf9 0%, #eef3f1 100%);
}

.mail-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 18px;
}

.mail-eyebrow { margin: 0 0 4px; color: var(--accent); font-size: 11px; font-weight: 800; letter-spacing: .18em; }
.mail-header h1 { margin: 0; font-family: Georgia, "Songti SC", serif; font-size: 32px; letter-spacing: -.03em; }
.mail-subtitle { margin: 5px 0 0; color: var(--muted); font-size: 13px; }
.mail-header-actions { display: flex; align-items: center; gap: 10px; }
.refresh-status { display: inline-flex; align-items: center; gap: 7px; color: var(--muted); font-size: 12px; }
.status-pulse { width: 7px; height: 7px; border-radius: 50%; background: #aeb8b5; }
.status-pulse.active { background: #20a27f; box-shadow: 0 0 0 5px rgba(32, 162, 127, .1); }

.mail-workspace {
  display: grid;
  grid-template-columns: 230px minmax(310px, 390px) minmax(420px, 1fr);
  grid-template-rows: minmax(0, 1fr);
  height: calc(100vh - 190px);
  min-height: 620px;
  overflow: hidden;
  border: 1px solid rgba(197, 213, 208, .9);
  border-radius: 16px;
  background: rgba(255, 255, 255, .94);
  box-shadow: 0 20px 55px rgba(31, 66, 58, .1);
}

.account-panel, .message-panel { min-width: 0; min-height: 0; overflow: hidden; border-right: 1px solid var(--line); }
.account-panel { display: flex; flex-direction: column; padding: 18px 12px 12px; background: #f8faf9; }
.panel-label { padding: 0 10px 12px; color: #8a9894; font-size: 11px; font-weight: 800; letter-spacing: .12em; text-transform: uppercase; }
.account-list { min-height: 0; overflow-y: auto; }
.account-item { display: flex; align-items: center; width: 100%; gap: 10px; padding: 10px; border: 0; border-radius: 11px; color: inherit; background: transparent; text-align: left; cursor: pointer; transition: .18s ease; }
.account-item:hover { background: #edf3f1; }
.account-item.active { background: #e4efec; box-shadow: inset 3px 0 var(--accent); }
.account-avatar, .sender-avatar { display: grid; place-items: center; flex: 0 0 auto; width: 36px; height: 36px; border-radius: 10px; color: #fff; background: #61736f; font-size: 11px; font-weight: 800; }
.account-avatar.all { background: var(--ink); font-size: 17px; }
.account-avatar.qq, .sender-avatar.qq { background: #168bd2; }
.account-avatar.netease-163, .sender-avatar.netease-163 { background: #d74b3f; }
.account-avatar.netease-126, .sender-avatar.netease-126 { background: #df7048; }
.account-avatar.yeah, .sender-avatar.yeah { background: #52934a; }
.account-copy { min-width: 0; flex: 1; }
.account-copy strong, .account-copy small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.account-copy strong { font-size: 13px; }
.account-copy small { margin-top: 2px; color: var(--muted); font-size: 11px; }
.message-count { min-width: 24px; padding: 2px 7px; border-radius: 99px; color: var(--accent); background: #d8e9e5; font-size: 11px; text-align: center; }
.connection-dot { width: 8px; height: 8px; border-radius: 50%; background: #28a77f; }
.connection-dot.error { background: #e35d54; }
.connection-dot.disabled { background: #b6bfbd; }
.account-tools { margin-top: auto; padding: 12px 4px 0; }
.account-tools .el-button { width: 100%; }

.message-panel { display: flex; flex-direction: column; }
.message-toolbar { display: flex; align-items: center; justify-content: space-between; min-height: 64px; padding: 0 16px; border-bottom: 1px solid var(--line); }
.message-toolbar strong, .message-toolbar span { display: block; }
.message-toolbar strong { font-size: 14px; }
.message-toolbar span { margin-top: 2px; color: var(--muted); font-size: 11px; }
.message-list { min-height: 0; flex: 1; overflow-y: auto; }
.message-item { display: flex; width: 100%; gap: 11px; padding: 14px 15px; border: 0; border-bottom: 1px solid #edf1f0; color: inherit; background: #fff; text-align: left; cursor: pointer; transition: .18s ease; }
.message-item:hover { background: #f6faf8; }
.message-item.active { background: #ecf5f2; box-shadow: inset 3px 0 var(--accent); }
.message-item.unread .message-copy > .message-row strong::after { content: ""; display: inline-block; width: 6px; height: 6px; margin-left: 6px; border-radius: 50%; background: var(--accent); vertical-align: middle; }
.message-copy { min-width: 0; flex: 1; }
.message-row, .subject-row { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.message-row strong { overflow: hidden; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.message-row time { flex: 0 0 auto; color: #8b9995; font-size: 10px; }
.subject-row { margin-top: 5px; }
.subject-row b { overflow: hidden; font-size: 12px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.message-copy small { display: block; overflow: hidden; margin-top: 5px; color: var(--muted); font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }

.reader-panel { min-width: 0; min-height: 0; overflow-y: auto; background: #fff; }
.reader-header { padding: 28px 32px 22px; border-bottom: 1px solid var(--line); }
.reader-meta { display: flex; align-items: center; justify-content: space-between; color: var(--muted); font-size: 11px; }
.reader-badge { padding: 3px 9px; border-radius: 99px; color: var(--accent); background: #e5f1ee; font-weight: 700; }
.reader-header h2 { margin: 16px 0 20px; font-family: Georgia, "Songti SC", serif; font-size: 24px; line-height: 1.35; }
.sender-line { display: flex; align-items: center; gap: 12px; }
.sender-avatar.large { width: 44px; height: 44px; border-radius: 50%; font-size: 15px; }
.sender-line strong, .sender-line p, .sender-line small { display: block; margin: 0; }
.sender-line strong { font-size: 13px; }
.sender-line p, .sender-line small { margin-top: 2px; color: var(--muted); font-size: 11px; }
.attachment-strip { display: flex; gap: 8px; overflow-x: auto; padding: 13px 24px; border-bottom: 1px solid var(--line); background: #fafcfb; }
.attachment-strip button { display: flex; align-items: center; min-width: 210px; gap: 9px; padding: 9px 11px; border: 1px solid var(--line); border-radius: 9px; color: inherit; background: #fff; text-align: left; cursor: pointer; }
.attachment-strip button span { min-width: 0; flex: 1; }
.attachment-strip strong, .attachment-strip small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.attachment-strip strong { font-size: 11px; }.attachment-strip small { color: var(--muted); font-size: 10px; }
.load-more { display: flex; justify-content: center; padding: 14px; }
.reader-body { padding: 0 16px 24px; }
.mail-frame { width: 100%; min-height: 600px; margin-top: 8px; border: 0; background: #fff; }
.plain-body { min-height: 500px; margin: 0; padding: 28px 16px; color: #33433f; font-family: "Microsoft YaHei", sans-serif; font-size: 13px; line-height: 1.8; white-space: pre-wrap; overflow-wrap: anywhere; }
.reader-empty, .reader-loading { display: grid; place-items: center; height: 100%; padding: 32px; text-align: center; }
.reader-loading { display: block; }
.reader-empty h2 { margin: 18px 0 7px; font-family: Georgia, "Songti SC", serif; font-size: 22px; }
.reader-empty p { margin: 0; color: var(--muted); font-size: 12px; }
.empty-envelope { display: grid; place-items: center; width: 76px; height: 76px; border: 1px solid #cfe0dc; border-radius: 24px; color: var(--accent); background: #eff6f4; font-size: 34px; transform: rotate(-4deg); }
.form-grid { display: grid; grid-template-columns: 1.2fr 1fr; gap: 14px; }.form-grid.compact { grid-template-columns: 1fr 1fr; align-items: center; }
.email-domain-suffix { color: var(--el-text-color-secondary); font-size: 15px; white-space: nowrap; }
.auth-tip { margin: 6px 0 0; color: var(--el-text-color-secondary); font-size: 12px; }

@media (max-width: 1250px) {
  .mail-workspace { grid-template-columns: 200px 330px minmax(390px, 1fr); }
}

@media (max-width: 980px) {
  .mail-header { align-items: flex-start; flex-direction: column; }
  .mail-header-actions { flex-wrap: wrap; }
  .mail-workspace { grid-template-columns: 190px minmax(300px, 1fr); height: auto; min-height: 720px; }
  .reader-panel { grid-column: 1 / -1; min-height: 620px; border-top: 1px solid var(--line); }
}
</style>
