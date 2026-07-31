<template>
  <section class="form-section">
    <div class="section-title">
      <h3>注册流程</h3>
      <p>控制匿名注册入口、邮箱验证与新账号审核。</p>
    </div>
    <div class="switch-list">
      <div class="switch-row">
        <div><strong>开放用户注册</strong><span>关闭后隐藏注册入口并拒绝注册请求。</span></div>
        <el-switch v-model="model.enabled" />
      </div>
      <div class="switch-row">
        <div><strong>验证邮箱</strong><span>关闭后注册页不再要求邮箱验证码。</span></div>
        <el-switch v-model="model.verifyEmail" />
      </div>
      <div class="switch-row">
        <div><strong>注册后审核</strong><span>开启后新用户状态为“待审核”，需管理员审核通过。</span></div>
        <el-switch v-model="model.needAudit" />
      </div>
    </div>
    <el-form-item label="默认角色编码" prop="defaultRoleCode" class="compact-field">
      <el-input
        v-model.trim="model.defaultRoleCode"
        maxlength="50"
        placeholder="例如 user"
      />
      <div class="field-tip">保存时会校验角色是否存在。</div>
    </el-form-item>
  </section>
</template>

<script setup lang="ts">
import type { RegisterConfig } from '@/api/system/config'

const model = defineModel<RegisterConfig>({ required: true })
</script>
