<template>
  <section class="form-section">
    <div class="section-title">
      <h3>登录保护</h3>
      <p>登录失败次数使用规范化邮箱计数；安全缓存异常时拒绝登录。</p>
    </div>
    <div class="switch-list">
      <div class="switch-row">
        <div><strong>登录滑块验证</strong><span>密码校验前先完成滑块，滑块失败不累计密码错误。</span></div>
        <el-switch v-model="model.captchaEnabled" />
      </div>
      <div class="switch-row">
        <div><strong>允许记住我</strong><span>关闭后即使前端提交记住我，也使用普通会话时长。</span></div>
        <el-switch v-model="model.rememberMeEnabled" />
      </div>
      <div class="switch-row">
        <div><strong>单点登录</strong><span>新会话建立前踢出该用户的其他会话。</span></div>
        <el-switch v-model="model.singleLogin" />
      </div>
    </div>
  </section>

  <section class="form-section">
    <div class="section-title">
      <h3>重试与会话</h3>
      <p>时间单位明确区分分钟和秒，避免生产环境误配。</p>
    </div>
    <div class="form-grid two-columns">
      <el-form-item label="最大密码重试次数" prop="maxRetryCount">
        <el-input-number v-model="model.maxRetryCount" :min="1" :max="20" />
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
