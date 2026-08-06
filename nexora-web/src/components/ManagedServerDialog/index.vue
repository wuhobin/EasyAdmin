<template>
  <el-dialog
    v-model="visible"
    :title="form.id ? '编辑服务器' : '添加服务器'"
    width="620px"
    destroy-on-close
  >
    <p class="dialog-form-intro">配置 SSH 连接信息。每个用户只能查看和操作自己添加的服务器。</p>
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
        <p class="field-tip">
          默认不保存。临时密码只用于连接测试或打开终端，不会写入数据库。
        </p>
      </el-form-item>

      <el-form-item label="描述" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="记录用途、环境或负责人等信息"
        />
      </el-form-item>

      <div class="form-grid compact">
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </div>
    </el-form>

    <el-alert
      title="出于安全考虑，不能连接 Nexora 所在主机、回环、链路本地或保留地址。"
      type="info"
      :closable="false"
      show-icon
    />

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
.field-tip {
  margin: 1px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.6;
}
.endpoint-grid :deep(.el-input-number),
.form-grid.compact :deep(.el-input-number) {
  width: 100%;
}
@media (max-width: 640px) {
  .form-grid,
  .endpoint-grid,
  .form-grid.compact {
    grid-template-columns: 1fr;
  }
}
</style>
