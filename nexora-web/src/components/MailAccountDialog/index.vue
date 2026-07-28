<template>
  <el-dialog v-model="visible" :title="form.id ? '编辑邮箱账户' : '添加邮箱账户'" width="520px" destroy-on-close>
    <p class="dialog-form-intro">配置邮箱服务信息，用于收取和发送邮件。</p>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <div class="form-grid">
        <el-form-item label="账户名称" prop="accountName">
          <el-input v-model="form.accountName" placeholder="例如：工作 QQ 邮箱" />
        </el-form-item>
        <el-form-item label="邮箱类型" prop="provider">
          <el-select v-model="form.provider" :loading="providerOptionsLoading" style="width: 100%">
            <el-option v-for="item in providerOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </div>
      <el-form-item label="邮箱地址" prop="email">
        <el-input v-model="emailAccount" placeholder="请输入邮箱账号/手机号" autocomplete="email" @blur="validateEmail">
          <template #suffix><span class="email-domain-suffix">{{ emailDomainSuffix }}</span></template>
        </el-input>
      </el-form-item>
      <el-form-item label="邮箱授权码" prop="authCode">
        <el-input
          v-model="form.authCode"
          type="password"
          show-password
          autocomplete="new-password"
          :placeholder="form.id ? '留空表示不修改' : '不是邮箱登录密码，请填写 IMAP 授权码'"
        />
        <p class="auth-tip">请先在邮箱设置中开启 IMAP/SMTP 服务并生成授权码。</p>
      </el-form-item>
      <div class="form-grid compact">
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </div>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存账户</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { addMailAccountApi, updateMailAccountApi } from '@/api/mail'
import type { MailAccount, MailAccountForm, MailProvider, MailProviderConfig } from '@/api/mail'
import {
  buildMailAddress,
  isMailAddressForDomain,
  mailAddressAccount,
  replaceMailProviderDomain
} from '@/utils/mail-provider'

const props = defineProps<{
  modelValue: boolean
  account?: MailAccount
  providerOptions: MailProviderConfig[]
  providerOptionsLoading?: boolean
}>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: []
}>()

const formRef = ref<FormInstance>()
const saving = ref(false)
const initializing = ref(false)
const form = reactive<MailAccountForm>(emptyForm())
const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})
const domainOf = (provider: MailProvider) => props.providerOptions.find((item) => item.value === provider)?.domain || ''
const enabled = computed({
  get: () => form.enabled === 1,
  set: (value: boolean) => { form.enabled = value ? 1 : 0 }
})
const emailAccount = computed({
  get: () => mailAddressAccount(form.email),
  set: (value: string) => { form.email = buildMailAddress(value, domainOf(form.provider)) }
})
const emailDomainSuffix = computed(() => {
  const domain = domainOf(form.provider)
  return domain ? `@${domain}` : ''
})
const rules: FormRules<MailAccountForm> = {
  accountName: [{ required: true, message: '请输入账户名称', trigger: 'blur' }],
  provider: [{ required: true, message: '请选择邮箱类型', trigger: 'change' }],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        const domain = domainOf(form.provider)
        if (!value || isMailAddressForDomain(value, domain)) callback()
        else callback(new Error(`当前邮箱类型要求地址以 @${domain} 结尾`))
      },
      trigger: 'blur'
    }
  ],
  authCode: [{
    validator: (_rule, value, callback) => {
      if (!form.id && !value) callback(new Error('请输入邮箱授权码'))
      else callback()
    },
    trigger: 'blur'
  }]
}

function defaultProvider(): MailProvider {
  return props.providerOptions.find((item) => item.defaultProvider)?.value
    || props.providerOptions[0]?.value
    || ('' as MailProvider)
}

function emptyForm(): MailAccountForm {
  return { accountName: '', provider: '' as MailProvider, email: '', authCode: '', enabled: 1, sort: 0 }
}

const initialize = async () => {
  initializing.value = true
  Object.assign(form, props.account ? {
    id: props.account.id,
    accountName: props.account.accountName,
    provider: props.account.provider,
    email: props.account.email,
    authCode: '',
    enabled: props.account.enabled,
    sort: props.account.sort
  } : { ...emptyForm(), provider: defaultProvider() })
  await nextTick()
  formRef.value?.clearValidate()
  initializing.value = false
}

const validateEmail = () => formRef.value?.validateField('email').catch(() => undefined)

const save = async () => {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (form.id) await updateMailAccountApi({ ...form })
    else await addMailAccountApi({ ...form })
    ElMessage.success(form.id ? '邮箱账户已更新' : '邮箱账户已添加')
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

watch(() => props.modelValue, (value) => {
  if (value) void initialize()
})

watch(() => props.providerOptions, () => {
  if (props.modelValue && !form.provider) form.provider = defaultProvider()
})

watch(() => form.provider, (provider, previousProvider) => {
  if (initializing.value || !previousProvider) return
  form.email = replaceMailProviderDomain(form.email, domainOf(previousProvider), domainOf(provider))
  nextTick(() => {
    if (form.email) void validateEmail()
  })
})
</script>

<style scoped>
.form-grid { display: grid; grid-template-columns: 1.2fr 1fr; column-gap: 24px; }
.form-grid.compact { grid-template-columns: 1fr 1fr; align-items: center; }
.email-domain-suffix { color: var(--el-text-color-secondary); font-size: 15px; white-space: nowrap; }
.auth-tip { margin: 6px 0 0; color: var(--el-text-color-secondary); font-size: 12px; }
</style>
