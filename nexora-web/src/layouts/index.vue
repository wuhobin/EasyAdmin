<template>
  <a class="skip-link" href="#main-content">跳到主内容</a>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '224px'" class="transition-width" :class="{ 'mobile-collapsed': isCollapse }">
      <Sidebar :is-collapse="isCollapse" @select="closeMobileSidebar" @lock="handleLock" />
    </el-aside>
    <el-container>
      <el-header class="header">
        <Navbar 
          :is-collapse="isCollapse"
          @toggle-collapse="toggleCollapse"
          @theme-click="drawerVisible = true"
        />
      </el-header>
      <!-- 标签页 -->
      <tags-view v-if="settingsStore.showTags" />

      <el-main id="main-content" class="main-container" tabindex="-1">
 
        <router-view v-slot="{ Component }">
          <transition 
            :name="settingsStore.pageAnimation" 
            mode="out-in"
            appear
          >
            <keep-alive :include="cachedViews">
              <component 
                :is="Component" 
                :key="$route.fullPath" 
              />
            </keep-alive>
          </transition>

        </router-view>

      </el-main>

      <!-- 添加页脚 -->
      <Footer v-if="settingsStore.showFooter" />
    </el-container>
  </el-container>

  <!-- 设置抽屉 -->
  <setting-drawer
    v-model:visible="drawerVisible"
    v-model:isCollapse="isCollapse"
  />

  <!-- 添加锁屏组件 -->
  <lock-screen ref="lockScreenRef" />

  <!-- 水印组件 -->
  <Watermark />
</template>

<script setup lang="ts">
import TagsView from '@/components/TagsView/index.vue'
import SettingDrawer from '@/components/SettingDrawer/index.vue'
import Navbar from '@/layouts/components/Navbar/index.vue'
import Sidebar from './components/Sidebar/index.vue'
import LockScreen from '@/components/LockScreen/index.vue'
import Watermark from '@/components/Watermark/index.vue'
import Footer from '@/components/Footer/index.vue'

import { useSettingsStore, usePermissionStore } from "@/store";
import { useTagsViewStore } from '@/store/modules/tagsView'

const route = useRoute()
const tagsViewStore = useTagsViewStore()

const isCollapse = ref<boolean>(false)

const activeMenu = computed(() => route.path)

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const drawerVisible = ref(false)

const settingsStore = useSettingsStore()

// 缓存的视图
const cachedViews = computed(() => tagsViewStore.cachedViews)

// 初始化固定标签
onMounted(() => {
  tagsViewStore.initTags()
  if (window.innerWidth <= 768) isCollapse.value = true
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

const handleResize = () => {
  if (window.innerWidth <= 768) {
    isCollapse.value = true
  }
}

const lockScreenRef = ref()

const handleLock = () => {
  lockScreenRef.value?.lock()
}

const closeMobileSidebar = () => {
  if (window.innerWidth <= 768) isCollapse.value = true
}
</script>

<style scoped>
.skip-link {
  position: fixed;
  top: 8px;
  left: 8px;
  z-index: 3000;
  padding: 8px 12px;
  color: #fff;
  background: var(--el-color-primary);
  border-radius: 6px;
  transform: translateY(-150%);
  transition: transform 0.2s ease;
}

.skip-link:focus-visible { transform: translateY(0); }

.layout-container {
  height: 100vh;
  overflow: hidden;
  background-color: var(--el-bg-color-page);
  color: var(--el-text-color-primary);
}

.transition-width {
  transition: width 0.2s ease;
}

.el-aside {
  background-color: var(--nexora-sidebar-bg);
  border-right: 1px solid var(--nexora-sidebar-border);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.header {
  background-color: var(--el-bg-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.main-container {
  padding: 24px;
  overflow-y: auto;
  background-color: var(--nexora-content-bg);
}

@media (max-width: 768px) {
  .main-container { padding: 16px; }
  .el-aside {
    position: fixed;
    z-index: 1001;
    width: 224px !important;
    height: 100%;
    box-shadow: 12px 0 28px rgba(15, 23, 42, 0.12);
    transition: transform 0.2s ease;
  }
  .el-aside.mobile-collapsed { transform: translateX(-100%); }
}

@media (prefers-reduced-motion: reduce) {
  .skip-link,
  .transition-width,
  .el-aside { transition: none; }
}
</style> 
