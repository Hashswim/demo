import { ref } from 'vue'
import seed from '../demo/employees.json'
import type { Employee, User, Role, StaffingRequest, Job, Shortlist, Evaluation } from './types'
import { parseRequirements, recommend } from './demo-engine'
export const demoMode = import.meta.env.VITE_API_MODE === 'demo'
export const user = ref<User | null>(null)
export const aiMode = ref(demoMode ? '가상 데이터 · 별칭 검색 데모' : '내부 AI 상태 확인 중')
async function refreshMode() {
  try {
    const status = await live<{ mode: string }>('/system/status')
    aiMode.value = status.mode.includes('baseline')
      ? '내부 API · 별칭/텍스트 기준선'
      : '내부 API · 로컬 모델 모드'
  } catch {
    aiMode.value = '내부 AI 연결 확인 필요'
  }
}
let csrf: { token: string; headerName: string } | null = null
async function live<T>(path: string, method = 'GET', body?: unknown): Promise<T> {
  if (!csrf && method !== 'GET') csrf = await live('/auth/csrf')
  const res = await fetch('/api' + path, {
    method,
    credentials: 'include',
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(csrf && method !== 'GET' ? { [csrf.headerName]: csrf.token } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  })
  if (!res.ok) {
    const detail = await res.text()
    if (res.status === 401) user.value = null
    let message = ''
    try {
      message = JSON.parse(detail).message || ''
    } catch {
      message = detail.slice(0, 100)
    }
    throw new Error(message || `요청 실패 (${res.status})`)
  }
  const responseText = await res.text()
  return responseText ? JSON.parse(responseText) : (null as T)
}
const read = <T>(key: string, fallback: T): T => {
  try {
    return JSON.parse(localStorage.getItem('find:' + key) || 'null') ?? fallback
  } catch {
    return fallback
  }
}
const save = (k: string, v: unknown) => localStorage.setItem('find:' + k, JSON.stringify(v))
const id = () => crypto.randomUUID()
const employees = () => read<Employee[]>('employees', seed as Employee[])
const requests = () => read<StaffingRequest[]>('requests', [])
export const api = {
  async me() {
    if (demoMode) {
      user.value = JSON.parse(sessionStorage.getItem('find:user') || 'null')
      return user.value
    }
    try {
      user.value = await live<User>('/auth/me')
      await refreshMode()
      return user.value
    } catch {
      return null
    }
  },
  async login(username: string, password: string, role: Role) {
    if (demoMode) {
      user.value = { username: role === 'ACCOUNT' ? 'Account 담당자' : '인력 정보 관리자', role }
      sessionStorage.setItem('find:user', JSON.stringify(user.value))
      return user.value
    }
    csrf = null
    const signedIn = await live<User>('/auth/login', 'POST', { username, password })
    csrf = null
    user.value = signedIn
    await refreshMode()
    return signedIn
  },
  async logout() {
    if (!demoMode) await live('/auth/logout', 'POST')
    user.value = null
    csrf = null
    sessionStorage.removeItem('find:user')
  },
  async employees() {
    return demoMode ? employees() : live<Employee[]>('/employees')
  },
  async saveEmployee(e: Employee) {
    if (!demoMode)
      return live<Employee>(`/employees${e.id ? '/' + e.id : ''}`, e.id ? 'PUT' : 'POST', e)
    const list = employees()
    const item = { ...e, id: e.id || id() }
    const ix = list.findIndex((x) => x.id === item.id)
    if (ix < 0) list.push(item)
    else list[ix] = item
    save('employees', list)
    return item
  },
  async requests() {
    return demoMode ? requests() : live<StaffingRequest[]>('/requests')
  },
  async createRequest(r: StaffingRequest) {
    if (!demoMode) return live<StaffingRequest>('/requests', 'POST', r)
    const item = { ...r, id: id(), createdAt: new Date().toISOString() }
    save('requests', [item, ...requests()])
    return item
  },
  async parse(description: string, domain: string, skills: string[]) {
    return demoMode
      ? { requirements: parseRequirements(description, skills), mode: '브라우저 별칭 검색 데모' }
      : live<{ requirements: StaffingRequest['requirements']; mode: string }>(
          '/requirements/parse',
          'POST',
          { description, domain, skills },
        )
  },
  async latestJob(requestId: string) {
    if (!demoMode) return live<Job | null>(`/requests/${requestId}/latest-analysis`)
    const jobId = read<string | null>('requestJob:' + requestId, null)
    return jobId ? this.job(jobId) : null
  },
  async analyze(requestId: string) {
    if (!demoMode) return live<Job>(`/requests/${requestId}/analyze`, 'POST')
    const req = requests().find((r) => r.id === requestId)
    if (!req) throw new Error('요청을 찾을 수 없습니다.')
    const job: Job = {
      id: id(),
      requestId,
      status: 'SUCCEEDED',
      result: recommend(req, employees()),
      error: null,
    }
    save('job:' + job.id, job)
    save('requestJob:' + requestId, job.id)
    return job
  },
  async job(jobId: string) {
    if (!demoMode) return live<Job>('/jobs/' + jobId)
    const j = read<Job | null>('job:' + jobId, null)
    if (!j) throw new Error('분석 결과가 없습니다.')
    return {
      ...j,
      result:
        j.result?.map((r) => ({
          ...r,
          employee: employees().find((e) => e.id === r.employee.id) || r.employee,
        })) || null,
    }
  },
  async shortlists(requestId?: string) {
    return demoMode
      ? read<Shortlist[]>('shortlists', []).filter((x) => !requestId || x.requestId === requestId)
      : live<Shortlist[]>('/shortlists' + (requestId ? '?requestId=' + requestId : ''))
  },
  async shortlist(requestId: string, employeeId: string, reason: string) {
    if (!reason.trim()) throw new Error('후보 선택 이유를 적어 주세요.')
    if (!demoMode) return live<Shortlist>('/shortlists', 'POST', { requestId, employeeId, reason })
    const employee = employees().find((e) => e.id === employeeId)!
    const items = await this.shortlists()
    if (items.some((s) => s.requestId === requestId && s.employeeId === employeeId))
      throw new Error('이미 후보에 추가된 인력입니다.')
    const s = {
      id: id(),
      requestId,
      employeeId,
      reason,
      createdAt: new Date().toISOString(),
      employee,
    }
    save('shortlists', [s, ...items])
    return s
  },
  async removeShortlist(sid: string) {
    if (!demoMode) return live<void>('/shortlists/' + sid, 'DELETE')
    save(
      'shortlists',
      (await this.shortlists()).filter((s) => s.id !== sid),
    )
  },
  async evaluations(): Promise<Evaluation> {
    if (!demoMode) return live<Evaluation>('/evaluations')
    throw new Error(
      '브라우저 데모에서는 평가 점수를 만들지 않습니다. 실제 검증 결과는 API 모드에서 평가 세트를 실행해 확인하세요.',
    )
  },
}
