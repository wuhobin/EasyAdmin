<template>
  <section class="form-section">
    <div class="section-content form-grid two-columns">
      <el-form-item label="登录滑块验证" prop="captchaEnabled">
        <el-switch v-model="model.captchaEnabled" />
        <span class="form-hint">滑块失败不累计密码错误。</span>
      </el-form-item>
      <el-form-item label="允许记住我" prop="rememberMeEnabled">
        <el-switch v-model="model.rememberMeEnabled" />
        <span class="form-hint">关闭后使用普通会话时长。</span>
      </el-form-item>
      <el-form-item label="单点登录" prop="singleLogin">
        <el-switch v-model="model.singleLogin" />
        <span class="form-hint">新会话建立前踢出其他会话。</span>
      </el-form-item>
    </div>
  </section>

  <section class="form-section">
    <div class="section-content form-grid two-columns">
      <el-form-item label="最大密码重试次数" prop="maxRetryCount">
        <el-input-number
          v-model="model.maxRetryCount"
          :min="1"
          :max="20"
        />
      </el-form-item>
      <el-form-item label="锁定时间（分钟）" prop="lockTimeMinutes">
        <el-input-number v-model="model.lockTimeMinutes" :min="1" :max="1440" />
      </el-form-item>
      <el-form-item label="普通会话时长（秒）" prop="sessionTimeoutSeconds">
        <el-input-number
          v-model="model.sessionTimeoutSeconds"
          :min="300"
          :max="86400"
          :step="300"
        />
      </el-form-item>
      <el-form-item label="记住我会话时长（秒）" prop="rememberMeTimeoutSeconds">
        <el-input-number
          v-model="model.rememberMeTimeoutSeconds"
          :min="3600"
          :max="31536000"
          :step="3600"
        />
      </el-form-item>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { LoginConfig } from '@/api/system/config'

const model = defineModel<LoginConfig>({ required: true })
</script>
