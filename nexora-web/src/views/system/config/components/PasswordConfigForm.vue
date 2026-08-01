<template>
  <section class="form-section">
    <div class="section-content">
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
    </div>
  </section>

  <section class="form-section">
    <div class="section-content form-grid two-columns">
      <el-form-item label="必须包含大写字母" prop="requireUppercase">
        <el-switch v-model="model.requireUppercase" />
        <span class="form-hint">A–Z</span>
      </el-form-item>
      <el-form-item label="必须包含小写字母" prop="requireLowercase">
        <el-switch v-model="model.requireLowercase" />
        <span class="form-hint">a–z</span>
      </el-form-item>
      <el-form-item label="必须包含数字" prop="requireNumber">
        <el-switch v-model="model.requireNumber" />
        <span class="form-hint">0–9</span>
      </el-form-item>
      <el-form-item label="必须包含特殊字符" prop="requireSpecial">
        <el-switch v-model="model.requireSpecial" />
        <span class="form-hint">非字母、非数字字符</span>
      </el-form-item>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { PasswordConfig } from '@/api/system/config'

const model = defineModel<PasswordConfig>({ required: true })
</script>
