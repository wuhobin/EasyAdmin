<template>
  <div class="server-page data-list-page">
    <el-card class="server-workbench data-list-card" shadow="never">
      <div class="search-wrapper data-list-filters">
        <el-form :model="query" :inline="true">
          <el-form-item label="服务器">
            <el-input
              v-model="query.name"
              :prefix-icon="Search"
              clearable
              placeholder="搜索名称"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.enabled" clearable placeholder="全部状态">
              <el-option label="已启用" :value="1" />
              <el-option label="已停用" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
            <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
          <el-form-item class="server-list-actions">
            <el-button :icon="Refresh" :loading="loading" @click="loadServers">刷新</el-button>
            <el-button
              v-permission="['monitor:server:add']"
              type="primary"
              :icon="Plus"
              @click="openCreateDialog"
            >
              添加服务器
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <div v-loading="loading" class="server-grid" aria-live="polite">
        <article
          v-for="server in servers"
          :key="server.id"
          class="server-card"
          :class="serverState(server).className"
          :aria-label="`${server.name}，${serverState(server).label}`"
        >
          <header class="server-card-header">
            <span :class="['server-mark', serverState(server).className]">
              <Platform />
            </span>
            <div class="server-title">
              <strong>{{ server.name }}</strong>
              <code>{{ server.username }}@{{ server.host }}:{{ server.port }}</code>
            </div>
            <span :class="['state-badge', serverState(server).className]">
              <i />{{ serverState(server).label }}
            </span>
          </header>

          <p class="server-description">
            {{ server.description || '未填写用途说明' }}
          </p>

          <dl class="server-meta">
            <div>
              <dt>凭据</dt>
              <dd>
                <Lock v-if="server.hasSavedPassword" />
                <Unlock v-else />
                {{ server.hasSavedPassword ? '已加密保存' : '连接时输入' }}
              </dd>
            </div>
            <div>
              <dt>最后连接</dt>
              <dd>{{ server.lastConnectTime || '尚未连接' }}</dd>
            </div>
          </dl>

          <section :class="['fingerprint-strip', { trusted: server.trustedFingerprint }]">
            <div class="fingerprint-label">
              <span><Key />主机指纹</span>
              <el-tag
                v-if="server.trustedFingerprint"
                type="success"
                size="small"
                effect="plain"
              >
                已确认
              </el-tag>
              <el-tag v-else type="info" size="small" effect="plain">待确认</el-tag>
            </div>
            <template v-if="server.trustedFingerprint">
              <el-tooltip :content="server.trustedFingerprint" placement="top">
                <code>{{ compactFingerprint(server.trustedFingerprint) }}</code>
              </el-tooltip>
              <small>
                {{ server.fingerprintAlgorithm || '未知算法' }}
                · {{ server.fingerprintVerifiedTime || '确认时间未知' }}
              </small>
            </template>
            <template v-else>
              <p>首次测试连接后，请核对并确认服务器返回的指纹。</p>
            </template>
          </section>

          <el-alert
            v-if="server.lastError"
            class="server-error"
            :title="server.lastError"
            type="error"
            :closable="false"
            show-icon
          />

          <footer class="server-actions">
            <el-button
              v-permission="['monitor:server:terminal']"
              type="primary"
              :icon="Monitor"
              :disabled="server.enabled !== 1"
              @click="handleOpenTerminal(server)"
            >
              SSH 终端
            </el-button>
            <el-button
              v-permission="['monitor:server:test']"
              :icon="Connection"
              :loading="testingId === server.id"
              :disabled="server.enabled !== 1"
              @click="handleTest(server)"
            >
              测试
            </el-button>
            <el-dropdown trigger="click">
              <el-button :icon="MoreFilled" aria-label="更多服务器操作" />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-if="server.trustedFingerprint"
                    v-permission="['monitor:server:fingerprint']"
                    :icon="RefreshLeft"
                    @click="handleResetFingerprint(server)"
                  >
                    重置主机指纹
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-permission="['monitor:server:update']"
                    :icon="Edit"
                    @click="openEditDialog(server)"
                  >
                    编辑配置
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-permission="['monitor:server:delete']"
                    :icon="Delete"
                    divided
                    @click="handleDelete(server)"
                  >
                    删除服务器
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </footer>
        </article>

        <el-empty
          v-if="!loading && servers.length === 0"
          class="server-empty"
          description="暂无符合条件的服务器"
        >
          <el-button
            v-permission="['monitor:server:add']"
            type="primary"
            :icon="Plus"
            @click="openCreateDialog"
          >
            添加第一台服务器
          </el-button>
        </el-empty>
      </div>

      <div v-if="total > 0" class="data-list-pagination">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          background
          :page-sizes="[6, 12, 24, 48]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @current-change="loadServers"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <ManagedServerDialog
      v-model="serverDialogVisible"
      :server="editingServer"
      @saved="loadServers"
    />
    <SshTerminalDialog
      v-model="terminalVisible"
      :server="terminalServer"
      :password="terminalPassword"
    />
  </div>
</template>

<script setup lang="ts">
import {
  Connection,
  Delete,
  Edit,
  Key,
  Lock,
  Monitor,
  MoreFilled,
  Platform,
  Plus,
  Refresh,
  RefreshLeft,
  Search,
  Unlock
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  confirmServerFingerprintApi,
  deleteManagedServerApi,
  getManagedServersApi,
  resetServerFingerprintApi,
  testManagedServerApi,
  type ManagedServer,
  type ManagedServerQuery,
  type ServerConnectionTest
} from '@/api/monitor/server'
import ManagedServerDialog from '@/components/ManagedServerDialog/index.vue'
import SshTerminalDialog from '@/components/SshTerminalDialog/index.vue'

const servers = ref<ManagedServer[]>([])
const total = ref(0)
const loading = ref(false)
const testingId = ref<number>()
const serverDialogVisible = ref(false)
const editingServer = ref<ManagedServer>()
const terminalVisible = ref(false)
const terminalServer = ref<ManagedServer>()
const terminalPassword = ref('')
const query = reactive<ManagedServerQuery>({
  pageNum: 1,
  pageSize: 12,
  name: '',
  enabled: undefined
})

async function loadServers() {
  loading.value = true
  try {
    const { data } = await getManagedServersApi({
      ...query,
      name: query.name?.trim() || undefined
    })
    servers.value = data.records
    total.value = data.total
    if (servers.value.length === 0 && data.total > 0 && query.pageNum > 1) {
      query.pageNum -= 1
      await loadServers()
    }
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  query.pageNum = 1
  void loadServers()
}

function resetQuery() {
  query.pageNum = 1
  query.name = ''
  query.enabled = undefined
  void loadServers()
}

function handleSizeChange() {
  query.pageNum = 1
  void loadServers()
}

function openCreateDialog() {
  editingServer.value = undefined
  serverDialogVisible.value = true
}

function openEditDialog(server: ManagedServer) {
  editingServer.value = server
  serverDialogVisible.value = true
}

async function askForPassword(server: ManagedServer, action: string) {
  if (server.hasSavedPassword) return { ready: true, password: undefined }
  try {
    const { value } = await ElMessageBox.prompt(
      `“${server.name}”没有保存密码，请输入本次${action}使用的 SSH 密码。`,
      '临时 SSH 密码',
      {
        confirmButtonText: '继续',
        cancelButtonText: '取消',
        inputType: 'password',
        inputPlaceholder: '密码仅用于本次连接',
        inputValidator: (input) => {
          if (!input) return '请输入 SSH 密码'
          if (input.length > 512) return 'SSH 密码不能超过 512 个字符'
          return true
        }
      }
    )
    return { ready: true, password: value }
  } catch {
    return { ready: false, password: undefined }
  }
}

async function approveFingerprint(server: ManagedServer, result: ServerConnectionTest) {
  if (!result.fingerprint) return false
  const detail = [
    `服务器：${server.name}（${server.host}:${server.port}）`,
    `算法：${result.algorithm || '未知'}`,
    `指纹：${result.fingerprint}`,
    '',
    '请与服务器管理员提供的指纹核对一致后再确认。'
  ].join('\n')
  try {
    await ElMessageBox.confirm(detail, '确认 SSH 主机指纹', {
      type: 'warning',
      confirmButtonText: '指纹一致，确认信任',
      cancelButtonText: '取消',
      customClass: 'fingerprint-confirm-box'
    })
    await confirmServerFingerprintApi(server.id, result.fingerprint)
    return true
  } catch {
    return false
  }
}

async function showFingerprintMismatch(server: ManagedServer, result: ServerConnectionTest) {
  const detail = [
    `服务器“${server.name}”返回的主机指纹与已信任值不同，连接已阻止。`,
    '',
    `已信任：${result.trustedFingerprint || '无'}`,
    `当前返回：${result.fingerprint || '未知'}`,
    '',
    '请先核实服务器是否更换过主机密钥；确认安全后再重置指纹并重新测试。'
  ].join('\n')
  await ElMessageBox.alert(detail, '主机指纹发生变化', {
    type: 'error',
    confirmButtonText: '知道了',
    customClass: 'fingerprint-confirm-box'
  })
}

async function executeConnectionTest(
  server: ManagedServer,
  password?: string,
  allowConfirmation = true
): Promise<boolean> {
  const { data } = await testManagedServerApi(server.id, password)
  if (data.status === 'SUCCESS') return true
  if (data.status === 'FINGERPRINT_MISMATCH') {
    await showFingerprintMismatch(server, data)
    return false
  }
  if (data.status === 'CONFIRM_REQUIRED' && allowConfirmation) {
    const approved = await approveFingerprint(server, data)
    if (!approved) return false
    return executeConnectionTest(server, password, false)
  }
  return false
}

async function handleTest(server: ManagedServer) {
  const credential = await askForPassword(server, '连接测试')
  if (!credential.ready) return
  testingId.value = server.id
  try {
    if (await executeConnectionTest(server, credential.password)) {
      ElMessage.success(`${server.name} SSH 连接成功`)
    }
    await loadServers()
  } finally {
    testingId.value = undefined
  }
}

async function handleOpenTerminal(server: ManagedServer) {
  if (server.enabled !== 1) {
    ElMessage.warning('请先启用服务器')
    return
  }
  const credential = await askForPassword(server, '打开终端')
  if (!credential.ready) return
  if (!server.trustedFingerprint) {
    testingId.value = server.id
    try {
      const connected = await executeConnectionTest(server, credential.password)
      if (!connected) return
      await loadServers()
    } finally {
      testingId.value = undefined
    }
  }
  terminalServer.value = server
  terminalPassword.value = credential.password || ''
  terminalVisible.value = true
}

async function handleResetFingerprint(server: ManagedServer) {
  try {
    await ElMessageBox.confirm(
      `重置“${server.name}”的主机指纹后，必须重新测试并确认才能连接。`,
      '重置主机指纹',
      { type: 'warning', confirmButtonText: '确认重置', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await resetServerFingerprintApi(server.id)
  ElMessage.success('主机指纹已重置')
  await loadServers()
}

async function handleDelete(server: ManagedServer) {
  try {
    await ElMessageBox.confirm(
      `确定删除服务器“${server.name}”吗？正在使用的终端会立即断开。`,
      '删除服务器',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await deleteManagedServerApi(server.id)
  ElMessage.success('服务器已删除')
  await loadServers()
}

function compactFingerprint(fingerprint: string) {
  if (fingerprint.length <= 38) return fingerprint
  return `${fingerprint.slice(0, 24)}…${fingerprint.slice(-12)}`
}

function serverState(server: ManagedServer) {
  if (server.enabled !== 1) return { label: '已停用', className: 'disabled' }
  if (server.lastError) return { label: '连接异常', className: 'error' }
  if (server.lastConnectTime) return { label: '连接正常', className: 'online' }
  return { label: '尚未测试', className: 'pending' }
}

watch(terminalVisible, (value) => {
  if (!value) {
    terminalPassword.value = ''
    terminalServer.value = undefined
  }
})

onMounted(loadServers)
</script>

<style scoped>
.server-page {
  --server-line: var(--nexora-list-divider);
  --server-muted: var(--el-text-color-secondary);
  --server-code: "JetBrains Mono", "Cascadia Code", Consolas, monospace;
}
.server-list-actions {
  margin-left: auto !important;
}
.server-list-actions :deep(.el-form-item__content) {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.server-list-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}
.server-grid {
  display: grid;
  min-height: 220px;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}
.server-card {
  --server-state-color: var(--el-color-info);
  --server-state-soft: var(--el-color-info-light-9);
  position: relative;
  display: flex;
  min-width: 0;
  overflow: hidden;
  flex-direction: column;
  padding: 18px 18px 16px 20px;
  border: 1px solid color-mix(
    in srgb,
    var(--server-state-color) 14%,
    var(--nexora-list-border)
  );
  border-radius: 14px;
  background:
    linear-gradient(
      135deg,
      color-mix(in srgb, var(--server-state-color) 5%, var(--el-bg-color)) 0,
      var(--el-bg-color) 42%
    );
  box-shadow:
    0 1px 2px rgba(15, 23, 42, .04),
    0 14px 30px -26px rgba(15, 23, 42, .5);
  transition:
    border-color .18s ease,
    box-shadow .18s ease,
    transform .18s ease;
}
.server-card::before {
  position: absolute;
  top: 16px;
  bottom: 16px;
  left: 0;
  width: 4px;
  content: '';
  background: var(--server-state-color);
  border-radius: 0 999px 999px 0;
  box-shadow: 0 0 14px color-mix(in srgb, var(--server-state-color) 26%, transparent);
}
.server-card.online {
  --server-state-color: var(--el-color-success);
  --server-state-soft: var(--el-color-success-light-9);
}
.server-card.error {
  --server-state-color: var(--el-color-danger);
  --server-state-soft: var(--el-color-danger-light-9);
}
.server-card.disabled {
  --server-state-color: var(--el-text-color-placeholder);
  --server-state-soft: var(--el-fill-color-light);
}
.server-card:hover {
  border-color: color-mix(
    in srgb,
    var(--server-state-color) 42%,
    var(--nexora-list-border)
  );
  box-shadow:
    0 2px 4px rgba(15, 23, 42, .06),
    0 20px 36px -28px color-mix(in srgb, var(--server-state-color) 58%, transparent);
  transform: translateY(-3px);
}
.server-card:focus-within {
  border-color: color-mix(
    in srgb,
    var(--el-color-primary) 48%,
    var(--nexora-list-border)
  );
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--el-color-primary) 11%, transparent);
}
.server-card-header {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
}
.server-mark {
  display: grid;
  width: 46px;
  height: 46px;
  place-items: center;
  color: var(--server-state-color);
  background: color-mix(in srgb, var(--server-state-soft) 84%, var(--el-bg-color));
  border: 1px solid color-mix(in srgb, var(--server-state-color) 18%, transparent);
  border-radius: 12px;
  box-shadow: inset 0 0 0 3px color-mix(in srgb, var(--el-bg-color) 54%, transparent);
}
.server-mark :deep(svg) {
  width: 22px;
}
.server-title {
  min-width: 0;
}
.server-title strong,
.server-title code {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.server-title strong {
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 700;
  line-height: 22px;
  letter-spacing: -.01em;
}
.server-title code {
  margin-top: 4px;
  color: var(--server-muted);
  font-family: var(--server-code);
  font-size: 11px;
  font-variant-numeric: tabular-nums;
}
.state-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 8px;
  color: var(--server-state-color);
  background: color-mix(in srgb, var(--server-state-soft) 72%, var(--el-bg-color));
  border: 1px solid color-mix(in srgb, var(--server-state-color) 20%, transparent);
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}
.state-badge i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--server-state-color);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--server-state-color) 11%, transparent);
}
.server-description {
  display: -webkit-box;
  min-height: 40px;
  margin: 16px 2px 14px;
  overflow: hidden;
  color: var(--el-text-color-regular);
  font-size: 12.5px;
  line-height: 20px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.server-meta {
  display: grid;
  overflow: hidden;
  grid-template-columns: .85fr 1.15fr;
  gap: 1px;
  padding: 1px;
  margin: 0 0 14px;
  background: var(--server-line);
  border-radius: 10px;
}
.server-meta > div {
  min-width: 0;
  padding: 10px 11px;
  background: color-mix(in srgb, var(--el-fill-color-lighter) 68%, var(--el-bg-color));
}
.server-meta dt {
  color: var(--server-muted);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: .04em;
}
.server-meta dd {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 5px;
  margin: 4px 0 0;
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 11.5px;
  font-variant-numeric: tabular-nums;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.server-meta dd :deep(svg) {
  width: 13px;
  flex: 0 0 auto;
}
.fingerprint-strip {
  min-height: 92px;
  padding: 12px 13px;
  border: 1px solid color-mix(in srgb, var(--el-color-info) 18%, var(--server-line));
  border-radius: 10px;
  background: color-mix(in srgb, var(--el-color-info) 4%, var(--el-bg-color));
}
.fingerprint-strip.trusted {
  border-color: color-mix(in srgb, var(--el-color-success) 23%, var(--server-line));
  background: color-mix(in srgb, var(--el-color-success) 4%, var(--el-bg-color));
}
.fingerprint-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.fingerprint-label > span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--el-text-color-primary);
  font-size: 11.5px;
  font-weight: 650;
  letter-spacing: .02em;
}
.fingerprint-label :deep(svg) {
  width: 13px;
}
.fingerprint-strip code,
.fingerprint-strip small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.fingerprint-strip code {
  margin-top: 9px;
  color: var(--el-text-color-primary);
  font-family: var(--server-code);
  font-size: 11.5px;
  font-variant-numeric: tabular-nums;
  cursor: help;
}
.fingerprint-strip small {
  margin-top: 5px;
  color: var(--server-muted);
  font-size: 10px;
}
.fingerprint-strip p {
  margin: 10px 0 0;
  color: var(--server-muted);
  font-size: 11px;
  line-height: 18px;
}
.server-error {
  margin-top: 12px;
  border: 1px solid color-mix(in srgb, var(--el-color-danger) 22%, transparent);
  border-radius: 9px;
}
.server-error :deep(.el-alert__title) {
  display: block;
  overflow: hidden;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.server-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid color-mix(in srgb, var(--server-line) 86%, transparent);
}
.server-actions .el-button + .el-button {
  margin-left: 0;
}
.server-actions :deep(.el-button) {
  min-height: 34px;
  border-radius: 8px;
}
.server-actions > .el-button:first-child {
  flex: 1;
  font-weight: 600;
  box-shadow: 0 8px 16px -12px color-mix(in srgb, var(--el-color-primary) 78%, transparent);
}
.server-actions :deep(.el-button:focus-visible) {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}
.server-empty {
  min-height: 260px;
  grid-column: 1 / -1;
}
@media (max-width: 760px) {
  .server-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 640px) {
  .server-list-actions {
    width: 100%;
    margin-left: 0 !important;
  }
  .server-list-actions :deep(.el-form-item__content) {
    justify-content: flex-end;
  }
}
@media (max-width: 420px) {
  .server-card {
    padding: 16px 15px 14px 18px;
  }
  .server-card-header {
    grid-template-columns: 38px minmax(0, 1fr);
  }
  .server-mark {
    width: 38px;
    height: 38px;
  }
  .state-badge {
    grid-column: 2;
    justify-self: start;
  }
  .server-meta {
    grid-template-columns: 1fr;
  }
}
@media (prefers-reduced-motion: reduce) {
  .server-card {
    transition: none;
  }
  .server-card:hover {
    transform: none;
  }
}
</style>

<style>
.fingerprint-confirm-box .el-message-box__message {
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
