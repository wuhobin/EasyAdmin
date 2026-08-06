<template>
  <el-dialog
    v-model="visible"
    :title="form.id ? '编辑服务器' : '添加服务器'"
    width="620px"
    destroy-on-close
    class="managed-server-dialog"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <div class="form-grid">
        <el-form-item label="服务器名称" prop="name">
          <el-input v-model="form.name" maxlength="100" placeholder="例如：生产应用节点" />
        </el-form-item>
        <el-form-item label="SSH 用户名" prop="username">
          <el-input v-model="form.username" maxlength="100" autocomplete="username" placeholder="root" />
        </el-form-item>
      </div>

      <div class="endpoint-grid">
        <el-form-item label="服务器地址" prop="host">
          <el-input v-model="form.host" maxlength="255" placeholder="公网或远程内网 IP / 域名" />
        </el-form-item>
        <el-form-item label="SSH 端口" prop="port">
          <el-input-number v-model="form.port" :min="1" :max="65535" controls-position="right" />
        </el-form-item>
      </div>

      <div class="details-grid">
        <el-form-item label="SSH 密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            maxlength="512"
            autocomplete="new-password"
            :placeholder="form.id ? '留空表示不修改已保存的密码' : '可留空，连接时再临时输入'"
          />
          <div class="password-options">
            <el-checkbox v-model="form.savePassword" :disabled="!form.password">
              加密保存本次填写的密码
            </el-checkbox>
            <el-checkbox
              v-if="server?.hasSavedPassword"
              v-model="form.clearSavedPassword"
              :disabled="form.savePassword"
            >
              清除已保存密码
            </el-checkbox>
          </div>
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
            placeholder="记录用途、环境或负责人等信息"
          />
        </el-form-item>
      </div>

      <div class="form-grid compact">
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">
        {{ form.id ? '保存修改' : '添加服务器' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  addManagedServerApi,
  updateManagedServerApi,
  type ManagedServer,
  type ManagedServerForm
} from '@/api/monitor/server'

const props = defineProps<{
  modelValue: boolean
  server?: ManagedServer
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: []
}>()

const formRef = ref<FormInstance>()
const saving = ref(false)
const form = reactive<ManagedServerForm>(emptyForm())
const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})
const enabled = computed({
  get: () => form.enabled === 1,
  set: (value: boolean) => { form.enabled = value ? 1 : 0 }
})
const rules: FormRules<ManagedServerForm> = {
  name: [{ required: true, message: '请输入服务器名称', trigger: 'blur' }],
  host: [{ required: true, message: '请输入服务器地址', trigger: 'blur' }],
  port: [
    { required: true, message: '请输入 SSH 端口', trigger: 'change' },
    { type: 'number', min: 1, max: 65535, message: 'SSH 端口范围为 1-65535', trigger: 'change' }
  ],
  username: [{ required: true, message: '请输入 SSH 用户名', trigger: 'blur' }]
}

function emptyForm(): ManagedServerForm {
  return {
    name: '',
    host: '',
    port: 22,
    username: 'root',
    password: '',
    savePassword: false,
    clearSavedPassword: false,
    description: '',
    enabled: 1,
    sort: 0
  }
}

async function initialize() {
  Object.assign(form, props.server ? {
    id: props.server.id,
    name: props.server.name,
    host: props.server.host,
    port: props.server.port,
    username: props.server.username,
    password: '',
    savePassword: false,
    clearSavedPassword: false,
    description: props.server.description || '',
    enabled: props.server.enabled,
    sort: props.server.sort
  } : emptyForm())
  await nextTick()
  formRef.value?.clearValidate()
}

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload: ManagedServerForm = {
      ...form,
      password: form.password || undefined,
      savePassword: Boolean(form.password && form.savePassword),
      clearSavedPassword: Boolean(form.clearSavedPassword && !form.savePassword)
    }
    if (form.id) await updateManagedServerApi(payload)
    else await addManagedServerApi(payload)
    ElMessage.success(form.id ? '服务器配置已更新' : '服务器已添加')
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

watch(() => props.modelValue, (value) => {
  if (value) void initialize()
})

watch(() => form.password, (password) => {
  if (!password) form.savePassword = false
})

watch(() => form.savePassword, (savePassword) => {
  if (savePassword) form.clearSavedPassword = false
})
</script>

<style scoped>
:global(.managed-server-dialog .el-dialog__body) {
  overflow: hidden;
  padding: 12px 24px 0;
}

:global(.managed-server-dialog .el-form-item) {
  margin-bottom: 10px;
}

:global(.managed-server-dialog .el-form-item__label) {
  margin-bottom: 3px;
  line-height: 18px;
}

:global(.managed-server-dialog .el-input__wrapper),
:global(.managed-server-dialog .el-select__wrapper) {
  min-height: 38px;
}

:global(.managed-server-dialog .el-textarea__inner) {
  min-height: 72px;
  padding-block: 8px;
}

:global(.managed-server-dialog .form-grid:not(.compact)),
:global(.managed-server-dialog .endpoint-grid) {
  margin-bottom: 10px;
}

:global(.managed-server-dialog .el-dialog__body > .el-form:last-child .el-form-item:last-child) {
  margin-bottom: 0;
}

.form-grid {
  display: grid;
  grid-template-columns: 1.25fr 1fr;
  column-gap: 20px;
}
.endpoint-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 140px;
  column-gap: 20px;
}
.details-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 20px;
}
.details-grid > :deep(.el-form-item) {
  min-width: 0;
}
.form-grid.compact {
  grid-template-columns: 1fr 1fr;
  align-items: center;
}
.password-options {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 18px;
  width: 100%;
  margin-top: 5px;
}
.password-options :deep(.el-checkbox + .el-checkbox) {
  margin-left: 0;
}
.endpoint-grid :deep(.el-input-number),
.form-grid.compact :deep(.el-input-number) {
  width: 100%;
}
@media (max-width: 640px) {
  :global(.managed-server-dialog .form-grid) {
    grid-template-columns: 1.25fr 1fr !important;
  }

  :global(.managed-server-dialog .endpoint-grid) {
    grid-template-columns: minmax(0, 1fr) 140px !important;
  }

  :global(.managed-server-dialog .form-grid.compact) {
    grid-template-columns: 1fr 1fr !important;
  }
}

@media (max-width: 480px) {
  :global(.managed-server-dialog .form-grid),
  :global(.managed-server-dialog .endpoint-grid),
  :global(.managed-server-dialog .form-grid.compact),
  :global(.managed-server-dialog .details-grid) {
    grid-template-columns: 1fr !important;
    row-gap: 20px;
  }

  :global(.managed-server-dialog .form-grid > .el-form-item),
  :global(.managed-server-dialog .endpoint-grid > .el-form-item),
  :global(.managed-server-dialog .form-grid.compact > .el-form-item),
  :global(.managed-server-dialog .details-grid > .el-form-item) {
    margin-bottom: 0;
  }
}
</style>
