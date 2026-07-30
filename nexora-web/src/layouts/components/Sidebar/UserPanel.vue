<template>
  <div class="sidebar-user" :class="{ 'is-collapsed': isCollapse }">
    <el-dropdown trigger="click" placement="right-end" :teleported="true" @command="handleCommand">
      <button class="user-trigger" type="button" aria-label="打开用户菜单">
        <el-avatar :size="36" :src="userStore.user.avatar || ''">{{ avatarText }}</el-avatar>
        <span v-if="!isCollapse" class="user-copy">
          <strong>{{ displayName }}</strong>
          <small>{{ roleLabel }}</small>
        </span>
        <el-icon v-if="!isCollapse" class="user-arrow"><ArrowRight /></el-icon>
      </button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="profile"><el-icon><User /></el-icon>个人中心</el-dropdown-item>
          <el-dropdown-item command="lock"><el-icon><Lock /></el-icon>锁定屏幕</el-dropdown-item>
          <el-dropdown-item command="repository" divided><el-icon><Document /></el-icon>仓库地址</el-dropdown-item>
          <el-dropdown-item command="logout"><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ArrowRight, Document, Lock, SwitchButton, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/modules/user'
import settings from '@/config/settings'

defineProps<{ isCollapse: boolean }>()
const emit = defineEmits<{ lock: [] }>()
const router = useRouter()
const userStore = useUserStore()

const displayName = computed(() => userStore.user.nickname || userStore.user.email || '管理员')
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase())
const roleLabel = computed(() => {
  const role = userStore.user.roles?.[0]
  if (!role) return '管理后台'
  if (/^(admin|super-admin|root)$/i.test(role)) return '超级管理员'
  if (/admin/i.test(role)) return '管理员'
  return role
})

const handleCommand = async (command: string) => {
  if (command === 'profile') {
    await router.push('/system/profile')
    return
  }
  if (command === 'lock') return emit('lock')
  if (command === 'repository') {
    window.open(settings.repository, '_blank', 'noopener,noreferrer')
    return
  }
  if (command === 'logout') {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    await userStore.logout()
    await router.push('/login')
  }
}
</script>

<style lang="scss" scoped>
.sidebar-user {
  flex-shrink: 0;
  padding: 10px 10px 12px;
  border-top: 1px solid var(--nexora-sidebar-border);
  background: var(--nexora-sidebar-bg);
  :deep(.el-dropdown) { display: block; width: 100%; }
}

.user-trigger {
  width: 100%;
  min-height: 50px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 8px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: var(--nexora-sidebar-title);
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.18s ease;
  &:hover, &:focus-visible { background: var(--nexora-sidebar-hover); }
  &:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 2px; }
  :deep(.el-avatar) {
    flex-shrink: 0;
    background: var(--el-color-primary);
    color: #fff;
    font-weight: 600;
  }
}

.user-copy {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  strong, small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  strong { font-size: 14px; font-weight: 600; line-height: 20px; }
  small { color: var(--el-text-color-placeholder); font-size: 12px; line-height: 16px; }
}

.user-arrow { flex-shrink: 0; color: var(--el-text-color-placeholder); font-size: 14px; }
.sidebar-user.is-collapsed {
  padding-inline: 8px;
  .user-trigger { justify-content: center; padding-inline: 6px; }
}

@media (prefers-reduced-motion: reduce) { .user-trigger { transition: none; } }
</style>
