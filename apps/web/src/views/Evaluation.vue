<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  ChartNoAxesCombined,
  RefreshCw,
  Target,
  ListChecks,
  FileCheck2,
  FlaskConical,
  Info,
} from 'lucide-vue-next'
import { api, demoMode } from '../lib/api'
import type { Evaluation } from '../lib/types'
import PageHeader from '../components/PageHeader.vue'

const result = ref<Evaluation>(),
  busy = ref(false),
  error = ref('')
const pct = (v: number) => `${(100 * v).toFixed(1)}%`
onMounted(() => {
  if (!demoMode) load()
})
async function load() {
  busy.value = true
  error.value = ''
  try {
    result.value = await api.evaluations()
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <PageHeader subtitle="정답 인력과 근거를 붙인 평가용 요청 세트로 추천 결과를 검증합니다.">
    <button class="btn btn-primary" :disabled="busy || demoMode" @click="load">
      <RefreshCw :size="16" :class="{ spin: busy }" />{{ busy ? '평가 중…' : '평가 실행' }}
    </button>
  </PageHeader>
  <div v-if="error" class="alert danger" role="alert">{{ error }}</div>
  <div v-if="busy" class="linear-progress" role="progressbar" aria-label="평가 진행 중">
    <span></span>
  </div>

  <section v-if="!result" class="card">
    <div class="empty-state">
      <ChartNoAxesCombined :size="36" />
      <strong>{{
        demoMode
          ? '실제 평가 결과는 API 모드에서 확인하세요'
          : busy
            ? '평가를 실행하고 있습니다'
            : '평가 결과가 없습니다'
      }}</strong>
      <p>
        {{
          demoMode
            ? '브라우저 데모에서는 임의의 성능 점수를 표시하지 않습니다.'
            : '평가 실행 버튼을 눌러 정답 세트의 결과를 계산하세요.'
        }}
      </p>
      <code v-if="demoMode">VITE_API_MODE=live npm run dev</code>
    </div>
  </section>

  <template v-else>
    <div class="stat-grid">
      <div class="card stat-card">
        <span class="stat-icon primary"><Target :size="20" /></span>
        <span class="stat-text"
          ><span class="stat-label">Hit@{{ result.k }}</span
          ><strong class="stat-value">{{ pct(result.hitAtK) }}</strong></span
        >
      </div>
      <div class="card stat-card">
        <span class="stat-icon info"><ListChecks :size="20" /></span>
        <span class="stat-text"
          ><span class="stat-label">Recall@{{ result.k }}</span
          ><strong class="stat-value">{{ pct(result.recallAtK) }}</strong></span
        >
      </div>
      <div class="card stat-card">
        <span class="stat-icon success"><FileCheck2 :size="20" /></span>
        <span class="stat-text"
          ><span class="stat-label">근거 일치 비율</span
          ><strong class="stat-value">{{ pct(result.evidenceAccuracy) }}</strong></span
        >
      </div>
      <div class="card stat-card">
        <span class="stat-icon warning"><FlaskConical :size="20" /></span>
        <span class="stat-text"
          ><span class="stat-label">평가용 요청</span
          ><strong class="stat-value">{{ result.requestCount }}<small>건</small></strong></span
        >
      </div>
    </div>
    <dl class="metric-help card">
      <div>
        <dt>Hit@{{ result.k }}</dt>
        <dd>정답 인력이 상위 {{ result.k }}명 안에 한 명이라도 포함된 요청의 비율</dd>
      </div>
      <div>
        <dt>Recall@{{ result.k }}</dt>
        <dd>요청별로 정답 인력을 상위 {{ result.k }}명 안에서 찾아낸 비율의 평균</dd>
      </div>
      <div>
        <dt>근거 일치</dt>
        <dd>‘확인’으로 표시한 근거가 정답 이력 ID와 원문에 맞는 비율</dd>
      </div>
    </dl>
    <section class="card">
      <div class="card-head">
        <div>
          <h2 class="card-title">요청별 검증 결과</h2>
          <p class="card-description">{{ result.mode }} · 현재 실행 기준</p>
        </div>
      </div>
      <div class="table-responsive">
        <table class="table">
          <thead>
            <tr>
              <th>평가 요청</th>
              <th>정답 인력 ID</th>
              <th>상위 {{ result.k }}명 ID</th>
              <th>Hit</th>
              <th class="num">Recall</th>
              <th class="num">근거 일치</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in result.cases" :key="c.id">
              <td class="cell-strong">{{ c.title }}</td>
              <td class="mono">{{ c.expectedIds.join(', ') }}</td>
              <td class="mono">{{ c.actualIds.join(', ') }}</td>
              <td>
                <span :class="['badge', c.hit ? 'badge-success' : 'badge-warning']">{{
                  c.hit ? '포함' : '미포함'
                }}</span>
              </td>
              <td class="num">{{ pct(c.recall) }}</td>
              <td class="num">{{ pct(c.evidenceAccuracy) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p class="hint">
        <Info :size="14" />정답 데이터는 추천 엔진에 전달되지 않습니다. 소규모 합성 세트 결과이므로
        실제 성능으로 해석하지 마세요.
      </p>
    </section>
  </template>
</template>
