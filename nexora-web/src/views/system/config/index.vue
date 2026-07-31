<template>
  <main class="config-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">SYSTEM CONFIGURATION</p>
        <h1>系统配置</h1>
        <p class="page-description">按业务分组维护运行参数。每次保存会整组覆盖，并立即刷新对应缓存。</p>
      </div>
      <div class="header-actions">
        <el-button
          v-permission="['sys:config:update']"
          icon="Refresh"
          :loading="refreshing"
          @click="refreshCache"
        >
          刷新缓存
        </el-button>
        <el-button
          v-permission="['sys:config:update']"
          type="primary"
          icon="Check"
          :loading="saving"
          @click="saveCurrentGroup"
        >
          保存当前分组
        </el-button>
      </div>
    </header>

    <el-card class="config-card" shadow="never">
      <el-tabs v-model="activeGroup" class="config-tabs" @tab-change="changeGroup">
        <el-tab-pane
          v-for="tab in tabs"
          :key="tab.groupCode"
          :name="tab.groupCode"
          :label="tab.groupName"
        />
      </el-tabs>

      <div class="group-heading">
        <div class="group-mark">{{ activeTab.short }}</div>
        <div>
          <h2>{{ activeSummary?.groupName }}</h2>
          <p>{{ activeTab.description }}</p>
        </div>
        <span v-if="activeSummary?.updateTime" class="update-time">
          最近更新 {{ activeSummary.updateTime }}
        </span>
      </div>

      <el-form
        ref="formRef"
        v-loading="loading"
        :model="activeForm"
        :rules="activeRules"
        label-position="top"
        class="config-form"
        @submit.prevent
      >
        <SystemConfigForm
          v-if="activeGroup === 'system'"
          v-model="forms.system"
        />
        <RegisterConfigForm
          v-else-if="activeGroup === 'register'"
          v-model="forms.register"
        />
        <LoginConfigForm
          v-else-if="activeGroup === 'login'"
          v-model="forms.login"
        />
        <PasswordConfigForm
          v-else
          v-model="forms.password"
        />
      </el-form>
    </el-card>
  </main>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules, TabPaneName } from 'element-plus'
import { ElMessage } from 'element-plus'
import {
  getConfigGroupApi,
  getConfigGroupListApi,
  refreshConfigGroupCacheApi,
  updateConfigGroupApi,
  type ConfigGroupCode,
  type ConfigValueByGroup,
  type SysConfigGroupSummary
} from '@/api/system/config'
import { usePublicConfigStore } from '@/store/modules/publicConfig'
import LoginConfigForm from './components/LoginConfigForm.vue'
import PasswordConfigForm from './components/PasswordConfigForm.vue'
import RegisterConfigForm from './components/RegisterConfigForm.vue'
import SystemConfigForm from './components/SystemConfigForm.vue'

interface TabMetadata {
  short: string
  description: string
}

const tabMetadata: Record<ConfigGroupCode, TabMetadata> = {
  system: { short: '01', description: '站点品牌、合规信息与全局水印' },
  register: { short: '02', description: '注册入口、邮箱验证、默认角色与审核流程' },
  login: { short: '03', description: '滑块验证、失败锁定、会话时长与单点登录' },
  password: { short: '04', description: '统一的新密码长度和复杂度要求' }
}

const forms = reactive<ConfigValueByGroup>({
  system: {
    siteName: '',
    shortTitle: '',
    siteDescription: '',
    siteLogo: '',
    copyright: '',
    icp: '',
    watermarkEnabled: false,
    watermarkType: 'username_time',
    watermarkCustomText: '',
    watermarkOpacity: 0.15
  },
  register: {
    enabled: true,
    verifyEmail: true,
    defaultRoleCode: 'user',
    needAudit: false
  },
  login: {
    captchaEnabled: false,
    maxRetryCount: 5,
    lockTimeMinutes: 30,
    rememberMeEnabled: true,
    sessionTimeoutSeconds: 3600,
    rememberMeTimeoutSeconds: 259200,
    singleLogin: false
  },
  password: {
    minLength: 6,
    maxLength: 20,
    requireUppercase: false,
    requireLowercase: false,
    requireNumber: false,
    requireSpecial: false
  }
})

const activeGroup = ref<ConfigGroupCode>('system')
const summaries = ref<SysConfigGroupSummary[]>([])
const loading = ref(false)
const saving = ref(false)
const refreshing = ref(false)
const formRef = ref<FormInstance>()
const publicConfigStore = usePublicConfigStore()

const tabs = computed(() => summaries.value.map(summary => ({
  ...summary,
  ...tabMetadata[summary.groupCode]
})))
const activeSummary = computed(() =>
  summaries.value.find(group => group.groupCode === activeGroup.value)
)
const activeTab = computed(() => tabMetadata[activeGroup.value])
const activeForm = computed(() => forms[activeGroup.value])

const requiredText = (message: string, max: number) => [
  { required: true, whitespace: true, message, trigger: 'blur' },
  { max, message: `不能超过 ${max} 个字符`, trigger: 'blur' }
]

const rulesByGroup: Record<ConfigGroupCode, FormRules> = {
  system: {
    siteName: requiredText('请输入站点名称', 100),
    shortTitle: requiredText('请输入后台短标题', 100),
    siteDescription: [{ required: true, message: '请输入站点描述', trigger: 'blur' }],
    siteLogo: [{ max: 1024, message: 'Logo 地址不能超过 1024 个字符', trigger: 'blur' }],
    copyright: [{ max: 255, message: '版权信息不能超过 255 个字符', trigger: 'blur' }],
    icp: [{ max: 100, message: 'ICP备案信息不能超过 100 个字符', trigger: 'blur' }],
    watermarkCustomText: [{
      validator: (_rule, value: string, callback) => {
        if (forms.system.watermarkType === 'custom' && !value.trim()) {
          callback(new Error('请输入自定义水印文本'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }]
  },
  register: {
    defaultRoleCode: requiredText('请输入默认角色编码', 50)
  },
  login: {
    rememberMeTimeoutSeconds: [{
      validator: (_rule, value: number, callback) => {
        if (value < forms.login.sessionTimeoutSeconds) {
          callback(new Error('记住我会话时长不能小于普通会话时长'))
          return
        }
        callback()
      },
      trigger: 'change'
    }]
  },
  password: {
    maxLength: [{
      validator: (_rule, value: number, callback) => {
        if (value < forms.password.minLength) {
          callback(new Error('密码最大长度不能小于最小长度'))
          return
        }
        callback()
      },
      trigger: 'change'
    }]
  }
}

const activeRules = computed(() => rulesByGroup[activeGroup.value])

async function loadTypedGroup<T extends ConfigGroupCode>(
  groupCode: T,
  target: ConfigValueByGroup[T]
) {
  const { data } = await getConfigGroupApi(groupCode)
  Object.assign(target, data.configValue)
}

const groupLoaders: Record<ConfigGroupCode, () => Promise<void>> = {
  system: () => loadTypedGroup('system', forms.system),
  register: () => loadTypedGroup('register', forms.register),
  login: () => loadTypedGroup('login', forms.login),
  password: () => loadTypedGroup('password', forms.password)
}

const groupSavers: Record<ConfigGroupCode, () => Promise<unknown>> = {
  system: () => updateConfigGroupApi('system', forms.system),
  register: () => updateConfigGroupApi('register', forms.register),
  login: () => updateConfigGroupApi('login', forms.login),
  password: () => updateConfigGroupApi('password', forms.password)
}

async function loadGroup(groupCode: ConfigGroupCode) {
  loading.value = true
  try {
    await groupLoaders[groupCode]()
    await nextTick()
    formRef.value?.clearValidate()
  } finally {
    loading.value = false
  }
}

async function loadPage() {
  const { data } = await getConfigGroupListApi()
  summaries.value = data
  await loadGroup(activeGroup.value)
}

function isConfigGroupCode(value: string): value is ConfigGroupCode {
  return Object.hasOwn(groupLoaders, value)
}

function changeGroup(name: TabPaneName) {
  const groupCode = String(name)
  if (!isConfigGroupCode(groupCode)) return
  activeGroup.value = groupCode
  void loadGroup(groupCode)
}

async function saveCurrentGroup() {
  if (!formRef.value || !(await formRef.value.validate().catch(() => false))) return
  saving.value = true
  try {
    const groupCode = activeGroup.value
    await groupSavers[groupCode]()
    ElMessage.success(`${activeSummary.value?.groupName ?? '配置分组'}已保存`)
    await loadGroup(groupCode)
    const { data } = await getConfigGroupListApi()
    summaries.value = data
    if (!(await publicConfigStore.load(true))) {
      ElMessage.warning('配置已保存，但公共配置刷新失败，请稍后重试')
    }
  } finally {
    saving.value = false
  }
}

async function refreshCache() {
  refreshing.value = true
  try {
    await refreshConfigGroupCacheApi()
    await loadGroup(activeGroup.value)
    ElMessage.success('全部配置缓存已刷新')
  } finally {
    refreshing.value = false
  }
}

onMounted(() => void loadPage())
</script>

<style scoped lang="scss">
.config-page {
  min-width: 0;
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 20px;

  h1 {
    margin: 4px 0 6px;
    color: var(--el-text-color-primary);
    font-size: 26px;
    letter-spacing: -0.02em;
  }
}

.eyebrow {
  margin: 0;
  color: var(--el-color-primary);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.page-description,
.group-heading p {
  margin: 0;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}

.header-actions {
  display: flex;
  flex-shrink: 0;
  gap: 10px;
}

.config-card {
  border-color: var(--el-border-color-lighter);
}

.config-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 0;
  }
}

.group-heading {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 24px 0 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);

  h2 {
    margin: 0 0 4px;
    font-size: 18px;
  }
}

.group-mark {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 700;
  background: var(--el-color-primary-light-9);
  border: 1px solid var(--el-color-primary-light-7);
  border-radius: 10px;
}

.update-time {
  margin-left: auto;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.config-form {
  max-width: 980px;
}

:deep(.form-section) {
  padding: 26px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);

  &:last-child {
    border-bottom: 0;
  }
}

:deep(.section-title) {
  margin-bottom: 20px;

  h3 {
    margin: 0 0 5px;
    font-size: 15px;
  }

  p {
    margin: 0;
    color: var(--el-text-color-secondary);
    line-height: 1.6;
  }
}

:deep(.form-grid) {
  display: grid;
  gap: 0 24px;
}

:deep(.two-columns) {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

:deep(.full-width) {
  grid-column: 1 / -1;
}

:deep(.logo-field) {
  display: flex;
  width: 100%;
  gap: 12px;
}

:deep(.logo-preview) {
  display: grid;
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  place-items: center;
  overflow: hidden;
  color: var(--el-text-color-secondary);
  font-size: 11px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color);
  border-radius: 8px;

  img {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
}

:deep(.switch-list) {
  margin-bottom: 22px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
}

:deep(.switch-row) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  min-height: 66px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);

  &:last-child {
    border-bottom: 0;
  }

  strong,
  span {
    display: block;
  }

  strong {
    margin-bottom: 4px;
    color: var(--el-text-color-primary);
    font-size: 14px;
  }

  span {
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 1.5;
  }
}

:deep(.compact-field) {
  max-width: 460px;
}

:deep(.field-tip) {
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

:deep(.el-input-number) {
  width: 100%;
}

@media (max-width: 760px) {
  .page-header {
    align-items: stretch;
    flex-direction: column;
  }

  .header-actions {
    justify-content: flex-end;
  }

  .group-heading {
    align-items: flex-start;
  }

  .update-time {
    display: none;
  }

  :deep(.two-columns) {
    grid-template-columns: 1fr;
  }
}
</style>
