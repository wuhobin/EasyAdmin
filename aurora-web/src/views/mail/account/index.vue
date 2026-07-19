<template>
  <div class="account-page">
    <header class="page-hero">
      <div>
        <p class="eyebrow">MAILBOX DIRECTORY</p>
        <h1>邮箱列表</h1>
        <p>集中维护聚合收件箱使用的邮箱账户与 IMAP 授权码。</p>
      </div>
      <div class="hero-actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadAccounts">刷新</el-button>
        <el-button v-permission="['mail:account:add']" type="primary" :icon="Plus" @click="openCreateDialog">
          添加邮箱
        </el-button>
      </div>
    </header>

    <section class="account-stats">
      <div class="stat-card">
        <span class="stat-icon total"><Message /></span>
        <div><strong>{{ accounts.length }}</strong><small>邮箱总数</small></div>
      </div>
      <div class="stat-card">
        <span class="stat-icon active"><CircleCheck /></span>
        <div><strong>{{ enabledCount }}</strong><small>已启用</small></div>
      </div>
      <div class="stat-card">
        <span class="stat-icon warning"><Warning /></span>
        <div><strong>{{ errorCount }}</strong><small>连接异常</small></div>
      </div>
    </section>

    <el-card class="account-card" shadow="never">
      <div class="filter-bar">
        <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="搜索账户名称或邮箱地址" />
        <el-select v-model="providerFilter" clearable placeholder="全部邮箱类型">
          <el-option v-for="item in providerOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="statusFilter" clearable placeholder="全部状态">
          <el-option label="已启用" :value="1" />
          <el-option label="已停用" :value="0" />
        </el-select>
        <span class="filter-result">显示 {{ filteredAccounts.length }} 个账户</span>
      </div>

      <el-table v-loading="loading" :data="filteredAccounts" row-key="id" class="account-table">
        <el-table-column label="邮箱账户" min-width="230">
          <template #default="{ row }">
            <div class="account-cell">
              <span :class="['provider-avatar', providerClass(row.provider)]">{{ providerMark(row.provider) }}</span>
              <div><strong>{{ row.accountName }}</strong><small>{{ row.email }}</small></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="邮箱类型" width="125" align="center">
          <template #default="{ row }">
            <el-tag effect="plain" round>{{ providerLabel(row.provider) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <span :class="['state-badge', row.enabled === 1 ? 'enabled' : 'disabled']">
              <i />{{ row.enabled === 1 ? '已启用' : '已停用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="连接状态" min-width="150" align="center">
          <template #default="{ row }">
            <el-tooltip v-if="row.lastError" :content="row.lastError" placement="top">
              <span class="connection-state error"><WarningFilled />连接异常</span>
            </el-tooltip>
            <span v-else-if="row.lastConnectTime" class="connection-state success"><CircleCheckFilled />连接正常</span>
            <span v-else class="connection-state pending"><Clock />尚未测试</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastConnectTime" label="最后连接" min-width="170" align="center">
          <template #default="{ row }">{{ row.lastConnectTime || '-' }}</template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="操作" width="250" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link :icon="View" @click="openDetail(row)">查看</el-button>
            <el-button
              v-permission="['mail:account:test']"
              link
              :icon="Connection"
              :loading="testingId === row.id"
              @click="testConnection(row)"
            >测试</el-button>
            <el-button v-permission="['mail:account:update']" link :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-button
              v-permission="['mail:account:delete']"
              link
              type="danger"
              :icon="Delete"
              @click="deleteAccount(row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !filteredAccounts.length" description="没有符合条件的邮箱账户" />
    </el-card>

    <el-drawer v-model="detailVisible" title="邮箱账户详情" size="430px">
      <template v-if="detailAccount">
        <div class="drawer-account">
          <span :class="['provider-avatar large', providerClass(detailAccount.provider)]">
            {{ providerMark(detailAccount.provider) }}
          </span>
          <div><h3>{{ detailAccount.accountName }}</h3><p>{{ detailAccount.email }}</p></div>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="邮箱类型">{{ providerLabel(detailAccount.provider) }}</el-descriptions-item>
          <el-descriptions-item label="账户状态">{{ detailAccount.enabled === 1 ? '已启用' : '已停用' }}</el-descriptions-item>
          <el-descriptions-item label="排序">{{ detailAccount.sort }}</el-descriptions-item>
          <el-descriptions-item label="最后连接">{{ detailAccount.lastConnectTime || '尚未连接' }}</el-descriptions-item>
          <el-descriptions-item label="连接结果">
            <span :class="detailAccount.lastError ? 'error-text' : 'success-text'">
              {{ detailAccount.lastError || (detailAccount.lastConnectTime ? '连接正常' : '尚未测试') }}
            </span>
          </el-descriptions-item>
        </el-descriptions>
        <div class="drawer-note"><Lock />授权码已经加密保存，不会在详情或编辑接口中返回。</div>
      </template>
    </el-drawer>

    <el-dialog v-model="dialogVisible" :title="accountForm.id ? '编辑邮箱账户' : '添加邮箱账户'" width="520px">
      <el-form ref="formRef" :model="accountForm" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="账户名称" prop="accountName">
            <el-input v-model="accountForm.accountName" placeholder="例如：工作 QQ 邮箱" />
          </el-form-item>
          <el-form-item label="邮箱类型" prop="provider">
            <el-select v-model="accountForm.provider" style="width: 100%">
              <el-option v-for="item in providerOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="邮箱地址" prop="email">
          <el-input v-model="accountForm.email" type="email" placeholder="name@qq.com" autocomplete="email" />
        </el-form-item>
        <el-form-item label="邮箱授权码" prop="authCode">
          <el-input
            v-model="accountForm.authCode"
            type="password"
            show-password
            autocomplete="new-password"
            :placeholder="accountForm.id ? '不修改授权码请留空' : '请填写邮箱生成的 IMAP 授权码'"
          />
          <p class="auth-tip">这里填写的是邮箱授权码，不是邮箱登录密码。</p>
        </el-form-item>
        <div class="form-grid compact">
          <el-form-item label="排序" prop="sort">
            <el-input-number v-model="accountForm.sort" :min="0" :max="999" />
          </el-form-item>
          <el-form-item label="账户状态">
            <el-switch v-model="accountEnabled" active-text="启用" inactive-text="停用" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAccount">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {
  CircleCheck,
  CircleCheckFilled,
  Clock,
  Connection,
  Delete,
  Edit,
  Lock,
  Message,
  Plus,
  Refresh,
  Search,
  View,
  Warning,
  WarningFilled
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  addMailAccountApi,
  deleteMailAccountApi,
  getMailAccountsApi,
  testMailAccountApi,
  updateMailAccountApi,
  type MailAccount,
  type MailAccountForm,
  type MailProvider
} from '@/api/mail'

const providerOptions: { label: string; value: MailProvider }[] = [
  { label: 'QQ 邮箱', value: 'QQ' },
  { label: '163 邮箱', value: 'NETEASE_163' },
  { label: '126 邮箱', value: 'NETEASE_126' },
  { label: 'yeah 邮箱', value: 'YEAH' }
]

const accounts = ref<MailAccount[]>([])
const loading = ref(false)
const saving = ref(false)
const testingId = ref<number>()
const keyword = ref('')
const providerFilter = ref<MailProvider>()
const statusFilter = ref<number>()
const dialogVisible = ref(false)
const detailVisible = ref(false)
const detailAccount = ref<MailAccount>()
const formRef = ref<FormInstance>()
const accountForm = reactive<MailAccountForm>(emptyForm())

const accountEnabled = computed({
  get: () => accountForm.enabled === 1,
  set: (value: boolean) => { accountForm.enabled = value ? 1 : 0 }
})
const enabledCount = computed(() => accounts.value.filter((item) => item.enabled === 1).length)
const errorCount = computed(() => accounts.value.filter((item) => Boolean(item.lastError)).length)
const filteredAccounts = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()
  return accounts.value.filter((account) => {
    const matchesKeyword = !normalizedKeyword
      || account.accountName.toLowerCase().includes(normalizedKeyword)
      || account.email.toLowerCase().includes(normalizedKeyword)
    const matchesProvider = !providerFilter.value || account.provider === providerFilter.value
    const matchesStatus = statusFilter.value === undefined || account.enabled === statusFilter.value
    return matchesKeyword && matchesProvider && matchesStatus
  })
})

const rules: FormRules<MailAccountForm> = {
  accountName: [{ required: true, message: '请输入账户名称', trigger: 'blur' }],
  provider: [{ required: true, message: '请选择邮箱类型', trigger: 'change' }],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  authCode: [{
    validator: (_rule, value, callback) => {
      if (!accountForm.id && !value) callback(new Error('请输入邮箱授权码'))
      else callback()
    },
    trigger: 'blur'
  }]
}

function emptyForm(): MailAccountForm {
  return { accountName: '', provider: 'QQ', email: '', authCode: '', enabled: 1, sort: 0 }
}

const loadAccounts = async () => {
  loading.value = true
  try {
    const { data } = await getMailAccountsApi()
    accounts.value = data
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  Object.assign(accountForm, emptyForm())
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const openEditDialog = (row: unknown) => {
  const account = row as MailAccount
  Object.assign(accountForm, {
    id: account.id,
    accountName: account.accountName,
    provider: account.provider,
    email: account.email,
    authCode: '',
    enabled: account.enabled,
    sort: account.sort
  })
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const openDetail = (row: unknown) => {
  const account = row as MailAccount
  detailAccount.value = account
  detailVisible.value = true
}

const saveAccount = async () => {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (accountForm.id) await updateMailAccountApi(accountForm)
    else await addMailAccountApi(accountForm)
    ElMessage.success(accountForm.id ? '邮箱账户已更新' : '邮箱账户已添加')
    dialogVisible.value = false
    await loadAccounts()
  } finally {
    saving.value = false
  }
}

const testConnection = async (row: unknown) => {
  const account = row as MailAccount
  testingId.value = account.id
  try {
    await testMailAccountApi(account.id)
    ElMessage.success(`${account.accountName} 连接成功`)
    await loadAccounts()
  } finally {
    testingId.value = undefined
  }
}

const deleteAccount = async (row: unknown) => {
  const account = row as MailAccount
  await ElMessageBox.confirm(
    `确定删除邮箱“${account.accountName}”吗？删除后不会影响邮箱服务器中的邮件。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  )
  await deleteMailAccountApi(account.id)
  ElMessage.success('邮箱账户已删除')
  await loadAccounts()
}

const providerLabel = (provider: MailProvider) => providerOptions.find((item) => item.value === provider)?.label || provider
const providerMark = (provider: MailProvider) => provider === 'QQ' ? 'Q' : provider === 'NETEASE_163' ? '163' : provider === 'NETEASE_126' ? '126' : 'Y'
const providerClass = (provider: MailProvider) => provider.toLowerCase().replace('_', '-')

onMounted(loadAccounts)
</script>

<style scoped>
.account-page {
  --ink: #20302d;
  --muted: #74847f;
  --line: #dde7e4;
  --accent: #0d7a6b;
  min-height: calc(100vh - 84px);
  padding: 24px;
  color: var(--ink);
  background:
    radial-gradient(circle at 88% 0, rgba(13, 122, 107, .08), transparent 26%),
    linear-gradient(145deg, #f8faf9 0%, #eff4f2 100%);
}
.page-hero { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 18px; }
.eyebrow { margin: 0 0 4px; color: var(--accent); font-size: 11px; font-weight: 800; letter-spacing: .18em; }
.page-hero h1 { margin: 0; font-family: Georgia, "Songti SC", serif; font-size: 32px; letter-spacing: -.03em; }
.page-hero p:not(.eyebrow) { margin: 6px 0 0; color: var(--muted); font-size: 13px; }
.hero-actions { display: flex; gap: 10px; }
.account-stats { display: grid; grid-template-columns: repeat(3, minmax(180px, 1fr)); gap: 12px; margin-bottom: 14px; }
.stat-card { display: flex; align-items: center; gap: 13px; padding: 16px 18px; border: 1px solid rgba(207, 221, 217, .9); border-radius: 13px; background: rgba(255,255,255,.88); }
.stat-icon { display: grid; place-items: center; width: 40px; height: 40px; border-radius: 11px; font-size: 20px; }
.stat-icon.total { color: #236a8d; background: #e6f3f8; }.stat-icon.active { color: #168362; background: #e2f3ed; }.stat-icon.warning { color: #bd692d; background: #fff0df; }
.stat-card strong, .stat-card small { display: block; }.stat-card strong { font-family: Georgia, serif; font-size: 24px; }.stat-card small { margin-top: 1px; color: var(--muted); font-size: 11px; }
.account-card { border-color: rgba(201, 216, 211, .9); border-radius: 14px; background: rgba(255,255,255,.94); }
.filter-bar { display: grid; grid-template-columns: minmax(260px, 1fr) 180px 150px auto; gap: 10px; align-items: center; margin-bottom: 16px; }
.filter-result { padding-left: 8px; color: var(--muted); font-size: 12px; white-space: nowrap; }
.account-cell { display: flex; align-items: center; gap: 11px; }
.account-cell strong, .account-cell small { display: block; }.account-cell strong { font-size: 13px; }.account-cell small { margin-top: 3px; color: var(--muted); font-size: 11px; }
.provider-avatar { display: grid; place-items: center; flex: 0 0 auto; width: 38px; height: 38px; border-radius: 11px; color: #fff; background: #61736f; font-size: 11px; font-weight: 800; }
.provider-avatar.qq { background: #168bd2; }.provider-avatar.netease-163 { background: #d74b3f; }.provider-avatar.netease-126 { background: #df7048; }.provider-avatar.yeah { background: #52934a; }
.provider-avatar.large { width: 52px; height: 52px; border-radius: 15px; font-size: 14px; }
.state-badge, .connection-state { display: inline-flex; align-items: center; justify-content: center; gap: 6px; font-size: 11px; }
.state-badge i { width: 7px; height: 7px; border-radius: 50%; }.state-badge.enabled { color: #168362; }.state-badge.enabled i { background: #26a77e; }.state-badge.disabled { color: #7f8c88; }.state-badge.disabled i { background: #adb7b4; }
.connection-state :deep(svg) { width: 14px; }.connection-state.success { color: #168362; }.connection-state.error { color: #d15048; cursor: help; }.connection-state.pending { color: #8a9793; }
.drawer-account { display: flex; align-items: center; gap: 14px; margin-bottom: 24px; padding: 16px; border-radius: 13px; background: #f1f6f4; }.drawer-account h3, .drawer-account p { margin: 0; }.drawer-account h3 { font-family: Georgia, "Songti SC", serif; font-size: 20px; }.drawer-account p { margin-top: 4px; color: var(--muted); font-size: 12px; }
.drawer-note { display: flex; gap: 8px; margin-top: 18px; padding: 12px; border-radius: 9px; color: #64736f; background: #f5f8f7; font-size: 12px; line-height: 1.6; }.drawer-note :deep(svg) { flex: 0 0 auto; width: 15px; margin-top: 2px; }
.error-text { color: #d15048; }.success-text { color: #168362; }
.form-grid { display: grid; grid-template-columns: 1.2fr 1fr; gap: 14px; }.form-grid.compact { grid-template-columns: 1fr 1fr; align-items: center; }
.auth-tip { margin: 6px 0 0; color: var(--el-text-color-secondary); font-size: 12px; }
@media (max-width: 900px) {
  .page-hero { align-items: flex-start; flex-direction: column; }
  .account-stats { grid-template-columns: 1fr; }
  .filter-bar { grid-template-columns: 1fr 1fr; }
  .filter-result { display: none; }
}
</style>
