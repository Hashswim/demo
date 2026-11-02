import { ref } from 'vue'

// Material snackbar: 짧은 작업 결과를 화면 하단에 알리고 4초 뒤 사라진다.
export interface Snack {
  id: number
  text: string
  tone: 'info' | 'error'
  action?: { label: string; to: string }
}
export const snacks = ref<Snack[]>([])
let seq = 0
export function toast(text: string, options: Partial<Omit<Snack, 'id' | 'text'>> = {}) {
  const item: Snack = { id: ++seq, text, tone: 'info', ...options }
  snacks.value = [...snacks.value.slice(-2), item]
  setTimeout(() => dismiss(item.id), 4000)
}
export const dismiss = (id: number) => (snacks.value = snacks.value.filter((s) => s.id !== id))

export const availabilityLabel = {
  AVAILABLE: '투입 가능',
  ASSIGNED: '투입 중',
  UNKNOWN: '확인 필요',
} as const

export const formatDate = (value?: string | null) =>
  value ? value.slice(0, 10).replaceAll('-', '.') : '—'

export const daysUntil = (value: string) =>
  Math.ceil((new Date(value + 'T23:59:59').getTime() - Date.now()) / 86_400_000)

export function readPref<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem('find:pref:' + key)
    return raw === null ? fallback : (JSON.parse(raw) as T)
  } catch {
    return fallback
  }
}
export function writePref(key: string, value: unknown) {
  try {
    localStorage.setItem('find:pref:' + key, JSON.stringify(value))
  } catch {
    /* 저장소를 쓸 수 없는 환경에서는 기본값으로 동작 */
  }
}
