<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import { Sparkles, Plus, Trash2, FileText, ArrowRight, Check, Info } from 'lucide-vue-next'
import { api } from '../lib/api'
import type { Requirement } from '../lib/types'
import { toast } from '../lib/ui'
import PageHeader from '../components/PageHeader.vue'

const router = useRouter(),
  title = ref(''),
  domain = ref('제조'),
  description = ref(''),
  skillText = ref(''),
  startDate = ref(''),
  endDate = ref(''),
  requirements = ref<Requirement[]>([]),
  busy = ref(false),
  error = ref(''),
  touched = ref(false),
  parsed = ref(false),
  submitted = ref(false),
  mode = ref('')
const skills = () =>
  skillText.value
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
const step = computed(() => (parsed.value ? 2 : 1))
const mustCount = computed(() => requirements.value.filter((r) => r.kind === 'MUST').length)
const dirty = computed(() => !!(title.value || description.value || skillText.value))
const invalid = computed(() => ({
  title: touched.value && !title.value.trim(),
  description: touched.value && !description.value.trim(),
  period: !!(startDate.value && endDate.value && endDate.value < startDate.value),
}))

onBeforeRouteLeave(
  () =>
    submitted.value ||
    !dirty.value ||
    window.confirm('작성 중인 요청이 저장되지 않습니다. 나가시겠어요?'),
)

function sample() {
  title.value = '제조 고객사 MES 구축 제안'
  description.value =
    '제조 고객사의 MES(제조실행시스템) 구축 제안에 참여할 인력이 필요합니다. Java 개발 경험은 필수이며 Spring 경험을 우대합니다.'
  skillText.value = 'MES, Java'
  parsed.value = false
}
async function parse() {
  touched.value = true
  if (!title.value.trim() || !description.value.trim() || invalid.value.period) {
    error.value = '표시된 항목을 확인해 주세요.'
    return
  }
  busy.value = true
  error.value = ''
  try {
    const res = await api.parse(description.value, domain.value, skills())
    requirements.value = res.requirements
    mode.value = res.mode
    parsed.value = true
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    busy.value = false
  }
}
function addRequirement() {
  requirements.value.push({ id: crypto.randomUUID(), text: '', kind: 'PREFERRED' })
}
async function submit() {
  if (!requirements.value.length || requirements.value.some((r) => !r.text.trim())) {
    error.value = '비어 있는 요건을 채우거나 삭제해 주세요.'
    return
  }
  busy.value = true
  error.value = ''
  try {
    const r = await api.createRequest({
      id: '',
      title: title.value,
      domain: domain.value,
      description: description.value,
      skills: skills(),
      requirements: requirements.value,
      startDate: startDate.value,
      endDate: endDate.value,
    })
    submitted.value = true
    toast('추천 요청을 등록했습니다. 추천 분석을 시작합니다.')
    await router.push('/requests/' + r.id)
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <PageHeader
    subtitle="프로젝트 정보를 입력하고, 정리된 필수·우대 요건을 확인한 뒤 추천을 실행합니다."
  >
    <button class="btn btn-outline-primary" @click="sample">
      <FileText :size="16" />예시 불러오기
    </button>
  </PageHeader>

  <ol class="stepper" aria-label="진행 단계">
    <li :class="{ active: step === 1, done: step > 1 }">
      <span class="step-dot"><Check v-if="step > 1" :size="14" /><template v-else>1</template></span
      >프로젝트 정보
    </li>
    <li :class="{ active: step === 2 }"><span class="step-dot">2</span>요건 검토</li>
    <li><span class="step-dot">3</span>추천 결과</li>
  </ol>
  <div v-if="error" class="alert danger" role="alert">{{ error }}</div>

  <div class="grid-1-1">
    <section class="card">
      <div class="card-head">
        <div>
          <h2 class="card-title">1. 프로젝트 정보</h2>
          <p class="card-description"><span class="req">*</span> 표시는 필수 입력입니다.</p>
        </div>
      </div>
      <div class="form-grid">
        <label class="field full" :class="{ invalid: invalid.title }"
          ><span class="field-label">프로젝트명 <span class="req">*</span></span
          ><input
            v-model="title"
            placeholder="예: 제조 고객사 MES 구축 제안"
            @input="parsed = false"
          /><span v-if="invalid.title" class="field-help">프로젝트명을 입력해 주세요.</span></label
        >
        <label class="field"
          ><span class="field-label">사업 분야 <span class="req">*</span></span
          ><select v-model="domain">
            <option>제조</option>
            <option>금융</option>
            <option>유통</option>
            <option>공공</option>
            <option>통신</option>
            <option>기타</option>
          </select></label
        >
        <label class="field"
          ><span class="field-label">필요한 기술·지식</span
          ><input v-model="skillText" placeholder="MES, Java, Spring" /><span class="field-help"
            >쉼표(,)로 구분합니다.</span
          ></label
        >
        <label class="field full" :class="{ invalid: invalid.description }"
          ><span class="field-label">프로젝트 내용 <span class="req">*</span></span
          ><textarea
            v-model="description"
            rows="6"
            placeholder="사업 내용, 담당 역할, 반드시 필요한 경험과 우대 역량을 적어 주세요."
          ></textarea
          ><span class="field-help">{{
            invalid.description
              ? '프로젝트 내용을 입력해 주세요.'
              : '“필수”, “우대” 같은 표현이 요건 구분에 반영됩니다.'
          }}</span></label
        >
        <label class="field"
          ><span class="field-label">예상 시작일</span><input v-model="startDate" type="date"
        /></label>
        <label class="field" :class="{ invalid: invalid.period }"
          ><span class="field-label">예상 종료일</span
          ><input v-model="endDate" type="date" :min="startDate" /><span
            v-if="invalid.period"
            class="field-help"
            >종료일은 시작일 이후여야 합니다.</span
          ></label
        >
      </div>
      <div class="card-actions">
        <button class="btn btn-primary" :disabled="busy" @click="parse">
          <Sparkles :size="16" />{{
            busy && !parsed ? '정리 중…' : parsed ? '요건 다시 정리' : '요건 정리하기'
          }}
        </button>
      </div>
    </section>

    <section :class="['card', { 'card-disabled': !parsed }]">
      <div class="card-head">
        <div>
          <h2 class="card-title">2. 요건 검토</h2>
          <p class="card-description">
            {{
              parsed
                ? `필수 ${mustCount}개 · 우대 ${requirements.length - mustCount}개`
                : '1단계를 완료하면 수정 가능한 요건이 표시됩니다.'
            }}
          </p>
        </div>
      </div>
      <div v-if="!parsed" class="empty-state">
        <Sparkles :size="30" />
        <strong>필수 요건과 우대 요건</strong>
        <p>추천은 여기서 확정한 요건을 기준으로 이력 근거를 찾습니다.</p>
      </div>
      <template v-else>
        <div class="alert info">
          <Info :size="16" />{{ mode }} · 최종 요건은 담당자가 확인해 주세요.
        </div>
        <div class="table-responsive">
          <table class="table table-edit">
            <thead>
              <tr>
                <th style="width: 112px">구분</th>
                <th>요건</th>
                <th style="width: 48px"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(r, i) in requirements" :key="r.id">
                <td>
                  <select
                    v-model="r.kind"
                    :aria-label="'요건 ' + (i + 1) + ' 구분'"
                    :class="['kind-select', r.kind.toLowerCase()]"
                  >
                    <option value="MUST">필수</option>
                    <option value="PREFERRED">우대</option>
                  </select>
                </td>
                <td>
                  <input
                    v-model="r.text"
                    :aria-label="'요건 ' + (i + 1)"
                    placeholder="예: Java 개발 경험"
                  />
                </td>
                <td>
                  <button
                    class="icon-btn danger"
                    :aria-label="'요건 ' + (i + 1) + ' 삭제'"
                    @click="requirements.splice(i, 1)"
                  >
                    <Trash2 :size="16" />
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <button class="btn btn-text" @click="addRequirement"><Plus :size="16" />요건 추가</button>
        <p class="hint">등록된 이력에서 확인되지 않는 요건은 ‘확인되지 않음’으로 표시됩니다.</p>
        <div class="card-actions">
          <button class="btn btn-primary" :disabled="busy" @click="submit">
            요건 확정하고 추천 실행<ArrowRight :size="16" />
          </button>
        </div>
      </template>
    </section>
  </div>
</template>
