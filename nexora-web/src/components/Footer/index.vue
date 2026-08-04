<template>
  <footer class="footer">
    <div class="footer-content">
      <div class="copyright">
        {{ systemConfig.copyright }}
        <span v-if="systemConfig.icp" class="icp">{{ systemConfig.icp }}</span>
      </div>
      <div class="powered-by">Powered by Vue 3 & Element Plus</div>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { usePublicConfigStore } from '@/store/modules/publicConfig'

const publicConfigStore = usePublicConfigStore()
const systemConfig = computed(() => publicConfigStore.system)
</script>

<style lang="scss" scoped>
.footer {
  padding: 12px 24px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  background: var(--el-bg-color);
  border-top: 1px solid var(--el-border-color-light);
}

.footer-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1400px;
  margin: 0 auto;
}

.copyright {
  color: var(--el-text-color-regular);
}

.icp {
  margin-left: 12px;
  color: var(--el-text-color-secondary);
}

@media (max-width: 768px) {
  .footer-content {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }
}
</style>
