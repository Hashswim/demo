<script setup lang="ts">
import { useRoute } from 'vue-router'
import { ChevronRight, House } from 'lucide-vue-next'
defineProps<{ title?: string; subtitle?: string }>()
const route = useRoute()
</script>
<template>
  <div class="page-header">
    <div class="page-header-text">
      <nav class="breadcrumb" aria-label="현재 위치">
        <RouterLink to="/" aria-label="대시보드"><House :size="14" /></RouterLink>
        <template v-for="c in route.meta.crumbs" :key="c.label"
          ><ChevronRight :size="13" /><RouterLink v-if="c.to" :to="c.to">{{ c.label }}</RouterLink
          ><span v-else>{{ c.label }}</span></template
        >
        <template v-if="route.path !== '/'"
          ><ChevronRight :size="13" /><span aria-current="page">{{
            title || route.meta.title
          }}</span></template
        >
      </nav>
      <h1>{{ title || route.meta.title }}</h1>
      <p v-if="subtitle">{{ subtitle }}</p>
    </div>
    <div class="page-header-actions"><slot /></div>
  </div>
</template>
