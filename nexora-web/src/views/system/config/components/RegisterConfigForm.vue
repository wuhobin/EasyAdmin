<template>
  <section class="form-section">
    <div class="section-content form-grid two-columns">
      <el-form-item label="开放用户注册" prop="enabled">
        <el-switch v-model="model.enabled" />
        <span class="form-hint">关闭后隐藏注册入口并拒绝注册请求。</span>
      </el-form-item>
      <el-form-item label="验证邮箱" prop="verifyEmail">
        <el-switch v-model="model.verifyEmail" />
        <span class="form-hint">关闭后注册页不再要求邮箱验证码。</span>
      </el-form-item>
      <el-form-item label="默认角色编码" prop="defaultRoleCode">
        <el-input
          v-model.trim="model.defaultRoleCode"
          maxlength="50"
          placeholder="例如 user"
        />
      </el-form-item>
      <el-form-item label="注册后审核" prop="needAudit">
        <el-switch v-model="model.needAudit" />
        <span class="form-hint">开启后新用户需管理员审核通过。</span>
      </el-form-item>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { RegisterConfig } from '@/api/system/config'

const model = defineModel<RegisterConfig>({ required: true })
</script>
