<template>
  <img
    class="logo-svg"
    :src="logoUrl"
    alt=""
    :width="size"
    :height="size"
    aria-hidden="true"
  />
</template>

<script setup lang="ts">
import defaultLogoUrl from '@/assets/brand/nexora-logo.svg'
import { usePublicConfigStore } from '@/store/modules/publicConfig'

defineProps({
  size: {
    type: [Number, String],
    default: 32
  }
})

const publicConfigStore = usePublicConfigStore()
const logoUrl = computed(() => publicConfigStore.system.siteLogo || defaultLogoUrl)
</script>

<style scoped>
.logo-svg {
  display: block;
  object-fit: contain;
  transition: transform 0.3s ease;
}

.logo-svg:hover {
  transform: scale(1.1);
}
</style> 
