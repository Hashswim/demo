<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import {
  Plus,
  Users,
  UserCheck,
  UserRound,
  CircleHelp,
  ClipboardList,
  Bookmark,
  ArrowRight,
  CalendarClock,
  FileSearch,
  CircleCheck,
} from 'lucide-vue-next'
import { api, user } from '../lib/api'
import type { Employee, StaffingRequest, Shortlist, Availability } from '../lib/types'
import { availabilityLabel, daysUntil, formatDate } from '../lib/ui'
import PageHeader from '../components/PageHeader.vue'
import StatusBadge from '../components/StatusBadge.vue'

const people = ref<Employee[]>([]),
  requests = ref<StaffingRequest[]>([]),
  shortlists = ref<Shortlist[]>([]),
  loading = ref(true),
  error = ref('')
const isManager = computed(() => user.value?.role === 'MANAGER')

onMounted(async () => {
  try {
    people.value = await api.employees()
    if (!isManager.value)
      [requests.value, shortlists.value] = await Promise.all([api.requests(), api.shortlists()])
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
})

const count = (s: Availability) => people.value.filter((p) => p.availability === s).length
const statusOrder: Availability[] = ['AVAILABLE', 'ASSIGNED', 'UNKNOWN']
const distribution = computed(() =>
  statusOrder.map((s) => ({
    status: s,
    label: availabilityLabel[s],
    value: count(s),
    share: people.value.length ? (count(s) / people.value.length) * 100 : 0,
  })),
)
const departments = computed(() => {
  const map = new Map<string, { total: number; available: number }>()
  for (const p of people.value) {
    const d = map.get(p.department) || { total: 0, available: 0 }
    d.total++
    if (p.availability === 'AVAILABLE') d.available++
    map.set(p.department, d)
  }
  return [...map.entries()].map(([name, v]) => ({ name, ...v })).sort((a, b) => b.total - a.total)
})
const maxDept = computed(() => Math.max(1, ...departments.value.map((d) => d.total)))

const stats = computed(() =>
  isManager.value
    ? [
        {
          label: '등록 인력',
          value: people.value.length,
          unit: '명',
          icon: Users,
          tone: 'primary',
          to: '/people',
        },
        {
          label: '투입 가능',
          value: count('AVAILABLE'),
          unit: '명',
          icon: UserCheck,
          tone: 'success',
          to: '/allocation',
        },
        {
          label: '투입 중',
          value: count('ASSIGNED'),
          unit: '명',
          icon: UserRound,
          tone: 'info',
          to: '/allocation',
        },
        {
          label: '상태 확인 필요',
          value: count('UNKNOWN'),
          unit: '명',
          icon: CircleHelp,
          tone: 'warning',
          to: '/people',
        },
      ]
    : [
        {
          label: '내 추천 요청',
          value: requests.value.length,
          unit: '건',
          icon: ClipboardList,
          tone: 'primary',
          to: '/requests',
        },
        {
          label: '저장한 후보',
          value: shortlists.value.length,
          unit: '명',
          icon: Bookmark,
          tone: 'info',
          to: '/shortlists',
        },
        {
          label: '투입 가능 인력',
          value: count('AVAILABLE'),
          unit: '명',
          icon: UserCheck,
          tone: 'success',
          to: '/allocation',
        },
        {
          label: '전체 인력',
          value: people.value.length,
          unit: '명',
          icon: Users,
          tone: 'warning',
          to: '/allocation',
        },
      ],
)

// 처리할 일: 다음 행동으로 바로 이어지는 항목만 보여준다.
const tasks = computed(() => {
  const list: {
    id: string
    icon: typeof Plus
    title: string
    detail: string
    to: string
    cta: string
  }[] = []
  if (!isManager.value)
    for (const r of requests.value
      .filter((r) => !shortlists.value.some((s) => s.requestId === r.id))
      .slice(0, 3))
      list.push({
        id: 'r' + r.id,
        icon: FileSearch,
        title: `${r.title}`,
        detail: '아직 저장한 후보가 없습니다',
        to: '/requests/' + r.id,
        cta: '후보 검토',
      })
  for (const p of people.value) {
    if (p.assignment) {
      const d = daysUntil(p.assignment.endDate)
      if (d >= 0 && d <= 30)
        list.push({
          id: 'e' + p.id,
          icon: CalendarClock,
          title: `${p.name} 투입 종료 D-${d}`,
          detail: `${p.assignment.projectTitle} · ${formatDate(p.assignment.endDate)}`,
          to: isManager.value ? '/people/' + p.id : '/allocation',
          cta: isManager.value ? '현황 수정' : '일정 보기',
        })
    }
    if (isManager.value && p.availability === 'UNKNOWN')
      list.push({
        id: 'u' + p.id,
        icon: CircleHelp,
        title: `${p.name} 투입 상태 미등록`,
        detail: `${p.department} · ${p.title}`,
        to: '/people/' + p.id,
        cta: '상태 입력',
      })
    if (isManager.value && !p.experiences.length)
      list.push({
        id: 'x' + p.id,
        icon: FileSearch,
        title: `${p.name} 참여 이력 없음`,
        detail: '추천 근거로 쓸 이력이 없습니다',
        to: '/people/' + p.id,
        cta: '이력 추가',
      })
  }
  return list
})
const shortlistCount = (id: string) => shortlists.value.filter((s) => s.requestId === id).length
const availablePeople = computed(() => people.value.filter((p) => p.availability === 'AVAILABLE'))
</script>

<template>
  <PageHeader
    title="대시보드"
    :subtitle="
      isManager
        ? '인력 프로필과 투입 현황을 한눈에 관리합니다.'
        : '진행 중인 추천 요청과 후보 검토 현황입니다.'
    "
  >
    <RouterLink v-if="isManager" class="btn btn-outline-primary" to="/allocation"
      >투입 일정</RouterLink
    >
    <RouterLink v-else class="btn btn-outline-primary" to="/shortlists">후보 보관함</RouterLink>
    <RouterLink class="btn btn-primary" :to="isManager ? '/people/new' : '/requests/new'"
      ><Plus :size="17" />{{ isManager ? '인력 등록' : '새 추천 요청' }}</RouterLink
    >
  </PageHeader>
  <div v-if="error" class="alert danger" role="alert">{{ error }}</div>

  <div class="stat-grid">
    <RouterLink v-for="s in stats" :key="s.label" :to="s.to" class="card stat-card">
      <span :class="['stat-icon', s.tone]"><component :is="s.icon" :size="20" /></span>
      <span class="stat-text">
        <span class="stat-label">{{ s.label }}</span>
        <strong class="stat-value"
          >{{ loading ? '–' : s.value }}<small>{{ s.unit }}</small></strong
        >
      </span>
      <ArrowRight :size="16" class="stat-go" />
    </RouterLink>
  </div>

  <div class="grid-2-1">
    <section class="card">
      <div class="card-head">
        <div>
          <h2 class="card-title">인력 투입 현황</h2>
          <p class="card-description">현재 등록된 상태 기준 · 총 {{ people.length }}명</p>
        </div>
        <RouterLink to="/allocation" class="btn btn-text"
          >일정 보기<ArrowRight :size="15"
        /></RouterLink>
      </div>
      <div
        class="stack-bar"
        role="img"
        :aria-label="distribution.map((d) => `${d.label} ${d.value}명`).join(', ')"
      >
        <span
          v-for="d in distribution.filter((d) => d.value)"
          :key="d.status"
          :class="['stack-seg', d.status.toLowerCase()]"
          :style="{ width: d.share + '%' }"
          :title="`${d.label} ${d.value}명 (${d.share.toFixed(0)}%)`"
        ></span>
      </div>
      <ul class="legend">
        <li v-for="d in distribution" :key="d.status">
          <StatusBadge :status="d.status" /><b>{{ d.value }}명</b
          ><small>{{ d.share.toFixed(0) }}%</small>
        </li>
      </ul>
      <h3 class="subhead">소속별 인원 <small>막대: 전체 · 숫자: 투입 가능/전체</small></h3>
      <ul class="bar-list">
        <li
          v-for="d in departments"
          :key="d.name"
          :title="`${d.name}: 전체 ${d.total}명, 투입 가능 ${d.available}명`"
        >
          <span class="bar-label">{{ d.name }}</span>
          <span class="bar-track"
            ><span class="bar-fill" :style="{ width: (d.total / maxDept) * 100 + '%' }"></span
          ></span>
          <span class="bar-value">{{ d.available }}/{{ d.total }}</span>
        </li>
      </ul>
    </section>

    <section class="card">
      <div class="card-head">
        <div>
          <h2 class="card-title">처리할 일</h2>
          <p class="card-description">바로 처리할 수 있는 항목 {{ tasks.length }}건</p>
        </div>
      </div>
      <div v-if="!tasks.length && !loading" class="empty-state compact">
        <CircleCheck :size="28" />
        <strong>처리할 일이 없습니다</strong>
      </div>
      <ul class="task-list">
        <li v-for="t in tasks.slice(0, 7)" :key="t.id">
          <RouterLink :to="t.to" class="task">
            <span class="task-icon"><component :is="t.icon" :size="16" /></span>
            <span class="task-text"
              ><strong>{{ t.title }}</strong
              ><small>{{ t.detail }}</small></span
            >
            <span class="task-cta">{{ t.cta }}</span>
          </RouterLink>
        </li>
      </ul>
    </section>
  </div>

  <section v-if="!isManager" class="card">
    <div class="card-head">
      <div>
        <h2 class="card-title">최근 추천 요청</h2>
        <p class="card-description">요청을 선택하면 추천 결과와 근거를 확인합니다.</p>
      </div>
      <RouterLink to="/requests" class="btn btn-text"
        >전체 목록<ArrowRight :size="15"
      /></RouterLink>
    </div>
    <div v-if="!requests.length && !loading" class="empty-state">
      <ClipboardList :size="32" />
      <strong>아직 등록한 추천 요청이 없습니다</strong>
      <p>예: “제조 고객사 MES 구축 제안, Java 개발 경험 필요”</p>
      <RouterLink to="/requests/new" class="btn btn-primary"
        ><Plus :size="16" />첫 요청 작성</RouterLink
      >
    </div>
    <div v-else class="table-responsive">
      <table class="table">
        <thead>
          <tr>
            <th>프로젝트</th>
            <th>사업 분야</th>
            <th class="num">요건</th>
            <th class="num">저장 후보</th>
            <th>등록일</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="r in requests.slice(0, 5)"
            :key="r.id"
            class="row-link"
            @click="$router.push('/requests/' + r.id)"
          >
            <td>
              <RouterLink :to="'/requests/' + r.id" class="cell-strong" @click.stop>{{
                r.title
              }}</RouterLink>
            </td>
            <td>{{ r.domain }}</td>
            <td class="num">{{ r.requirements.length }}</td>
            <td class="num">{{ shortlistCount(r.id) }}</td>
            <td>{{ formatDate(r.createdAt) }}</td>
            <td class="actions"><ArrowRight :size="16" /></td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>

  <section v-else class="card">
    <div class="card-head">
      <div>
        <h2 class="card-title">지금 투입 가능한 인력</h2>
        <p class="card-description">추천 화면에서 우선 검토되는 인력입니다.</p>
      </div>
      <RouterLink to="/people" class="btn btn-text">인력 목록<ArrowRight :size="15" /></RouterLink>
    </div>
    <div class="table-responsive">
      <table class="table">
        <thead>
          <tr>
            <th>이름</th>
            <th>직무</th>
            <th>소속</th>
            <th>보유 기술</th>
            <th class="num">참여 이력</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="p in availablePeople.slice(0, 6)"
            :key="p.id"
            class="row-link"
            @click="$router.push('/people/' + p.id)"
          >
            <td>
              <RouterLink :to="'/people/' + p.id" class="cell-strong" @click.stop>{{
                p.name
              }}</RouterLink>
            </td>
            <td>{{ p.title }}</td>
            <td>{{ p.department }}</td>
            <td>
              <span class="chips"
                ><span v-for="s in p.skills.slice(0, 3)" :key="s" class="chip">{{ s }}</span></span
              >
            </td>
            <td class="num">{{ p.experiences.length }}</td>
            <td class="actions"><ArrowRight :size="16" /></td>
          </tr>
        </tbody>
      </table>
      <div v-if="!availablePeople.length && !loading" class="empty-state compact">
        <strong>투입 가능한 인력이 없습니다</strong>
      </div>
    </div>
  </section>
</template>
