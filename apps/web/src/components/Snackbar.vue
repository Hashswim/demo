<script setup lang="ts">
import { X } from 'lucide-vue-next'
import { snacks, dismiss } from '../lib/ui'
</script>
<template>
  <div class="snackbar-host" role="status" aria-live="polite">
    <div v-for="s in snacks" :key="s.id" :class="['snackbar', s.tone]">
      <span>{{ s.text }}</span>
      <RouterLink
        v-if="s.action"
        :to="s.action.to"
        class="snackbar-action"
        @click="dismiss(s.id)"
        >{{ s.action.label }}</RouterLink
      >
      <button class="snackbar-close" aria-label="알림 닫기" @click="dismiss(s.id)">
        <X :size="16" />
      </button>
    </div>
  </div>
</template>
