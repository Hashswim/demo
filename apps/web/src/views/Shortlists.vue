<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Bookmark, Trash2, ArrowRight, Plus, Search } from 'lucide-vue-next'
import { api } from '../lib/api'
import type { Shortlist, StaffingRequest } from '../lib/types'
import { toast, formatDate } from '../lib/ui'
import PageHeader from '../components/PageHeader.vue'
import StatusBadge from '../components/StatusBadge.vue'
import Modal from '../components/Modal.vue'

const items = ref<Shortlist[]>([]),
  requests = ref<StaffingRequest[]>([]),
  filter = ref('ALL'),
  query = ref(''),
  loading = ref(true),
  error = ref(''),
  confirming = ref<Shortlist | null>(null),
  removing = ref(false)
onMounted(async () => {
  try {
    ;[items.value, requests.value] = await Promise.all([api.shortlists(), api.requests()])
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
})
const title = (id: string) => requests.value.find((r) => r.id === id)?.title || id
const filtered = computed(() =>
  items.value.filter(
    (i) =>
      (filter.value === 'ALL' || i.requestId === filter.value) &&
      `${i.employee.name} ${i.reason} ${title(i.requestId)}`
        .toLowerCase()
        .includes(query.value.toLowerCase()),
  ),
)
const projects = computed(() =>
  requests.value.filter((r) => items.value.some((i) => i.requestId === r.id)),
)
async function remove() {
  if (!confirming.value) return
  removing.value = true
  try {
    await api.removeShortlist(confirming.value.id)
    items.value = items.value.filter((s) => s.id !== confirming.value!.id)
    toast(`${confirming.value.employee.name} 님을 후보에서 삭제했습니다.`)
    confirming.value = null
  } catch (e) {
    toast((e as Error).message, { tone: 'error' })
  } finally {
    removing.value = false
  }
}
</script>

<template>
  <PageHeader
    subtitle="프로젝트별로 저장한 후보와 선택 이유를 검토합니다. 후보 저장은 배정 확정이 아닙니다."
  >
    <RouterLink class="btn btn-primary" to="/requests/new"
      ><Plus :size="17" />새 추천 요청</RouterLink
    >
  </PageHeader>
  <div v-if="error" class="alert danger" role="alert">{{ error }}</div>
  <section class="card">
    <div class="toolbar">
      <label class="search-input"
        ><Search :size="16" /><input
          v-model="query"
          placeholder="이름, 선택 이유 검색"
          aria-label="후보 검색"
      /></label>
      <label class="select-inline"
        >프로젝트
        <select v-model="filter" aria-label="프로젝트별 후보 필터">
          <option value="ALL">전체 ({{ items.length }})</option>
          <option v-for="r in projects" :key="r.id" :value="r.id">
            {{ r.title }} ({{ items.filter((i) => i.requestId === r.id).length }})
          </option>
        </select></label
      >
    </div>
    <div v-if="!loading && !items.length" class="empty-state">
      <Bookmark :size="32" />
      <strong>아직 저장한 후보가 없습니다</strong>
      <p>추천 결과에서 인력을 선택하고 이유와 함께 저장해 보세요.</p>
      <RouterLink to="/requests" class="btn btn-outline-primary">요청 목록으로</RouterLink>
    </div>
    <div v-else class="table-responsive">
      <table class="table">
        <thead>
          <tr>
            <th>후보</th>
            <th>프로젝트</th>
            <th style="min-width: 260px">선택 이유</th>
            <th>현재 상태</th>
            <th>저장일</th>
            <th class="actions">작업</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="s in filtered" :key="s.id">
            <td>
              <span class="cell-person"
                ><span class="avatar sm">{{ s.employee.name.slice(-2) }}</span
                ><span
                  ><strong>{{ s.employee.name }}</strong
                  ><small>{{ s.employee.title }} · {{ s.employee.department }}</small></span
                ></span
              >
            </td>
            <td>
              <RouterLink :to="'/requests/' + s.requestId" class="link">{{
                title(s.requestId)
              }}</RouterLink>
            </td>
            <td class="reason-cell">{{ s.reason }}</td>
            <td><StatusBadge :status="s.employee.availability" /></td>
            <td>{{ formatDate(s.createdAt) }}</td>
            <td class="actions">
              <RouterLink :to="'/requests/' + s.requestId" class="btn btn-text btn-sm"
                >근거 보기<ArrowRight :size="14"
              /></RouterLink>
              <button
                class="icon-btn danger"
                :aria-label="s.employee.name + ' 후보 삭제'"
                @click="confirming = s"
              >
                <Trash2 :size="16" />
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!filtered.length && items.length" class="empty-state compact">
        <strong>조건에 맞는 후보가 없습니다</strong>
      </div>
    </div>
    <div class="table-footer">총 {{ filtered.length }}명</div>
  </section>

  <Modal v-if="confirming" title="후보를 삭제할까요?" @close="confirming = null">
    <p>
      <strong>{{ confirming.employee.name }}</strong> 님을 「{{ title(confirming.requestId) }}」
      후보에서 삭제합니다. 저장한 선택 이유도 함께 삭제되며 되돌릴 수 없습니다.
    </p>
    <template #footer>
      <button class="btn btn-text" @click="confirming = null">취소</button>
      <button class="btn btn-danger" :disabled="removing" @click="remove">
        {{ removing ? '삭제 중…' : '삭제' }}
      </button>
    </template>
  </Modal>
</template>
