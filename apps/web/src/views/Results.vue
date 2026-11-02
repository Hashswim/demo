<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  RefreshCw,
  Bookmark,
  BookmarkCheck,
  CircleCheck,
  CircleHelp,
  Search,
  Users,
  Info,
} from 'lucide-vue-next'
import { api, demoMode, aiMode } from '../lib/api'
import type { StaffingRequest, Recommendation, Job, Shortlist } from '../lib/types'
import { toast, formatDate } from '../lib/ui'
import PageHeader from '../components/PageHeader.vue'
import StatusBadge from '../components/StatusBadge.vue'
import Modal from '../components/Modal.vue'

const route = useRoute(),
  req = ref<StaffingRequest>(),
  items = ref<Recommendation[]>([]),
  saved = ref<Shortlist[]>([]),
  selected = ref<Recommendation>(),
  job = ref<Job>(),
  onlyAvailable = ref(false),
  query = ref(''),
  error = ref(''),
  busy = ref(false),
  dialog = ref(false),
  reason = ref(''),
  saving = ref(false),
  tab = ref<'evidence' | 'history' | 'profile'>('evidence')
let stopped = false

const filtered = computed(() =>
  items.value.filter(
    (r) =>
      (!onlyAvailable.value || r.employee.availability === 'AVAILABLE') &&
      `${r.employee.name} ${r.employee.department} ${r.employee.skills.join(' ')}`
        .toLowerCase()
        .includes(query.value.toLowerCase()),
  ),
)
const rankOf = (r: Recommendation) => items.value.indexOf(r) + 1
const isSaved = (employeeId: string) => saved.value.some((s) => s.employeeId === employeeId)
const statusText = computed(() =>
  job.value?.status === 'RUNNING'
    ? '참여 이력을 분석하고 있습니다'
    : '분석 작업을 준비하고 있습니다',
)

onMounted(async () => {
  try {
    req.value = (await api.requests()).find((r) => r.id === route.params.id)
    if (!req.value) throw new Error('추천 요청을 찾을 수 없습니다.')
    saved.value = await api.shortlists(req.value.id)
    const previous = await api.latestJob(req.value.id)
    await analyze(previous || undefined)
  } catch (e) {
    error.value = (e as Error).message
  }
})
onUnmounted(() => (stopped = true))

async function analyze(previous?: Job) {
  if (!req.value) return
  busy.value = true
  error.value = ''
  try {
    job.value = previous || (await api.analyze(req.value.id))
    while (!stopped && (job.value.status === 'QUEUED' || job.value.status === 'RUNNING')) {
      await new Promise((r) => setTimeout(r, 1200))
      if (stopped) return
      job.value = await api.job(job.value.id)
    }
    if (job.value.status === 'FAILED')
      throw new Error(job.value.error || '분석이 완료되지 않았습니다.')
    items.value = job.value.result || []
    selected.value = items.value[0]
    tab.value = 'evidence'
    if (!previous) toast(`추천 분석을 완료했습니다. ${items.value.length}명을 검토할 수 있습니다.`)
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    busy.value = false
  }
}
function choose(r: Recommendation) {
  selected.value = r
  tab.value = 'evidence'
}
async function save() {
  if (!req.value || !selected.value) return
  saving.value = true
  try {
    const s = await api.shortlist(req.value.id, selected.value.employee.id, reason.value)
    saved.value = [s, ...saved.value.filter((x) => x.employeeId !== s.employeeId)]
    toast(`${selected.value.employee.name} 님을 후보 보관함에 저장했습니다.`, {
      action: { label: '보관함 보기', to: '/shortlists' },
    })
    dialog.value = false
    reason.value = ''
  } catch (e) {
    toast((e as Error).message, { tone: 'error' })
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <PageHeader
    :title="req?.title || '추천 결과'"
    subtitle="점수는 요건과 이력의 일치 정도이며 업무 성과의 확률이 아닙니다."
  >
    <RouterLink class="btn btn-outline-primary" to="/shortlists"
      ><Bookmark :size="16" />후보 보관함
      <b v-if="saved.length" class="btn-count">{{ saved.length }}</b></RouterLink
    >
    <button class="btn btn-primary" :disabled="busy || !req" @click="analyze()">
      <RefreshCw :size="16" :class="{ spin: busy }" />다시 분석
    </button>
  </PageHeader>
  <div v-if="error" class="alert danger" role="alert">{{ error }}</div>

  <section v-if="req" class="card request-summary">
    <dl class="info-grid">
      <div>
        <dt>사업 분야</dt>
        <dd>{{ req.domain }}</dd>
      </div>
      <div>
        <dt>예상 기간</dt>
        <dd>
          {{
            req.startDate || req.endDate
              ? `${formatDate(req.startDate)} ~ ${formatDate(req.endDate)}`
              : '미정'
          }}
        </dd>
      </div>
      <div>
        <dt>등록일</dt>
        <dd>{{ formatDate(req.createdAt) }}</dd>
      </div>
      <div>
        <dt>분석 방식</dt>
        <dd>{{ demoMode ? '브라우저 별칭 검색 데모' : aiMode }}</dd>
      </div>
    </dl>
    <div class="req-chips">
      <span class="info-label">추천 기준</span>
      <span
        v-for="r in req.requirements"
        :key="r.id"
        :class="['chip', r.kind === 'MUST' ? 'chip-must' : 'chip-pref']"
        ><b>{{ r.kind === 'MUST' ? '필수' : '우대' }}</b
        >{{ r.text }}</span
      >
    </div>
  </section>

  <div v-if="busy" class="linear-progress" role="progressbar" aria-label="분석 진행 중">
    <span></span>
  </div>

  <div class="master-detail">
    <section class="card master">
      <div class="card-head tight">
        <h2 class="card-title">
          추천 인력 <span class="count">{{ filtered.length }}</span>
        </h2>
        <label class="switch"
          ><input v-model="onlyAvailable" type="checkbox" /><span class="switch-track"></span>투입
          가능만</label
        >
      </div>
      <label class="search-input full"
        ><Search :size="16" /><input
          v-model="query"
          placeholder="이름, 소속, 기술 검색"
          aria-label="추천 인력 검색"
      /></label>
      <div v-if="busy" class="empty-state compact">
        <RefreshCw class="spin" :size="26" />
        <strong>{{ statusText }}</strong>
        <p>작업 상태: {{ job?.status || '연결 중' }} · 페이지를 닫아도 서버 작업은 계속됩니다.</p>
      </div>
      <div v-else-if="!filtered.length" class="empty-state compact">
        <Search :size="24" />
        <strong>조건에 맞는 인력이 없습니다</strong>
      </div>
      <ul v-else class="candidate-list" role="listbox" aria-label="추천 인력">
        <li v-for="r in filtered" :key="r.employee.id">
          <button
            :class="['candidate', { selected: selected?.employee.id === r.employee.id }]"
            role="option"
            :aria-selected="selected?.employee.id === r.employee.id"
            @click="choose(r)"
          >
            <span class="rank">{{ rankOf(r) }}</span>
            <span class="candidate-main">
              <span class="candidate-name"
                >{{ r.employee.name }}<small>{{ r.employee.title }}</small
                ><BookmarkCheck
                  v-if="isSaved(r.employee.id)"
                  :size="15"
                  class="saved-mark"
                  aria-label="저장됨"
              /></span>
              <span class="candidate-meta">{{ r.employee.department }}</span>
              <span class="score-row">
                <span class="score-track"
                  ><span class="score-fill" :style="{ width: r.score + '%' }"></span
                ></span>
                <span class="score-text"
                  >요건 {{ r.matchedRequirements }}/{{ r.totalRequirements }}</span
                >
              </span>
            </span>
            <span class="candidate-side">
              <strong class="score">{{ r.score }}</strong>
              <StatusBadge :status="r.employee.availability" />
            </span>
          </button>
        </li>
      </ul>
    </section>

    <section class="card detail">
      <template v-if="selected">
        <div class="detail-head">
          <span class="avatar lg">{{ selected.employee.name.slice(-2) }}</span>
          <div class="detail-title">
            <h2>
              {{ selected.employee.name }} <small>{{ selected.employee.title }}</small>
            </h2>
            <p>
              {{ selected.employee.department }} · 사번 {{ selected.employee.id }} ·
              <StatusBadge :status="selected.employee.availability" />
            </p>
          </div>
          <button v-if="isSaved(selected.employee.id)" class="btn btn-outline-primary" disabled>
            <BookmarkCheck :size="16" />저장됨
          </button>
          <button v-else class="btn btn-primary" @click="dialog = true">
            <Bookmark :size="16" />후보 저장
          </button>
        </div>
        <div class="kpi-inline">
          <div>
            <span>일치 점수</span><strong>{{ selected.score }}</strong>
          </div>
          <div>
            <span>확인된 요건</span
            ><strong>{{ selected.matchedRequirements }} / {{ selected.totalRequirements }}</strong>
          </div>
          <div>
            <span>투입률</span><strong>{{ selected.employee.allocation }}%</strong>
          </div>
          <div>
            <span>투입 가능일</span
            ><strong>{{ formatDate(selected.employee.availableFrom) }}</strong>
          </div>
        </div>
        <div class="tabs" role="tablist">
          <button
            role="tab"
            :aria-selected="tab === 'evidence'"
            :class="{ active: tab === 'evidence' }"
            @click="tab = 'evidence'"
          >
            요건별 근거
          </button>
          <button
            role="tab"
            :aria-selected="tab === 'history'"
            :class="{ active: tab === 'history' }"
            @click="tab = 'history'"
          >
            프로젝트 이력 <span class="count">{{ selected.employee.experiences.length }}</span>
          </button>
          <button
            role="tab"
            :aria-selected="tab === 'profile'"
            :class="{ active: tab === 'profile' }"
            @click="tab = 'profile'"
          >
            기술·투입 정보
          </button>
        </div>

        <div v-if="tab === 'evidence'" class="tab-panel" role="tabpanel">
          <article
            v-for="e in selected.evidence"
            :key="e.requirementId"
            :class="['evidence', e.status.toLowerCase()]"
          >
            <header>
              <component
                :is="e.status === 'MATCHED' ? CircleCheck : CircleHelp"
                :size="17"
                class="evidence-icon"
              />
              <strong>{{ e.requirementText }}</strong>
              <span
                :class="['badge', e.status === 'MATCHED' ? 'badge-success' : 'badge-warning']"
                >{{ e.status === 'MATCHED' ? '이력에서 확인' : '확인되지 않음' }}</span
              >
            </header>
            <template v-if="e.experienceId">
              <p class="evidence-source">
                {{ e.projectTitle }} <small>이력 ID {{ e.experienceId }}</small>
              </p>
              <blockquote>{{ e.quote }}</blockquote>
              <p class="evidence-note">{{ e.explanation }}</p>
            </template>
            <p v-else class="evidence-note">
              이 요건을 뒷받침하는 등록된 참여 이력이 없습니다. 담당자 확인이 필요합니다.
            </p>
          </article>
        </div>

        <div v-if="tab === 'history'" class="tab-panel" role="tabpanel">
          <ol class="timeline">
            <li v-for="ex in selected.employee.experiences" :key="ex.id">
              <span class="timeline-date"
                >{{ formatDate(ex.startDate) }} ~ {{ formatDate(ex.endDate) }}</span
              >
              <strong>{{ ex.title }}</strong>
              <span class="muted">{{ ex.domain }} · {{ ex.role }}</span>
              <p>{{ ex.description }}</p>
              <span class="chips"
                ><span v-for="t in ex.technologies" :key="t" class="chip">{{ t }}</span></span
              >
            </li>
          </ol>
        </div>

        <div v-if="tab === 'profile'" class="tab-panel" role="tabpanel">
          <h3 class="subhead">보유 기술</h3>
          <span class="chips"
            ><span v-for="s in selected.employee.skills" :key="s" class="chip">{{ s }}</span></span
          >
          <h3 class="subhead">현재 투입 현황</h3>
          <dl class="info-grid">
            <div>
              <dt>상태</dt>
              <dd><StatusBadge :status="selected.employee.availability" /></dd>
            </div>
            <div>
              <dt>투입률</dt>
              <dd>{{ selected.employee.allocation }}%</dd>
            </div>
            <div>
              <dt>투입 프로젝트</dt>
              <dd>{{ selected.employee.assignment?.projectTitle || '—' }}</dd>
            </div>
            <div>
              <dt>투입 기간</dt>
              <dd>
                {{
                  selected.employee.assignment
                    ? `${formatDate(selected.employee.assignment.startDate)} ~ ${formatDate(selected.employee.assignment.endDate)}`
                    : '—'
                }}
              </dd>
            </div>
          </dl>
          <p class="hint">
            <Info :size="14" />근거는 분석 당시 기준이며, 투입 상태는 조회 시점 기준입니다.
          </p>
          <RouterLink class="btn btn-text" to="/allocation">투입 일정에서 보기</RouterLink>
        </div>
      </template>
      <div v-else class="empty-state">
        <Users :size="30" />
        <strong>왼쪽 목록에서 인력을 선택하세요</strong>
      </div>
    </section>
  </div>

  <Modal
    v-if="dialog && selected"
    :title="selected.employee.name + ' 님을 후보로 저장'"
    @close="dialog = false"
  >
    <p class="muted">배정을 확정하지 않습니다. 검토할 후보와 선택 이유를 보관함에 남깁니다.</p>
    <label class="field"
      ><span class="field-label">선택 이유 <span class="req">*</span></span
      ><textarea
        v-model="reason"
        rows="4"
        maxlength="2000"
        placeholder="어떤 경험이 이 프로젝트에 적합한가요?"
      ></textarea
      ><span class="field-help">{{ reason.length }} / 2000</span></label
    >
    <template #footer>
      <button class="btn btn-text" @click="dialog = false">취소</button>
      <button class="btn btn-primary" :disabled="saving || !reason.trim()" @click="save">
        {{ saving ? '저장 중…' : '후보 저장' }}
      </button>
    </template>
  </Modal>
</template>
