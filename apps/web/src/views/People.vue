<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Plus, Search, Pencil, Users } from 'lucide-vue-next'
import { api } from '../lib/api'
import type { Employee, Availability } from '../lib/types'
import { availabilityLabel, formatDate } from '../lib/ui'
import PageHeader from '../components/PageHeader.vue'
import StatusBadge from '../components/StatusBadge.vue'

const items = ref<Employee[]>([]),
  query = ref(''),
  status = ref<'ALL' | Availability>('ALL'),
  sort = ref<'name' | 'department' | 'allocation'>('name'),
  loading = ref(true),
  error = ref('')
onMounted(async () => {
  try {
    items.value = await api.employees()
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
})
const statuses: Availability[] = ['AVAILABLE', 'ASSIGNED', 'UNKNOWN']
const filtered = computed(() =>
  items.value
    .filter(
      (e) =>
        (status.value === 'ALL' || e.availability === status.value) &&
        [e.name, e.title, e.department, ...e.skills, ...e.experiences.map((x) => x.title)]
          .join(' ')
          .toLowerCase()
          .includes(query.value.toLowerCase()),
    )
    .sort((a, b) =>
      sort.value === 'allocation'
        ? b.allocation - a.allocation
        : a[sort.value].localeCompare(b[sort.value], 'ko'),
    ),
)
</script>

<template>
  <PageHeader
    subtitle="보유 기술, 참여 이력, 투입 현황을 같은 기준으로 관리합니다. 행을 선택하면 상세 정보를 수정합니다."
  >
    <RouterLink class="btn btn-primary" to="/people/new"><Plus :size="17" />인력 등록</RouterLink>
  </PageHeader>
  <div v-if="error" class="alert danger" role="alert">{{ error }}</div>
  <section class="card">
    <div class="toolbar">
      <label class="search-input"
        ><Search :size="16" /><input
          v-model="query"
          placeholder="이름, 기술, 프로젝트 검색"
          aria-label="인력 검색"
      /></label>
      <div class="filter-chips" role="group" aria-label="투입 상태 필터">
        <button
          :class="['filter-chip', { selected: status === 'ALL' }]"
          :aria-pressed="status === 'ALL'"
          @click="status = 'ALL'"
        >
          전체 <b>{{ items.length }}</b>
        </button>
        <button
          v-for="s in statuses"
          :key="s"
          :class="['filter-chip', { selected: status === s }]"
          :aria-pressed="status === s"
          @click="status = s"
        >
          {{ availabilityLabel[s] }} <b>{{ items.filter((e) => e.availability === s).length }}</b>
        </button>
      </div>
      <label class="select-inline"
        >정렬
        <select v-model="sort" aria-label="정렬 기준">
          <option value="name">이름순</option>
          <option value="department">소속순</option>
          <option value="allocation">투입률 높은 순</option>
        </select></label
      >
    </div>
    <div class="table-responsive">
      <table class="table">
        <thead>
          <tr>
            <th>이름 / 직무</th>
            <th>소속</th>
            <th>보유 기술</th>
            <th class="num">참여 이력</th>
            <th style="width: 150px">투입률</th>
            <th>상태</th>
            <th>투입 가능일</th>
            <th class="actions">작업</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="p in filtered"
            :key="p.id"
            class="row-link"
            @click="$router.push('/people/' + p.id)"
          >
            <td>
              <span class="cell-person"
                ><span class="avatar sm">{{ p.name.slice(-2) }}</span
                ><span
                  ><strong>{{ p.name }}</strong
                  ><small>{{ p.title }}</small></span
                ></span
              >
            </td>
            <td>{{ p.department }}</td>
            <td>
              <span class="chips"
                ><span v-for="s in p.skills.slice(0, 3)" :key="s" class="chip">{{ s }}</span
                ><span v-if="p.skills.length > 3" class="chip chip-more"
                  >+{{ p.skills.length - 3 }}</span
                ></span
              >
            </td>
            <td class="num">{{ p.experiences.length }}건</td>
            <td>
              <span class="progress-cell"
                ><span class="progress"><span :style="{ width: p.allocation + '%' }"></span></span
                >{{ p.allocation }}%</span
              >
            </td>
            <td><StatusBadge :status="p.availability" /></td>
            <td>{{ formatDate(p.availableFrom) }}</td>
            <td class="actions">
              <RouterLink
                :to="'/people/' + p.id"
                class="icon-btn"
                :aria-label="p.name + ' 정보 수정'"
                @click.stop
                ><Pencil :size="16"
              /></RouterLink>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!loading && !filtered.length" class="empty-state compact">
        <Users :size="24" />
        <strong>{{
          items.length ? '검색 조건에 맞는 인력이 없습니다' : '등록된 인력이 없습니다'
        }}</strong>
      </div>
    </div>
    <div class="table-footer">총 {{ filtered.length }}명</div>
  </section>
</template>
