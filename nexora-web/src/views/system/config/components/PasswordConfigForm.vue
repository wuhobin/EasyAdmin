<template>
  <section class="form-section">
    <div class="section-title">
      <h3>密码长度</h3>
      <p>统一应用于注册、忘记密码、个人修改、管理员新增和重置密码。</p>
    </div>
    <div class="form-grid two-columns">
      <el-form-item label="最小长度" prop="minLength">
        <el-input-number v-model="model.minLength" :min="6" :max="32" />
      </el-form-item>
      <el-form-item label="最大长度" prop="maxLength">
        <el-input-number v-model="model.maxLength" :min="6" :max="64" />
      </el-form-item>
    </div>
    <el-alert
      title="BCrypt 仅接受不超过 72 字节的 UTF-8 输入；前后端都会额外校验该限制。"
      type="info"
      :closable="false"
      show-icon
    />
  </section>

  <section class="form-section">
    <div class="section-title">
      <h3>复杂度要求</h3>
      <p>仅影响之后设置的新密码，不追溯检查已有密码。</p>
    </div>
    <div class="switch-list">
      <div class="switch-row">
        <div><strong>必须包含大写字母</strong><span>A–Z</span></div>
        <el-switch v-model="model.requireUppercase" />
      </div>
      <div class="switch-row">
        <div><strong>必须包含小写字母</strong><span>a–z</span></div>
        <el-switch v-model="model.requireLowercase" />
      </div>
      <div class="switch-row">
        <div><strong>必须包含数字</strong><span>0–9</span></div>
        <el-switch v-model="model.requireNumber" />
      </div>
      <div class="switch-row">
        <div><strong>必须包含特殊字符</strong><span>非字母、非数字字符</span></div>
        <el-switch v-model="model.requireSpecial" />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { PasswordConfig } from '@/api/system/config'

const model = defineModel<PasswordConfig>({ required: true })
</script>
