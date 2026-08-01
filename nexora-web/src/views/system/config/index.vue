<template>
  <main class="config-page">
    <el-card class="config-card" shadow="never">
      <div class="config-toolbar">
        <el-tabs v-model="activeGroup" class="config-tabs" @tab-change="changeGroup">
          <el-tab-pane
            v-for="tab in summaries"
            :key="tab.groupCode"
            :name="tab.groupCode"
            :label="tab.groupName"
          />
        </el-tabs>
        <span v-if="activeSummary?.updateTime" class="update-time">
          最近更新 {{ activeSummary.updateTime }}
        </span>
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
      </div>

      <el-form
        ref="formRef"
        v-loading="loading"
        :model="activeForm"
        :rules="activeRules"
        label-position="right"
        :label-width="labelWidths[activeGroup]"
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
const labelWidths: Record<ConfigGroupCode, string> = {
  system: '120px',
  register: '120px',
  login: '180px',
  password: '150px'
}
const summaries = ref<SysConfigGroupSummary[]>([])
const loading = ref(false)
const saving = ref(false)
const refreshing = ref(false)
const formRef = ref<FormInstance>()
const publicConfigStore = usePublicConfigStore()

const activeSummary = computed(() =>
  summaries.value.find(group => group.groupCode === activeGroup.value)
)
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
  --config-surface: var(--el-bg-color, #fff);
  --config-ink: var(--el-text-color-primary, #18212f);
  --config-muted: var(--el-text-color-secondary, #697586);
  --config-border: color-mix(in srgb, var(--el-border-color-light, #e5eaf2) 88%, var(--el-color-primary) 12%);
  --config-soft: color-mix(in srgb, var(--el-color-primary) 4%, var(--config-surface));

  width: 100%;
  min-width: 0;
}

.config-card {
  overflow: hidden;
  min-height: calc(100vh - 140px);
  background: var(--config-surface);
  border: 1px solid var(--config-border);
  border-radius: 12px;
  box-shadow: 0 1px 2px rgb(15 23 42 / 4%), 0 12px 32px -28px rgb(15 23 42 / 36%);
}

.config-card :deep(.el-card__body) {
  min-width: 0;
  padding: 0;
}

.config-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 56px;
  padding: 0 20px;
  background: var(--config-surface);
  border-bottom: 1px solid var(--config-border);
}

.config-tabs {
  flex: 1;
  min-width: 0;

  :deep(.el-tabs__header) {
    margin-bottom: 0;
  }

  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }

  :deep(.el-tabs__item) {
    height: 56px;
    padding: 0 16px;
    color: var(--config-muted);
    font-weight: 600;
  }

  :deep(.el-tabs__item.is-active) {
    color: var(--el-color-primary);
  }

  :deep(.el-tabs__active-bar) {
    height: 3px;
    border-radius: 3px 3px 0 0;
  }

  :deep(.el-tabs__content) {
    display: none;
  }
}

.header-actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 10px;
}

.header-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.update-time {
  flex-shrink: 0;
  color: var(--config-muted);
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", monospace;
  font-size: 12px;
  white-space: nowrap;
}

.config-form {
  width: 100%;
  max-width: 660px;
  padding: 20px 24px 16px;
  margin: 0;
  box-sizing: border-box;
}

:deep(.form-section) {
  padding: 24px 0;
  border-bottom: 1px solid var(--config-border);

  &:last-child {
    border-bottom: 0;
  }
}

:deep(.section-content) {
  min-width: 0;
}

:deep(.form-grid) {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
}

:deep(.two-columns) {
  grid-template-columns: minmax(0, 1fr);
}

:deep(.full-width) {
  grid-column: 1 / -1;
}

:deep(.logo-field) {
  display: flex;
  width: 100%;
  max-width: 400px;
  gap: 12px;
}

:deep(.logo-field .el-input) {
  min-width: 0;
  flex: 1;
}

:deep(.logo-preview) {
  display: grid;
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  place-items: center;
  overflow: hidden;
  color: var(--config-muted);
  font-size: 11px;
  background: var(--config-soft);
  border: 1px solid var(--config-border);
  border-radius: 7px;

  img {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
}

:deep(.form-hint) {
  margin-left: 12px;
  color: var(--config-muted);
  font-size: 13px;
  line-height: 1.5;
}

:deep(.el-form-item__label) {
  color: var(--el-text-color-regular);
  font-weight: 500;
  white-space: nowrap;
}

:deep(.el-input),
:deep(.el-textarea),
:deep(.el-select),
:deep(.el-input-number),
:deep(.el-slider),
:deep(.el-alert) {
  width: 100%;
  max-width: 400px;
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  min-height: 36px;
}

:deep(.el-input-number) {
  width: 240px;
  max-width: 100%;
}

:deep(.el-input-number .el-input__wrapper) {
  min-height: 38px;
  padding-right: 56px;
  padding-left: 14px;
}

:deep(.el-input-number .el-input__inner) {
  text-align: left;
}

:deep(.el-input-number__decrease),
:deep(.el-input-number__increase) {
  top: 1px;
  bottom: 1px;
  left: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: auto;
  color: var(--config-muted);
  line-height: normal;
  background: transparent;
  border: 0;
  border-radius: 0;
}

:deep(.el-input-number__decrease) {
  right: 28px;
}

:deep(.el-input-number__increase) {
  right: 8px;
}

:deep(.el-input-number__decrease:not(.is-disabled):hover),
:deep(.el-input-number__increase:not(.is-disabled):hover) {
  color: var(--el-color-primary);
}

@media (max-width: 760px) {
  .config-toolbar {
    align-items: stretch;
    flex-direction: column;
    gap: 0;
    padding: 0 18px 16px;
  }

  .header-actions {
    justify-content: flex-end;
  }

  .update-time {
    display: none;
  }

  .config-form {
    padding: 18px 18px 12px;
  }

  .config-tabs :deep(.el-tabs__item) {
    height: 56px;
    padding: 0 12px;
  }

  :deep(.el-form-item) {
    display: block;
  }

  :deep(.el-form-item__label) {
    width: auto !important;
    height: auto;
    justify-content: flex-start;
    padding: 0 0 8px;
    line-height: 1.5;
  }

  :deep(.el-form-item__content) {
    margin-left: 0 !important;
  }

  :deep(.two-columns) {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .header-actions :deep(.el-button) {
    flex: 1;
    margin-left: 0;
  }

  :deep(.form-section) {
    padding: 22px 0;
  }
}
</style>
