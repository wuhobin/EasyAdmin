<template>
  <div class="navbar-container">
    <div class="navbar-left">
      <button class="icon-button collapse-btn" type="button" :aria-label="isCollapse ? '展开侧边栏' : '收起侧边栏'" @click="toggleCollapse">
        <el-icon><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
      </button>

      <Breadcrumb />
    </div>
    <div class="navbar-right">
      <!-- 菜单全局搜索 -->
      <global-search/>
      <!-- 主题切换 -->
      <button class="icon-button setting-icon" type="button" aria-label="打开外观设置" @click="handleThemeClick">
        <el-icon><Setting /></el-icon>
      </button>
      <!-- 全屏切换 -->
      <button class="icon-button setting-icon" type="button" :aria-label="isFullscreen ? '退出全屏' : '进入全屏'" @click="toggleFullscreen">
        <el-icon v-if="!isFullscreen"><FullScreen /></el-icon>
        <svg-icon v-else name="exitFullScreen" />
      </button>
      <!-- 通知中心 -->
      <notification />
      <!-- 用户信息 -->
    </div>

    <!-- 添加锁屏组件 -->
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import screenfull from 'screenfull'
import { useSettingsStore } from '@/store/modules/settings'
import GlobalSearch from '@/components/GlobalSearch/index.vue'
import Breadcrumb from './Breadcrumb/index.vue'
import Notification from './Notification/index.vue'
import { FullScreen, Setting } from '@element-plus/icons-vue'

const settingsStore = useSettingsStore()
const isFullscreen = ref(false)

defineProps({
  isCollapse: {
    type: Boolean,
    required: true
  }
})

const emit = defineEmits(['toggle-collapse', 'theme-click'])

const toggleCollapse = () => {
  emit('toggle-collapse')
}

const handleThemeClick = () => {
  emit('theme-click')
}

const toggleFullscreen = () => {
  if (screenfull.isEnabled) {
    screenfull.toggle()
    isFullscreen.value = !isFullscreen.value
  }
}
</script>

<style lang="scss" scoped>
.navbar-container {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0;
  
  .navbar-left {
    display: flex;
    align-items: center;

    
    .collapse-btn {
      font-size: 20px;
      cursor: pointer;
      margin-right: 16px;
      color: var(--el-text-color-secondary);
      transition: color 0.2s ease, background-color 0.2s ease;
      
      &:hover {
        color: v-bind('settingsStore.themeColor');
      }
    }
  }

  .icon-button {
    display: inline-grid;
    place-items: center;
    border: 0;
    background: transparent;
    cursor: pointer;

    &:focus-visible {
      outline: 2px solid v-bind('settingsStore.themeColor');
      outline-offset: 2px;
    }
  }
  
  .navbar-right {
    display: flex;
    align-items: center;
    gap: 4px;
    margin-left: auto;
    padding-right: 8px;
    height: 100%;
    
    .setting-icon {
      font-size: 20px;
      cursor: pointer;
      padding: 6px;
      border-radius: 50%;
      transition: color 0.2s ease, background-color 0.2s ease;
      color: var(--el-text-color-secondary);
      
      &:hover {
        background-color: v-bind('`${settingsStore.themeColor}1a`');
        color: v-bind('settingsStore.themeColor');
      }
    }
  }
}
</style>
