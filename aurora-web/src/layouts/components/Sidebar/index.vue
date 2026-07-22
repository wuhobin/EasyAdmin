<template>
  <div class="sidebar-container">
    <div v-if="settingsStore.showLogo" class="logo-container">
      <Logo :size="32" class="logo-icon" :color="settingsStore.themeColor" />
      <span v-show="!isCollapse" class="logo-text">{{ settings.title }}</span>
    </div>

    <el-scrollbar class="menu-scrollbar">
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        background-color="transparent"
        text-color="var(--aurora-sidebar-text)"
        :active-text-color="settingsStore.themeColor"
        :collapse-transition="false"
        :unique-opened="true"
        @select="handleSelect"
      >
        <section v-for="group in menuGroups" :key="group.key" class="menu-group">
          <template v-if="group.title && !isCollapse">
            <button
              v-if="group.collapsible"
              type="button"
              class="menu-group__title menu-group__toggle"
              :aria-expanded="!isGroupCollapsed(group.key)"
              @click="toggleGroup(group.key)"
            >
              <span>{{ group.title }}</span>
              <el-icon :class="{ 'is-collapsed': isGroupCollapsed(group.key) }"><ArrowDown /></el-icon>
            </button>
            <div v-else class="menu-group__title">{{ group.title }}</div>
          </template>

          <el-collapse-transition>
            <div v-show="isCollapse || !isGroupCollapsed(group.key)" class="menu-group__content">
              <template v-for="menuRoute in group.routes" :key="menuRoute.path">
                <menu-item
                  v-if="!menuRoute.meta?.hidden"
                  :route="menuRoute"
                  :base-path="group.basePath"
                />
              </template>
            </div>
          </el-collapse-transition>
        </section>
      </el-menu>
    </el-scrollbar>

    <UserPanel :is-collapse="isCollapse" @lock="emit('lock')" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'
import { usePermissionStore } from '@/store/modules/permission'
import { useSettingsStore } from '@/store/modules/settings'
import Logo from './Logo.vue'
import MenuItem from './MenuItem.vue'
import UserPanel from './UserPanel.vue'
import settings from '@/config/settings'
import { isExternal } from '@/utils/validate'

defineProps({
  isCollapse: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits<{ select: []; lock: [] }>()
const route = useRoute()
const router = useRouter()
const permissionStore = usePermissionStore()
const settingsStore = useSettingsStore()
const COLLAPSED_GROUPS_KEY = 'aurora-sidebar-collapsed-groups'
const collapsedGroups = ref(new Set<string>())

try {
  const savedGroups = JSON.parse(sessionStorage.getItem(COLLAPSED_GROUPS_KEY) || '[]')
  if (Array.isArray(savedGroups)) collapsedGroups.value = new Set(savedGroups)
} catch {
  sessionStorage.removeItem(COLLAPSED_GROUPS_KEY)
}

const menuRoutes = computed(() => permissionStore.routes.map(menuRoute => {
  if (menuRoute.path === '/' && menuRoute.children) {
    const dashboardRoute = menuRoute.children.find(child => child.path === 'dashboard')
    if (dashboardRoute) {
      return { ...dashboardRoute, path: '/dashboard', children: undefined }
    }
  }
  if (menuRoute.meta?.singleMenu && menuRoute.children?.length === 1) {
    const pageRoute = menuRoute.children[0]
    return {
      ...pageRoute,
      path: menuRoute.path,
      children: undefined,
      meta: {
        ...pageRoute.meta,
        activeMenu: menuRoute.path
      }
    }
  }
  return menuRoute
}))

const menuGroups = computed(() => menuRoutes.value
  .filter(menuRoute => !menuRoute.meta?.hidden)
  .map(menuRoute => {
    if (menuRoute.path === '/dashboard') {
      return {
        key: 'overview',
        title: '总览',
        basePath: '/',
        routes: [menuRoute],
        collapsible: false
      }
    }
    if (menuRoute.children?.length) {
      return {
        key: menuRoute.path,
        title: String(menuRoute.meta?.title || ''),
        basePath: menuRoute.path,
        routes: menuRoute.children,
        collapsible: true
      }
    }
    return {
      key: menuRoute.path,
      title: '',
      basePath: '/',
      routes: [menuRoute],
      collapsible: false
    }
  }))

const activeMenu = computed(() => typeof route.meta?.activeMenu === 'string'
  ? route.meta.activeMenu
  : route.path)

const isGroupCollapsed = (key: string) => collapsedGroups.value.has(key)

const toggleGroup = (key: string) => {
  const nextGroups = new Set(collapsedGroups.value)
  if (nextGroups.has(key)) nextGroups.delete(key)
  else nextGroups.add(key)
  collapsedGroups.value = nextGroups
  sessionStorage.setItem(COLLAPSED_GROUPS_KEY, JSON.stringify([...nextGroups]))
}

const handleSelect = (index: string) => {
  if (isExternal(index)) {
    window.open(index, '_blank', 'noopener,noreferrer')
    return
  }
  if (route.path !== index) router.push(index)
  emit('select')
}
</script>

<style lang="scss" scoped>
.sidebar-container {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background-color: var(--aurora-sidebar-bg);

  .logo-container {
    height: 60px;
    display: flex;
    align-items: center;
    flex-shrink: 0;
    padding: 0 18px;

    .logo-icon { flex-shrink: 0; }
    .logo-text {
      margin-left: 10px;
      color: var(--aurora-sidebar-title);
      font-size: 16px;
      font-weight: 600;
      white-space: nowrap;
    }
  }

  .menu-scrollbar { flex: 1; min-height: 0; }

  :deep(.el-menu) {
    border-right: none;
    padding: 12px 8px 18px;

    .menu-group + .menu-group { margin-top: 15px; }
    .menu-group__title {
      height: 24px;
      width: 100%;
      box-sizing: border-box;
      padding: 0 10px;
      border: 0;
      background: transparent;
      color: var(--el-text-color-placeholder);
      font-family: inherit;
      font-size: 12px;
      font-weight: 600;
      line-height: 24px;
      letter-spacing: 0.02em;
      text-align: left;
      white-space: nowrap;
    }

    .menu-group__toggle {
      display: flex;
      align-items: center;
      justify-content: space-between;
      border-radius: 6px;
      cursor: pointer;
      transition: color 0.18s ease, background-color 0.18s ease;

      &:hover {
        background: var(--aurora-sidebar-hover);
        color: var(--aurora-sidebar-text);
      }

      &:focus-visible {
        outline: 2px solid v-bind('settingsStore.themeColor');
        outline-offset: 1px;
      }

      .el-icon {
        margin: 0;
        font-size: 12px;
        transition: transform 0.18s ease;

        &.is-collapsed { transform: rotate(-90deg); }
      }
    }

    .el-menu-item,
    .el-sub-menu__title {
      width: 100%;
      height: 42px;
      margin: 2px 0;
      border-radius: 8px;
      color: var(--aurora-sidebar-text);
      line-height: 42px;
      transition: color 0.18s ease, background-color 0.18s ease;

      .el-icon {
        width: 24px;
        margin-right: 9px;
        font-size: 18px;
        text-align: center;
      }
    }

    .el-sub-menu__title > .el-sub-menu__icon-arrow {
      width: 12px;
      height: 12px;
      margin-right: 0;
      font-size: 12px;
    }

    .el-menu-item.is-active {
      background-color: v-bind('`${settingsStore.themeColor}14`');
      color: v-bind('settingsStore.themeColor');
      font-weight: 600;
    }

    .el-menu-item:hover,
    .el-sub-menu__title:hover { background-color: var(--aurora-sidebar-hover); }
  }
}

:deep(.el-menu--collapse) {
  width: 64px;
  padding: 12px 8px 18px;
  .menu-group + .menu-group { margin-top: 8px; }
  .el-menu-item,
  .el-sub-menu__title {
    padding: 0 12px !important;
    .el-icon { width: 24px !important; margin: 0 !important; text-align: center; }
  }
  .el-sub-menu__title span,
  .el-menu-item span,
  .el-sub-menu__title .el-sub-menu__icon-arrow { display: none; }
}

:deep(.el-scrollbar__view) { min-height: 100%; }
:deep(.el-menu .el-menu) {
  padding: 4px 0;
  background-color: transparent;
  .el-menu-item {
    height: 40px;
    margin: 2px 0;
    line-height: 40px;
    &.is-active { background-color: v-bind('`${settingsStore.themeColor}14`'); }
  }
}

@media (prefers-reduced-motion: reduce) {
  :deep(.el-menu-item),
  :deep(.el-sub-menu__title),
  :deep(.menu-group__toggle),
  :deep(.menu-group__toggle .el-icon) { transition: none; }
}
</style>
