<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ChevronLeft, ChevronRight, Search, Info } from 'lucide-vue-next'
import { api, user } from '../lib/api'
import type { Employee, Availability } from '../lib/types'
import { availabilityLabel, formatDate } from '../lib/ui'
import PageHeader from '../components/PageHeader.vue'
import StatusBadge from '../components/StatusBadge.vue'

const people = ref<Employee[]>([]),
  date = ref(new Date()),
  query = ref(''),
  status = ref<'ALL' | Availability>('ALL'),
  error = ref(''),
  view = ref<'calendar' | 'table'>('calendar')
onMounted(async () => {
  try {
    people.value = await api.employees()
  } catch (e) {
    error.value = (e as Error).message
  }
})
const days = computed(() =>
  new Date(date.value.getFullYear(), date.value.getMonth() + 1, 0).getDate(),
)
const month = computed(() => `${date.value.getFullYear()}년 ${date.value.getMonth() + 1}월`)
const positions = computed(() => Array.from({ length: days.value }, (_, i) => i + 1))
const statuses: Availability[] = ['AVAILABLE', 'ASSIGNED', 'UNKNOWN']
const filtered = computed(() =>
  people.value.filter(
    (p) =>
      (status.value === 'ALL' || p.availability === status.value) &&
      `${p.name} ${p.department} ${p.assignment?.projectTitle || ''}`
        .toLowerCase()
        .includes(query.value.toLowerCase()),
  ),
)
const today = new Date()
const todayPos = computed(() =>
  today.getFullYear() === date.value.getFullYear() && today.getMonth() === date.value.getMonth()
    ? `${((today.getDate() - 0.5) / days.value) * 100}%`
    : null,
)
const weekend = (day: number) =>
  [0, 6].includes(new Date(date.value.getFullYear(), date.value.getMonth(), day).getDay())
function move(n: number) {
  date.value = new Date(date.value.getFullYear(), date.value.getMonth() + n, 1)
}
function range(e: Employee) {
  if (!e.assignment) return null
  const start = new Date(e.assignment.startDate + 'T00:00:00'),
    end = new Date(e.assignment.endDate + 'T23:59:59'),
    monthStart = new Date(date.value.getFullYear(), date.value.getMonth(), 1),
    monthEnd = new Date(date.value.getFullYear(), date.value.getMonth() + 1, 0)
  if (isNaN(+start) || isNaN(+end) || end < monthStart || start > monthEnd) return null
  const left = start < monthStart ? 1 : start.getDate(),
    right = end > monthEnd ? days.value : end.getDate()
  return {
    left: `${((left - 1) / days.value) * 100}%`,
    width: `${((right - left + 1) / days.value) * 100}%`,
  }
}
const link = (p: Employee) => (user.value?.role === 'MANAGER' ? '/people/' + p.id : undefined)
</script>

<template>
  <PageHeader subtitle="등록된 투입 일정을 조회합니다. 이 화면에서 배정을 변경하지 않습니다.">
    <div class="segmented" role="group" aria-label="보기 방식">
      <button
        :class="{ active: view === 'calendar' }"
        :aria-pressed="view === 'calendar'"
        @click="view = 'calendar'"
      >
        일정 달력</button
      ><button
        :class="{ active: view === 'table' }"
        :aria-pressed="view === 'table'"
        @click="view = 'table'"
      >
        현황 표
      </button>
    </div>
  </PageHeader>
  <div v-if="error" class="alert danger" role="alert">{{ error }}</div>
  <section class="card">
    <div class="toolbar">
      <div class="month-control">
        <button class="icon-btn" aria-label="이전 달" @click="move(-1)">
          <ChevronLeft :size="18" />
        </button>
        <strong>{{ month }}</strong>
        <button class="icon-btn" aria-label="다음 달" @click="move(1)">
          <ChevronRight :size="18" />
        </button>
        <button class="btn btn-outline-primary btn-sm" @click="date = new Date()">오늘</button>
      </div>
      <div class="filter-chips" role="group" aria-label="투입 상태 필터">
        <button
          :class="['filter-chip', { selected: status === 'ALL' }]"
          :aria-pressed="status === 'ALL'"
          @click="status = 'ALL'"
        >
          전체
        </button>
        <button
          v-for="s in statuses"
          :key="s"
          :class="['filter-chip', { selected: status === s }]"
          :aria-pressed="status === s"
          @click="status = s"
        >
          {{ availabilityLabel[s] }} <b>{{ people.filter((p) => p.availability === s).length }}</b>
        </button>
      </div>
      <label class="search-input"
        ><Search :size="16" /><input
          v-model="query"
          placeholder="이름, 소속, 프로젝트 검색"
          aria-label="투입 인력 검색"
      /></label>
    </div>

    <div v-if="view === 'calendar'" class="gantt-scroll">
      <div class="gantt">
        <div class="gantt-row gantt-head">
          <div class="gantt-label">인력 / 현재 상태</div>
          <div class="gantt-days" :style="{ gridTemplateColumns: `repeat(${days},1fr)` }">
            <span
              v-for="d in positions"
              :key="d"
              :class="{ weekend: weekend(d), today: todayPos && d === today.getDate() }"
              >{{ d }}</span
            >
          </div>
        </div>
        <div v-for="p in filtered" :key="p.id" class="gantt-row">
          <div class="gantt-label">
            <component :is="link(p) ? 'RouterLink' : 'span'" :to="link(p)" class="cell-strong">{{
              p.name
            }}</component>
            <StatusBadge :status="p.availability" />
          </div>
          <div class="gantt-track">
            <div class="gantt-grid" :style="{ gridTemplateColumns: `repeat(${days},1fr)` }">
              <i v-for="d in positions" :key="d" :class="{ weekend: weekend(d) }"></i>
            </div>
            <span v-if="todayPos" class="today-line" :style="{ left: todayPos }"></span>
            <div
              v-if="range(p)"
              class="gantt-bar"
              :style="range(p)!"
              :title="`${p.assignment?.projectTitle} · ${formatDate(p.assignment?.startDate)} ~ ${formatDate(p.assignment?.endDate)}`"
            >
              {{ p.assignment?.projectTitle }}
            </div>
            <small v-else class="gantt-empty">{{
              p.assignment ? '이번 달 일정 없음' : '일정 정보 없음'
            }}</small>
          </div>
        </div>
        <div v-if="!filtered.length" class="empty-state compact">
          <strong>조건에 맞는 인력이 없습니다</strong>
        </div>
      </div>
    </div>

    <div v-else class="table-responsive">
      <table class="table">
        <thead>
          <tr>
            <th>인력</th>
            <th>소속</th>
            <th>현재 상태</th>
            <th>투입 프로젝트</th>
            <th>투입 기간</th>
            <th style="width: 150px">투입률</th>
            <th>투입 가능일</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in filtered" :key="p.id">
            <td>
              <component :is="link(p) ? 'RouterLink' : 'span'" :to="link(p)" class="cell-strong">{{
                p.name
              }}</component>
            </td>
            <td>{{ p.department }}</td>
            <td><StatusBadge :status="p.availability" /></td>
            <td>{{ p.assignment?.projectTitle || '—' }}</td>
            <td>
              {{
                p.assignment
                  ? `${formatDate(p.assignment.startDate)} ~ ${formatDate(p.assignment.endDate)}`
                  : '—'
              }}
            </td>
            <td>
              <span class="progress-cell"
                ><span class="progress"><span :style="{ width: p.allocation + '%' }"></span></span
                >{{ p.allocation }}%</span
              >
            </td>
            <td>{{ formatDate(p.availableFrom) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <p class="hint">
      <Info :size="14" />현재 상태와 조회 월의 일정은 다를 수 있습니다. 세로선은 오늘입니다.
    </p>
  </section>
</template>
