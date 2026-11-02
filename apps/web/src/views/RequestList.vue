<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Plus, Search, ClipboardList, ArrowRight } from 'lucide-vue-next'
import { api } from '../lib/api'
import type { StaffingRequest, Shortlist } from '../lib/types'
import { formatDate } from '../lib/ui'
import PageHeader from '../components/PageHeader.vue'

const items = ref<StaffingRequest[]>([]),
  shortlists = ref<Shortlist[]>([]),
  query = ref(''),
  domain = ref('ALL'),
  loading = ref(true),
  error = ref('')
onMounted(async () => {
  try {
    ;[items.value, shortlists.value] = await Promise.all([api.requests(), api.shortlists()])
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
})
const domains = computed(() => [...new Set(items.value.map((r) => r.domain))])
const filtered = computed(() =>
  items.value.filter(
    (r) =>
      (domain.value === 'ALL' || r.domain === domain.value) &&
      `${r.title} ${r.description} ${r.skills.join(' ')}`
        .toLowerCase()
        .includes(query.value.toLowerCase()),
  ),
)
const must = (r: StaffingRequest) => r.requirements.filter((x) => x.kind === 'MUST').length
const saved = (id: string) => shortlists.value.filter((s) => s.requestId === id).length
const period = (r: StaffingRequest) =>
  r.startDate || r.endDate ? `${formatDate(r.startDate)} ~ ${formatDate(r.endDate)}` : '미정'
</script>

<template>
  <PageHeader
    subtitle="등록한 추천 요청과 후보 저장 현황입니다. 행을 선택하면 추천 결과로 이동합니다."
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
          placeholder="프로젝트명, 내용, 기술 검색"
          aria-label="요청 검색"
      /></label>
      <div class="filter-chips" role="group" aria-label="사업 분야 필터">
        <button
          :class="['filter-chip', { selected: domain === 'ALL' }]"
          :aria-pressed="domain === 'ALL'"
          @click="domain = 'ALL'"
        >
          전체 <b>{{ items.length }}</b>
        </button>
        <button
          v-for="d in domains"
          :key="d"
          :class="['filter-chip', { selected: domain === d }]"
          :aria-pressed="domain === d"
          @click="domain = d"
        >
          {{ d }} <b>{{ items.filter((r) => r.domain === d).length }}</b>
        </button>
      </div>
    </div>
    <div v-if="!loading && !items.length" class="empty-state">
      <ClipboardList :size="32" />
      <strong>등록한 추천 요청이 없습니다</strong>
      <p>프로젝트 내용을 입력하면 필요한 요건을 정리하고 인력을 추천합니다.</p>
      <RouterLink to="/requests/new" class="btn btn-primary"
        ><Plus :size="16" />새 추천 요청</RouterLink
      >
    </div>
    <div v-else class="table-responsive">
      <table class="table">
        <thead>
          <tr>
            <th>프로젝트명</th>
            <th>사업 분야</th>
            <th>요건 (필수/우대)</th>
            <th>예상 기간</th>
            <th class="num">저장 후보</th>
            <th>등록일</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="r in filtered"
            :key="r.id"
            class="row-link"
            @click="$router.push('/requests/' + r.id)"
          >
            <td>
              <RouterLink :to="'/requests/' + r.id" class="cell-strong" @click.stop>{{
                r.title
              }}</RouterLink>
              <small class="cell-sub">{{ r.skills.join(', ') || '기술 미지정' }}</small>
            </td>
            <td>{{ r.domain }}</td>
            <td>
              <span class="badge badge-outline-primary">필수 {{ must(r) }}</span>
              <span class="badge badge-outline-muted"
                >우대 {{ r.requirements.length - must(r) }}</span
              >
            </td>
            <td>{{ period(r) }}</td>
            <td class="num">
              <span :class="saved(r.id) ? 'badge badge-success' : 'muted'">{{
                saved(r.id) ? saved(r.id) + '명' : '없음'
              }}</span>
            </td>
            <td>{{ formatDate(r.createdAt) }}</td>
            <td class="actions">
              <span class="btn btn-text btn-sm">결과 보기<ArrowRight :size="14" /></span>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!filtered.length && items.length" class="empty-state compact">
        <strong>검색 조건에 맞는 요청이 없습니다</strong>
        <button class="btn btn-text" @click="((query = ''), (domain = 'ALL'))">필터 초기화</button>
      </div>
    </div>
    <div class="table-footer">총 {{ filtered.length }}건</div>
  </section>
</template>
