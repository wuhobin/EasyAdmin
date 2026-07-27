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
import logoUrl from '@/assets/brand/nexora-logo.svg'

defineProps({
  size: {
    type: [Number, String],
    default: 32
  }
})
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
