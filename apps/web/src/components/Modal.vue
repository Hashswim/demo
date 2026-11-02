<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { X } from 'lucide-vue-next'
defineProps<{ title: string }>()
const emit = defineEmits<{ close: [] }>(),
  panel = ref<HTMLElement>(),
  previous = document.activeElement as HTMLElement | null
const focusable = () =>
  Array.from(
    panel.value?.querySelectorAll<HTMLElement>(
      'button:not(:disabled),input,select,textarea,a[href]',
    ) || [],
  )
function keyboard(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    emit('close')
    return
  }
  if (e.key !== 'Tab') return
  const nodes = focusable(),
    first = nodes[0],
    last = nodes[nodes.length - 1]
  if (e.shiftKey && document.activeElement === first) {
    e.preventDefault()
    last?.focus()
  } else if (!e.shiftKey && document.activeElement === last) {
    e.preventDefault()
    first?.focus()
  }
}
onMounted(async () => {
  await nextTick()
  ;(panel.value?.querySelector<HTMLElement>('input,textarea,select') || focusable()[0])?.focus()
  document.addEventListener('keydown', keyboard)
})
onUnmounted(() => {
  document.removeEventListener('keydown', keyboard)
  previous?.focus()
})
</script>
<template>
  <Teleport to="body"
    ><div class="modal-backdrop" @click.self="$emit('close')">
      <section ref="panel" class="modal" role="dialog" aria-modal="true" :aria-label="title">
        <header>
          <h2>{{ title }}</h2>
          <button class="icon-button" aria-label="닫기" @click="$emit('close')">
            <X :size="20" />
          </button>
        </header>
        <div class="modal-body"><slot /></div>
        <footer v-if="$slots.footer" class="modal-footer"><slot name="footer" /></footer>
      </section></div
  ></Teleport>
</template>
