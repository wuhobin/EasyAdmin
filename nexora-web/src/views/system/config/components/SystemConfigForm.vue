<template>
  <section class="form-section">
    <div class="section-content form-grid two-columns">
      <el-form-item label="站点名称" prop="siteName">
        <el-input v-model.trim="model.siteName" maxlength="100" />
      </el-form-item>
      <el-form-item label="后台短标题" prop="shortTitle">
        <el-input v-model.trim="model.shortTitle" maxlength="100" />
      </el-form-item>
      <el-form-item label="站点描述" prop="siteDescription" class="full-width">
        <el-input
          v-model.trim="model.siteDescription"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
      <el-form-item label="Logo 地址" prop="siteLogo" class="full-width">
        <div class="logo-field">
          <el-input
            v-model.trim="model.siteLogo"
            maxlength="1024"
            placeholder="留空时使用 Nexora 默认 Logo"
          />
          <div class="logo-preview">
            <img v-if="model.siteLogo" :src="model.siteLogo" alt="Logo 预览" />
            <span v-else>默认</span>
          </div>
        </div>
      </el-form-item>
    </div>
  </section>

  <section class="form-section">
    <div class="section-content form-grid two-columns">
      <el-form-item label="版权信息" prop="copyright">
        <el-input v-model.trim="model.copyright" maxlength="255" />
      </el-form-item>
      <el-form-item label="ICP备案号" prop="icp">
        <el-input v-model.trim="model.icp" maxlength="100" placeholder="可留空" />
      </el-form-item>
    </div>
  </section>

  <section class="form-section">
    <div class="section-content form-grid two-columns">
      <el-form-item label="强制开启水印" prop="watermarkEnabled">
        <el-switch v-model="model.watermarkEnabled" />
      </el-form-item>
      <el-form-item label="水印透明度" prop="watermarkOpacity">
        <el-slider
          v-model="model.watermarkOpacity"
          :min="0.01"
          :max="0.3"
          :step="0.01"
          style="width: 200px"
        />
        <span class="form-hint">{{ (model.watermarkOpacity * 100).toFixed(0) }}%</span>
      </el-form-item>
      <el-form-item label="水印内容" prop="watermarkType">
        <el-select v-model="model.watermarkType" style="width: 200px">
          <el-option label="用户名" value="username" />
          <el-option label="用户名 + 时间" value="username_time" />
          <el-option label="站点名称" value="sitename" />
          <el-option label="自定义文本" value="custom" />
        </el-select>
      </el-form-item>
      <el-form-item label="自定义水印文本" prop="watermarkCustomText">
        <el-input
          v-model.trim="model.watermarkCustomText"
          :disabled="model.watermarkType !== 'custom'"
          maxlength="100"
          placeholder="选择自定义文本时使用"
          style="width: 300px"
        />
      </el-form-item>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { SystemConfig } from '@/api/system/config'

const model = defineModel<SystemConfig>({ required: true })
</script>
