<template>
  <section class="form-section">
    <div class="section-content form-grid">
      <el-form-item label="启用邮件" prop="enabled">
        <el-switch v-model="model.enabled" />
      </el-form-item>
      <el-form-item label="SMTP服务器" prop="host">
        <el-input
          v-model.trim="model.host"
          maxlength="255"
          placeholder="如 smtp.qq.com"
        />
      </el-form-item>
      <el-form-item label="端口" prop="port">
        <el-input-number v-model="model.port" :min="1" :max="65535" />
      </el-form-item>
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model.trim="model.username"
          maxlength="255"
          placeholder="SMTP 登录用户名"
        />
      </el-form-item>
      <el-form-item label="密码/授权码" prop="password">
        <el-input
          v-model="model.password"
          type="password"
          maxlength="255"
          show-password
          placeholder="邮箱密码或授权码"
        />
      </el-form-item>
      <el-form-item label="发件人名称" prop="fromName">
        <el-input
          v-model.trim="model.fromName"
          maxlength="100"
          placeholder="显示的发件人名称"
        />
      </el-form-item>
      <el-form-item label="SSL加密" prop="ssl">
        <el-switch v-model="model.ssl" />
      </el-form-item>
    </div>
  </section>

  <section class="form-section">
    <div class="section-content">
      <el-form-item label="测试邮件">
        <div class="test-email-field">
          <el-input
            v-model.trim="testEmailAddress"
            placeholder="输入测试收件人邮箱"
          />
          <el-button
            v-permission="['sys:config:update']"
            type="primary"
            :loading="testing"
            :disabled="!model.enabled"
            @click="sendTestEmail"
          >
            发送测试邮件
          </el-button>
        </div>
      </el-form-item>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { testConfigEmailApi, type EmailConfig } from '@/api/system/config'

const model = defineModel<EmailConfig>({ required: true })
const testEmailAddress = ref('')
const testing = ref(false)
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

async function sendTestEmail() {
  if (!emailPattern.test(testEmailAddress.value)) {
    ElMessage.warning('请输入正确的测试收件人邮箱')
    return
  }
  testing.value = true
  try {
    await testConfigEmailApi(testEmailAddress.value)
    ElMessage.success('测试邮件发送成功，请查收')
  } finally {
    testing.value = false
  }
}
</script>

<style scoped lang="scss">
.test-email-field {
  display: flex;
  width: 100%;
  max-width: 400px;
  gap: 10px;

  .el-input {
    min-width: 0;
    flex: 1;
  }

  .el-button {
    flex-shrink: 0;
  }
}

@media (max-width: 480px) {
  .test-email-field {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
