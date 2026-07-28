<template>
  <el-dialog
    v-model="dialogVisible"
    title="选择图标"
    width="800px"
    append-to-body
    top="5vh"
  >
    <p class="dialog-form-intro">从图标库中搜索并选择一个菜单图标。</p>
    <div class="icon-container">
      <div class="search-bar">
        <el-input
          v-model="searchText"
          name="icon-search"
          aria-label="搜索图标"
          autocomplete="off"
          placeholder="搜索图标，例如：User…"
          clearable
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <el-scrollbar height="400px">
        <div class="icon-list">
          <button
            v-for="(component, name) in filteredIcons"
            :key="name"
            class="icon-item"
            type="button"
            :aria-pressed="modelValue === name"
            :class="{ active: modelValue === name }"
            @click="selectIcon(name)"
          >
            <el-icon>
              <component :is="component" />
            </el-icon>
            <span class="icon-name">{{ name }}</span>
          </button>
        </div>
      </el-scrollbar>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { Search } from '@element-plus/icons-vue'

const props = defineProps<{
  modelValue: string
  visible: boolean
}>()

const emit = defineEmits(['update:modelValue', 'update:visible'])

const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const searchText = ref('')

// 直接使用图标组件对象
const icons = ElementPlusIconsVue

const filteredIcons = computed(() => {
  const iconEntries = Object.entries(icons)
  if (!searchText.value) return icons
  
  return Object.fromEntries(
    iconEntries.filter(([name]) => 
      name.toLowerCase().includes(searchText.value.toLowerCase())
    )
  )
})

const selectIcon = (iconName: string) => {
  emit('update:modelValue', iconName)
  emit('update:visible', false)
}
</script>

<style scoped>
.icon-container {
  padding: 0;
}

.search-bar {
  margin-bottom: 18px;
}

.icon-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
  padding: 4px 6px 10px;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 88px;
  padding: 14px 8px;
  border-radius: 6px;
  cursor: pointer;
  color: inherit;
  background: var(--nexora-control-muted-bg);
  font: inherit;
  transition: color 0.18s, background-color 0.18s, border-color 0.18s, transform 0.18s;
  border: 1px solid var(--el-border-color-lighter);
}

.icon-item:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 2px; }

.icon-item:hover {
  background-color: color-mix(in srgb, var(--el-color-primary) 8%, var(--nexora-overlay-surface));
  border-color: color-mix(in srgb, var(--el-color-primary) 45%, var(--el-border-color));
  transform: translateY(-1px);
}

.icon-item.active {
  background-color: color-mix(in srgb, var(--el-color-primary) 11%, var(--nexora-overlay-surface));
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
  box-shadow: 0 6px 16px -12px var(--el-color-primary);
}

.icon-item .el-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.icon-name {
  font-size: 12px;
  color: var(--el-text-color-regular);
  line-height: 17px;
  word-break: break-all;
  text-align: center;
}

.icon-item.active .icon-name {
  color: var(--el-color-primary);
  font-weight: 600;
}
</style>
