<template>
  <div :class="{ 'dark': settingsStore.theme === 'dark' }">
    <router-view ></router-view>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useSettingsStore } from '@/store/modules/settings'
import { usePublicConfigStore } from '@/store/modules/publicConfig'

const settingsStore = useSettingsStore()
const publicConfigStore = usePublicConfigStore()

onMounted(() => {
  // 初始化主题设置
  settingsStore.initTheme()
  void publicConfigStore.load()
})
</script>

<style>
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
}

/* 默认尺寸 */
:root {
  --el-header-height: 56px;
  --el-aside-width: 224px;
  --el-menu-item-height: 50px;
  --el-font-size-base: 14px;
  --el-color-primary: #2563EB;
  --el-bg-color-page: #f8fafc;
  --nexora-content-bg: #f8fafc;
  --el-border-color-lighter: #e8eaf0;
  --el-border-radius-base: 8px;
  --el-border-radius-small: 6px;
  --nexora-sidebar-bg: #ffffff;
  --nexora-sidebar-border: #e8eaf0;
  --nexora-sidebar-text: #64748b;
  --nexora-sidebar-title: #1e293b;
  --nexora-sidebar-hover: #f1f5f9;
  --nexora-sidebar-submenu: transparent;
}

/* 深色模式样式 */
:root[data-theme='dark'], :root.dark {
  color-scheme: dark;
  --el-bg-color: #151b28;
  --el-bg-color-overlay: #1d2635;
  --el-text-color-primary: #edf2f7;
  --el-text-color-regular: #c4cedd;
  --el-border-color-light: #2d384a;
  --el-bg-color-page: #0f1520;
  --nexora-content-bg: #0f1520;
  --el-bg-color-container: #151b28;
  --el-fill-color-blank: #1d2635;
  --el-mask-color: rgba(0, 0, 0, 0.8);
  --el-border-color: #2d384a;
  --el-border-color-lighter: #263246;
  --el-text-color-secondary: #94a3b8;
  --el-text-color-placeholder: #718096;
  --el-text-color-disabled: #64748b;
  --el-disabled-bg-color: #263246;
  --el-disabled-text-color: #64748b;
  --el-box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.3);
  --el-box-shadow-light: 0 1px 6px 0 rgba(0, 0, 0, 0.2);
  --nexora-sidebar-bg: #151b28;
  --nexora-sidebar-border: rgba(148, 163, 184, 0.13);
  --nexora-sidebar-text: #a9b5c7;
  --nexora-sidebar-title: #f8fafc;
  --nexora-sidebar-hover: rgba(148, 163, 184, 0.10);
  --nexora-sidebar-submenu: transparent;
}

/* 深色模式下的组件样式 */
:root[data-theme='dark'] {
  .el-container,
  .el-main {
    background-color: var(--el-bg-color-page);
  }

  .el-header,
  .el-aside,
  .el-menu,
  .el-card,
  .el-dialog,
  .el-drawer {
    background-color: var(--el-bg-color-container);
    border-color: var(--el-border-color);
  }

  .el-table {
    --el-table-bg-color: var(--el-bg-color-container);
    --el-table-tr-bg-color: var(--el-bg-color-container);
    --el-table-border-color: var(--el-border-color);
    --el-table-header-bg-color: var(--el-bg-color-container);
    --el-table-row-hover-bg-color: var(--el-fill-color-light);

    th, td {
      background-color: var(--el-bg-color-container);
      border-bottom-color: var(--el-border-color);
    }
  }

  .el-input__inner,
  .el-textarea__inner {
    background-color: var(--el-bg-color-container);
    border-color: var(--el-border-color);
    color: var(--el-text-color-primary);
  }

  .el-dropdown-menu {
    background-color: var(--el-bg-color-container);
    border-color: var(--el-border-color);
  }
}

/* 字体大小设置 */
[data-size="small"] {
  --el-font-size-base: 12px !important;
  --el-font-size-large: 14px !important;
  --el-font-size-small: 10px !important;
}

[data-size="default"] {
  --el-font-size-base: 14px !important;
  --el-font-size-large: 16px !important;
  --el-font-size-small: 12px !important;
}

[data-size="large"] {
  --el-font-size-base: 16px !important;
  --el-font-size-large: 18px !important;
  --el-font-size-small: 14px !important;
}

/* 确保所有组件使用正确的字体大小
   合并为单一规则，避免重复声明 */
.el-button,
.el-input,
.el-select,
.el-menu-item,
.el-sub-menu__title,
.el-dropdown-menu__item,
.el-form-item__label,
.el-form-item__content,
.el-table,
.el-dialog__title,
.el-card__header,
.el-tabs__item,
.el-breadcrumb__item,
.el-radio__label,
.el-checkbox__label,
.el-tag,
.el-pagination,
.el-alert__title,
.el-notification__title,
.el-message-box__title,
.el-drawer__title,
.el-tooltip__trigger,
.el-popover__title {
  font-size: var(--el-font-size-base) !important;
}

/* 字体大小过渡仅作用于 body，避免全局通配符影响性能 */
body {
  font-size: var(--el-font-size-base);
  transition: font-size 0.3s ease;
}

/* 标题和特殊文本 */
h1 { font-size: calc(var(--el-font-size-base) * 2); }
h2 { font-size: calc(var(--el-font-size-base) * 1.75); }
h3 { font-size: calc(var(--el-font-size-base) * 1.5); }
h4 { font-size: calc(var(--el-font-size-base) * 1.25); }
h5 { font-size: calc(var(--el-font-size-base) * 1.1); }
h6 { font-size: var(--el-font-size-base); }

/* 表格内容 */
.el-table th,
.el-table td {
  font-size: var(--el-font-size-base);
}

/* 表单项 */
.el-form {
  .el-form-item__label,
  .el-form-item__content,
  .el-input__inner,
  .el-select__input,
  .el-textarea__inner {
    font-size: var(--el-font-size-base);
  }
}

/* 菜单项 */
.el-menu {
  .el-menu-item,
  .el-sub-menu__title {
    font-size: var(--el-font-size-base);
  }
}
</style>
