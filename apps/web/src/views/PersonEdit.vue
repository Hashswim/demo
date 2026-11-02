<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { Plus, Trash2, UserRound, CalendarClock, FolderKanban } from 'lucide-vue-next'
import { api } from '../lib/api'
import type { Employee, Availability } from '../lib/types'
import { toast } from '../lib/ui'
import PageHeader from '../components/PageHeader.vue'

const route = useRoute(),
  router = useRouter()
const blank = (): Employee => ({
  id: '',
  name: '',
  title: '',
  department: '',
  skills: [],
  availability: 'UNKNOWN',
  availableFrom: null,
  allocation: 0,
  experiences: [],
  assignment: null,
})
const form = ref<Employee>(blank()),
  skills = ref(''),
  original = ref(''),
  loading = ref(true),
  busy = ref(false),
  saved = ref(false),
  touched = ref(false),
  error = ref('')
const isNew = computed(() => route.path === '/people/new')
const snapshot = () => JSON.stringify({ ...form.value, skills: skills.value })
const dirty = computed(() => !loading.value && snapshot() !== original.value)

onMounted(async () => {
  try {
    if (!isNew.value) {
      const found = (await api.employees()).find((e) => e.id === route.params.id)
      if (!found) throw new Error('인력 정보를 찾을 수 없습니다.')
      form.value = JSON.parse(JSON.stringify(found))
    }
    skills.value = form.value.skills.join(', ')
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
    original.value = snapshot()
  }
})
onBeforeRouteLeave(
  () =>
    saved.value ||
    !dirty.value ||
    window.confirm('저장하지 않은 변경 사항이 있습니다. 나가시겠어요?'),
)

const missing = (v: string) => touched.value && !v.trim()
const assignmentInvalid = computed(
  () =>
    !!form.value.assignment?.startDate &&
    !!form.value.assignment?.endDate &&
    form.value.assignment.endDate < form.value.assignment.startDate,
)
function setAvailability(value: Availability) {
  form.value.availability = value
  if (value === 'ASSIGNED' && !form.value.assignment)
    form.value.assignment = { projectTitle: '', startDate: '', endDate: '' }
  if (value !== 'ASSIGNED') form.value.assignment = null
  if (value === 'AVAILABLE') form.value.allocation = 0
}
function addExperience() {
  form.value.experiences.unshift({
    id: crypto.randomUUID(),
    title: '',
    domain: '제조',
    role: '',
    startDate: '',
    endDate: '',
    technologies: [],
    description: '',
  })
}
function removeExperience(i: number) {
  const ex = form.value.experiences[i]
  if (ex.title && !window.confirm(`「${ex.title}」 이력을 삭제할까요?`)) return
  form.value.experiences.splice(i, 1)
}
async function save() {
  touched.value = true
  const f = form.value
  const problems = [
    (!f.name.trim() || !f.title.trim() || !f.department.trim()) &&
      '기본 정보의 이름·직무·소속을 입력해 주세요.',
    assignmentInvalid.value && '투입 종료일은 시작일 이후여야 합니다.',
    f.assignment &&
      (!f.assignment.projectTitle.trim() || !f.assignment.startDate || !f.assignment.endDate) &&
      '투입 프로젝트명과 기간을 입력해 주세요.',
    f.experiences.some(
      (e) =>
        !e.title.trim() ||
        !e.role.trim() ||
        !e.domain.trim() ||
        !e.startDate ||
        !e.description.trim(),
    ) && '프로젝트 이력의 프로젝트명·참여 역할·사업 분야·참여 시작일·수행 업무를 입력해 주세요.',
    f.experiences.some((e) => e.startDate && e.endDate && e.endDate < e.startDate) &&
      '이력 종료일은 시작일 이후여야 합니다.',
  ].filter(Boolean)
  if (problems.length) {
    error.value = problems[0] as string
    return
  }
  busy.value = true
  error.value = ''
  try {
    f.skills = skills.value
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean)
    await api.saveEmployee(f)
    saved.value = true
    toast(`${f.name} 님의 정보를 ${isNew.value ? '등록' : '저장'}했습니다.`)
    await router.push('/people')
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    busy.value = false
  }
}
const statusOptions: { value: Availability; label: string; help: string }[] = [
  { value: 'AVAILABLE', label: '투입 가능', help: '현재 비투입 · 투입률 0%' },
  { value: 'ASSIGNED', label: '투입 중', help: '투입 프로젝트와 기간 입력' },
  { value: 'UNKNOWN', label: '확인 필요', help: '현황을 아직 모름' },
]
</script>

<template>
  <PageHeader
    :title="isNew ? '인력 등록' : form.name ? `${form.name} · 정보 수정` : '인력 정보 수정'"
  >
    <span v-if="dirty" class="dirty-flag">저장하지 않은 변경 사항</span>
  </PageHeader>
  <div v-if="error" class="alert danger" role="alert">{{ error }}</div>

  <form v-if="!loading" class="edit-layout" novalidate @submit.prevent="save">
    <aside class="edit-index card" aria-label="입력 항목">
      <a href="#basic"><UserRound :size="16" />기본 정보</a>
      <a href="#allocation"><CalendarClock :size="16" />투입 현황</a>
      <a href="#experience"
        ><FolderKanban :size="16" />참여 이력
        <span class="count">{{ form.experiences.length }}</span></a
      >
    </aside>

    <div class="edit-sections">
      <section id="basic" class="card">
        <h2 class="card-title">기본 정보</h2>
        <div class="form-grid">
          <label class="field" :class="{ invalid: missing(form.name) }"
            ><span class="field-label">이름 <span class="req">*</span></span
            ><input v-model="form.name" autocomplete="off"
          /></label>
          <label class="field" :class="{ invalid: missing(form.title) }"
            ><span class="field-label">직무 <span class="req">*</span></span
            ><input v-model="form.title" placeholder="예: 백엔드 개발자"
          /></label>
          <label class="field" :class="{ invalid: missing(form.department) }"
            ><span class="field-label">소속 <span class="req">*</span></span
            ><input v-model="form.department" placeholder="예: 제조솔루션팀"
          /></label>
          <label class="field"
            ><span class="field-label">보유 기술</span
            ><input v-model="skills" placeholder="Java, MES, Spring" /><span class="field-help"
              >쉼표(,)로 구분합니다.</span
            ></label
          >
        </div>
      </section>

      <section id="allocation" class="card">
        <h2 class="card-title">투입 현황</h2>
        <div class="radio-cards" role="radiogroup" aria-label="투입 상태">
          <label
            v-for="o in statusOptions"
            :key="o.value"
            :class="{ checked: form.availability === o.value }"
            ><input
              type="radio"
              name="availability"
              :checked="form.availability === o.value"
              @change="setAvailability(o.value)"
            /><strong>{{ o.label }}</strong
            ><small>{{ o.help }}</small></label
          >
        </div>
        <div class="form-grid">
          <label class="field"
            ><span class="field-label">투입률 (%)</span
            ><input
              v-model.number="form.allocation"
              type="number"
              min="0"
              max="100"
              :disabled="form.availability === 'AVAILABLE'"
          /></label>
          <label class="field"
            ><span class="field-label">투입 가능일</span
            ><input
              type="date"
              :value="form.availableFrom || ''"
              @input="form.availableFrom = ($event.target as HTMLInputElement).value || null"
          /></label>
        </div>
        <template v-if="form.assignment">
          <h3 class="subhead">투입 프로젝트</h3>
          <div class="form-grid">
            <label
              class="field full"
              :class="{ invalid: touched && !form.assignment.projectTitle.trim() }"
              ><span class="field-label">프로젝트명 <span class="req">*</span></span
              ><input v-model="form.assignment.projectTitle"
            /></label>
            <label class="field" :class="{ invalid: touched && !form.assignment.startDate }"
              ><span class="field-label">시작일 <span class="req">*</span></span
              ><input v-model="form.assignment.startDate" type="date"
            /></label>
            <label
              class="field"
              :class="{ invalid: assignmentInvalid || (touched && !form.assignment.endDate) }"
              ><span class="field-label">종료일 <span class="req">*</span></span
              ><input
                v-model="form.assignment.endDate"
                type="date"
                :min="form.assignment.startDate"
              /><span v-if="assignmentInvalid" class="field-help"
                >종료일은 시작일 이후여야 합니다.</span
              ></label
            >
          </div>
        </template>
      </section>

      <section id="experience" class="card">
        <div class="card-head">
          <div>
            <h2 class="card-title">프로젝트 참여 이력</h2>
            <p class="card-description">
              추천 근거로 인용되므로 실제로 수행한 업무를 구체적으로 적어 주세요.
            </p>
          </div>
          <button type="button" class="btn btn-outline-primary" @click="addExperience">
            <Plus :size="16" />이력 추가
          </button>
        </div>
        <div v-if="!form.experiences.length" class="empty-state compact">
          <FolderKanban :size="24" />
          <strong>등록된 참여 이력이 없습니다</strong>
          <p>이력이 없으면 추천에서 모든 요건이 ‘확인되지 않음’으로 표시됩니다.</p>
        </div>
        <article v-for="(ex, i) in form.experiences" :key="ex.id" class="sub-card">
          <header>
            <strong>{{ ex.title || '새 이력' }}</strong>
            <button
              type="button"
              class="icon-btn danger"
              :aria-label="(ex.title || '새 이력') + ' 삭제'"
              @click="removeExperience(i)"
            >
              <Trash2 :size="16" />
            </button>
          </header>
          <div class="form-grid">
            <label class="field" :class="{ invalid: missing(ex.title) }"
              ><span class="field-label">프로젝트명 <span class="req">*</span></span
              ><input v-model="ex.title"
            /></label>
            <label class="field" :class="{ invalid: missing(ex.role) }"
              ><span class="field-label">참여 역할 <span class="req">*</span></span
              ><input v-model="ex.role" placeholder="예: Java 백엔드 개발"
            /></label>
            <label class="field" :class="{ invalid: missing(ex.domain) }"
              ><span class="field-label">사업 분야 <span class="req">*</span></span
              ><input v-model="ex.domain"
            /></label>
            <label class="field"
              ><span class="field-label">사용 기술</span
              ><input
                :value="ex.technologies.join(', ')"
                placeholder="쉼표로 구분"
                @input="
                  ex.technologies = ($event.target as HTMLInputElement).value
                    .split(',')
                    .map((s) => s.trim())
                    .filter(Boolean)
                "
            /></label>
            <label class="field" :class="{ invalid: touched && !ex.startDate }"
              ><span class="field-label">참여 시작 <span class="req">*</span></span
              ><input v-model="ex.startDate" type="date"
            /></label>
            <label class="field"
              ><span class="field-label">참여 종료</span
              ><input v-model="ex.endDate" type="date" :min="ex.startDate"
            /></label>
            <label class="field full" :class="{ invalid: missing(ex.description) }"
              ><span class="field-label">실제로 수행한 업무 <span class="req">*</span></span
              ><textarea v-model="ex.description" rows="3"></textarea>
            </label>
          </div>
        </article>
      </section>
    </div>

    <div class="action-bar">
      <span class="muted">{{ dirty ? '변경 사항이 있습니다.' : '변경 사항 없음' }}</span>
      <RouterLink to="/people" class="btn btn-text">취소</RouterLink>
      <button class="btn btn-primary" :disabled="busy">
        {{ busy ? '저장 중…' : isNew ? '등록' : '저장' }}
      </button>
    </div>
  </form>
</template>
