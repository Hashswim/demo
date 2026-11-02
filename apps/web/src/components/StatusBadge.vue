<script setup lang="ts">
import { CircleCheck, CircleDot, CircleHelp } from 'lucide-vue-next'
import type { Availability } from '../lib/types'
import { availabilityLabel } from '../lib/ui'
defineProps<{ status: Availability }>()
// 상태는 색만으로 구분하지 않도록 아이콘과 라벨을 함께 표시한다.
const icons = { AVAILABLE: CircleCheck, ASSIGNED: CircleDot, UNKNOWN: CircleHelp }
</script>
<template>
  <span class="status" :class="status.toLowerCase()"
    ><component :is="icons[status]" :size="13" aria-hidden="true" />{{
      availabilityLabel[status]
    }}</span
  >
</template>
