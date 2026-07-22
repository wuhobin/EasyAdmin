<template>
    <div class="sidebar-container">
      <div v-if="settingsStore.showLogo" class="logo-container" :class="{ 'dark': settingsStore.theme === 'dark' }">
        <Logo :size="32" class="logo-icon" :color="settingsStore.themeColor" />
        <span v-show="!isCollapse" class="logo-text">{{ settings.title }}</span>
      </div>
      <el-scrollbar>
        <el-menu style="height: 100%;"
          :default-active="activeMenu"
          :collapse="isCollapse"
          background-color="transparent"
          text-color="var(--aurora-sidebar-text)"
          :active-text-color="settingsStore.themeColor"
          :collapse-transition="false"
          @select="handleSelect"
          :unique-opened="true"
        >
          <template v-for="route in menuRoutes" :key="route.path">
            <menu-item v-if="!route.meta?.hidden" :route="route" :base-path="route.path" />
          </template>
        </el-menu>
      </el-scrollbar>
    </div>
  </template>
  
  <script setup lang="ts">
  import { computed } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { usePermissionStore } from '@/store/modules/permission'
  import { useSettingsStore } from '@/store/modules/settings'
  import Logo from '@/layouts/components/Sidebar/Logo.vue'
  import settings from '@/config/settings'
  import { isExternal } from '@/utils/validate'
  import MenuItem from './MenuItem.vue'
  
  const route = useRoute()
  const permissionStore = usePermissionStore()
  const settingsStore = useSettingsStore()
  const router = useRouter()
  // 从 props 接收折叠状态
  const emit = defineEmits<{ select: [] }>()

  defineProps({
    isCollapse: {
      type: Boolean,
      default: false
    }
  })
  
  // 获取路由菜单
  const menuRoutes = computed(() => {
    const routes = permissionStore.routes
    return routes.map(route => {
      // 如果是根路由且包含 dashboard 子路由
      if (route.path === '/' && route.children) {
        const dashboardRoute = route.children.find(child => child.path === 'dashboard')
        if (dashboardRoute) {
          // 将 dashboard 提升为一级路由
          return {
            ...dashboardRoute,
            path: '/dashboard',
            children: undefined
          }
        }
      }
      return route
    })
  })
  // 当前激活的菜单
  const activeMenu = computed(() => {
    const { meta, path } = route
    if (typeof meta?.activeMenu === 'string') {
      return meta.activeMenu
    }
    return path
  })
  
  // 修改路径处理函数
  const resolvePath = (routePath: string) => {
    // 如果是外部链接，直接返回原路径
    if (isExternal(routePath)) {
      return routePath
    }
    
    // 如果是根路径，直接返回
    if (routePath === '/') return routePath
    
    // 移除开头和结尾的斜杠
    routePath = routePath.replace(/^\/+|\/+$/g, '')
    
    // 如果是仪表盘路径，特殊处理
    if (routePath === 'dashboard') {
      return '/dashboard'
    }
    
    // 其他路径，确保只有一个斜杠
    return '/' + routePath
  }
  
  // 添加 select 事件处理函数
  const handleSelect = (index: string) => {
    if (isExternal(index)) {
      window.open(index, '_blank')
      return
    }
    
    // 内部路由跳转
    if (route.path !== index) {
      router.push(index)
    }
    emit('select')
  }
  </script>
  
  <style lang="scss" scoped>
  .sidebar-container {
    height: 100%;
    background-color: var(--aurora-sidebar-bg);
    
    .logo-container {
      height: 56px;
      display: flex;
      align-items: center;
      padding: 0 18px;
      background-color: var(--aurora-sidebar-bg);
      border-bottom: 1px solid var(--aurora-sidebar-border);
      
      .logo-icon {
        flex-shrink: 0;
      }
      
      .logo-text {
        color: var(--aurora-sidebar-title);
        font-size: 16px;
        margin-left: 10px;
        font-weight: 600;
        white-space: nowrap;
      }
    }

    :deep(.el-menu) {
      border-right: none;
      padding: 8px 0;

      // 一级菜单样式
      .el-menu-item, .el-sub-menu__title {
        height: 46px;
        line-height: 46px;
        margin: 0 10px;
        width: calc(100% - 20px);
        border-radius: 7px;
        color: var(--aurora-sidebar-text);
        transition: color 0.2s ease, background-color 0.2s ease;
        
        .el-icon {
          width: 24px;
          text-align: center;
          font-size: 18px;
          margin-right: 10px;
        }
      }

      // 激活状态
      .el-menu-item.is-active {
        background-color: v-bind('`${settingsStore.themeColor}18`');
        color: v-bind('settingsStore.themeColor');
      }

      // 悬停效果
      .el-menu-item:hover, .el-sub-menu__title:hover {
        background-color: var(--aurora-sidebar-hover);
      }
    }
  }

  // 折叠状态样式
  :deep(.el-menu--collapse) {
    width: 64px;

    .el-menu-item, .el-sub-menu__title {
      padding: 0 20px !important;
      
      .el-icon {
        margin: 0 !important;
        width: 24px !important;
        text-align: center;
      }
    }

    // 隐藏文字和箭头
    .el-sub-menu__title span,
    .el-menu-item span,
    .el-sub-menu__title .el-sub-menu__icon-arrow {
      display: none;
    }
  }

  :deep(.el-scrollbar__view) {
    height: 100% !important;
  }

  // 子菜单样式
  :deep(.el-menu .el-menu) {
    background-color: var(--aurora-sidebar-submenu);
    .el-menu-item {
      height: 42px;
      line-height: 42px;
      margin: 0 10px;
      
      &.is-active {
        background-color: v-bind('`${settingsStore.themeColor}18`');
      }
    }
  }

  :deep(.el-menu .el-menu) {
    padding: 4px 0;
  }
  </style>
