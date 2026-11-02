<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Layers3,
  Search,
  ChevronDown,
  ChevronsLeft,
  Menu,
  Bell,
  LogOut,
  Cpu,
  CalendarClock,
  CircleHelp,
  ArrowRight,
} from 'lucide-vue-next'
import { api, demoMode, user, aiMode } from './lib/api'
import { visibleSections, type NavItem } from './lib/nav'
import { readPref, writePref, daysUntil, formatDate } from './lib/ui'
import type { Employee, Role } from './lib/types'
import Snackbar from './components/Snackbar.vue'

const route = useRoute(),
  router = useRouter()
const role = ref<Role>('ACCOUNT'),
  username = ref(''),
  password = ref(''),
  error = ref(''),
  busy = ref(false)
const collapsed = ref(readPref('sidebarCollapsed', false)),
  mobileOpen = ref(false),
  menuQuery = ref(''),
  openMenu = ref<'' | 'alerts' | 'user'>(''),
  expanded = ref<Record<string, boolean>>({}),
  people = ref<Employee[]>([])

const roleName = computed(() =>
  user.value?.role === 'MANAGER' ? '인력 정보 관리자' : 'Account 담당자',
)
const initials = computed(() => (user.value?.role === 'MANAGER' ? 'HR' : 'AC'))
const sections = computed(() => visibleSections(user.value?.role, menuQuery.value))

// 상단바 알림: 담당자가 놓치기 쉬운 일정·데이터 확인 작업만 모은다.
const alerts = computed(() => {
  const ending = people.value
    .filter(
      (p) =>
        p.assignment &&
        daysUntil(p.assignment.endDate) >= 0 &&
        daysUntil(p.assignment.endDate) <= 30,
    )
    .map((p) => ({
      id: 'end-' + p.id,
      icon: CalendarClock,
      title: `${p.name} · 투입 종료 D-${daysUntil(p.assignment!.endDate)}`,
      detail: `${p.assignment!.projectTitle} · ${formatDate(p.assignment!.endDate)} 종료`,
      to: user.value?.role === 'MANAGER' ? '/people/' + p.id : '/allocation',
    }))
  const unknown =
    user.value?.role === 'MANAGER'
      ? people.value
          .filter((p) => p.availability === 'UNKNOWN')
          .map((p) => ({
            id: 'unk-' + p.id,
            icon: CircleHelp,
            title: `${p.name} · 투입 상태 확인 필요`,
            detail: `${p.department} · 현재 상태가 등록되지 않았습니다`,
            to: '/people/' + p.id,
          }))
      : []
  return [...ending, ...unknown]
})

const isActive = (to: string) => route.meta.nav === to
const groupActive = (item: NavItem) =>
  isActive(item.to) || !!item.children?.some((c) => isActive(c.to))
const isOpen = (item: NavItem) => expanded.value[item.to] ?? groupActive(item)
function toggleGroup(item: NavItem) {
  if (collapsed.value) {
    router.push(item.to)
    return
  }
  expanded.value[item.to] = !isOpen(item)
}
function toggleSidebar() {
  if (window.matchMedia('(max-width: 991px)').matches) mobileOpen.value = !mobileOpen.value
  else {
    collapsed.value = !collapsed.value
    writePref('sidebarCollapsed', collapsed.value)
  }
}
watch(
  () => route.fullPath,
  () => {
    mobileOpen.value = false
    openMenu.value = ''
    if (user.value) loadPeople()
  },
)

async function loadPeople() {
  try {
    people.value = await api.employees()
  } catch {
    people.value = []
  }
}
watch(user, (u) => (u ? loadPeople() : (people.value = [])), { immediate: true })

async function login() {
  busy.value = true
  error.value = ''
  try {
    await api.login(username.value, password.value, role.value)
    password.value = ''
    await router.replace(route.fullPath)
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    busy.value = false
  }
}
async function logout() {
  openMenu.value = ''
  await api.logout()
  await router.push('/')
}

function closeMenus(e: MouseEvent) {
  if (!(e.target as HTMLElement).closest('.dropdown')) openMenu.value = ''
}
function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    openMenu.value = ''
    mobileOpen.value = false
  }
}
onMounted(() => {
  document.addEventListener('click', closeMenus)
  document.addEventListener('keydown', onKey)
})
onUnmounted(() => {
  document.removeEventListener('click', closeMenus)
  document.removeEventListener('keydown', onKey)
})
</script>

<template>
  <div v-if="!user" class="auth-page">
    <form class="auth-card" @submit.prevent="login">
      <div class="auth-brand"><Layers3 :size="26" />FIND</div>
      <h1>업무를 시작해 볼까요?</h1>
      <p class="auth-sub">프로젝트 인력 추천 워크스페이스에 로그인하세요.</p>
      <div v-if="demoMode" class="alert info">
        가상 데이터 미리보기입니다. 실제 로그인·LLM 분석과 분리된 브라우저 데모입니다.
      </div>
      <div v-if="demoMode" class="field">
        <span class="field-label">사용자 역할</span>
        <div class="role-picker" role="radiogroup" aria-label="사용자 역할">
          <label :class="{ checked: role === 'ACCOUNT' }"
            ><input v-model="role" type="radio" value="ACCOUNT" /><strong>Account 담당자</strong
            ><small>추천 요청 · 후보 검토</small></label
          >
          <label :class="{ checked: role === 'MANAGER' }"
            ><input v-model="role" type="radio" value="MANAGER" /><strong>인력 정보 관리자</strong
            ><small>프로필 · 투입 현황 관리</small></label
          >
        </div>
      </div>
      <template v-else>
        <label class="field"
          ><span class="field-label">아이디</span
          ><input
            v-model="username"
            autocomplete="username"
            required
            placeholder="account 또는 manager"
        /></label>
        <label class="field"
          ><span class="field-label">비밀번호</span
          ><input v-model="password" type="password" autocomplete="current-password" required
        /></label>
      </template>
      <p v-if="error" class="field-error" role="alert">{{ error }}</p>
      <button class="btn btn-primary btn-lg btn-block" :disabled="busy">
        {{ busy ? '연결 중…' : demoMode ? '가상 데이터로 시작' : '로그인' }}
      </button>
      <p class="auth-foot">추천은 의사결정을 돕는 정보이며, 인력 배정은 담당자가 결정합니다.</p>
    </form>
  </div>

  <div v-else :class="['container-scroller', { 'sidebar-icon-only': collapsed }]">
    <nav :class="['sidebar', { open: mobileOpen }]" aria-label="주 메뉴">
      <div class="nav-profile">
        <div class="nav-profile-image">{{ initials }}</div>
        <strong>{{ user.username }}</strong>
        <small v-if="user.username !== roleName">{{ roleName }}</small>
      </div>
      <div class="sidebar-brand">
        <RouterLink to="/" class="brand-logo"><Layers3 :size="22" /><span>FIND</span></RouterLink>
        <small>프로젝트 인력 추천 ERP</small>
        <label class="sidebar-search"
          ><Search :size="15" /><input
            v-model="menuQuery"
            placeholder="메뉴 검색"
            aria-label="메뉴 검색"
        /></label>
      </div>
      <template v-for="section in sections" :key="section.head">
        <span class="nav-item-head">{{ section.head }}</span>
        <ul class="nav">
          <li
            v-for="item in section.items"
            :key="item.to"
            :class="['nav-item', { active: groupActive(item) }]"
          >
            <template v-if="item.children">
              <button
                class="nav-link"
                :aria-expanded="isOpen(item)"
                :title="collapsed ? item.label : undefined"
                @click="toggleGroup(item)"
              >
                <component :is="item.icon" class="menu-icon" :size="19" /><span
                  class="menu-title"
                  >{{ item.label }}</span
                ><ChevronDown :size="15" :class="['menu-arrow', { rotated: isOpen(item) }]" />
              </button>
              <ul v-show="isOpen(item) && !collapsed" class="sub-menu">
                <li v-for="child in item.children" :key="child.to">
                  <RouterLink
                    :to="child.to"
                    :class="['sub-link', { active: isActive(child.to) }]"
                    >{{ child.label }}</RouterLink
                  >
                </li>
              </ul>
            </template>
            <RouterLink
              v-else
              :to="item.to"
              class="nav-link"
              :title="collapsed ? item.label : undefined"
              ><component :is="item.icon" class="menu-icon" :size="19" /><span class="menu-title">{{
                item.label
              }}</span></RouterLink
            >
          </li>
        </ul>
      </template>
      <p v-if="!sections.length" class="nav-empty">일치하는 메뉴가 없습니다.</p>
    </nav>
    <div v-if="mobileOpen" class="sidebar-scrim" @click="mobileOpen = false"></div>

    <div class="page-body">
      <header class="navbar">
        <button
          class="navbar-toggler"
          :aria-label="collapsed ? '메뉴 펼치기' : '메뉴 접기'"
          @click="toggleSidebar"
        >
          <ChevronsLeft :size="20" class="toggler-desktop" :class="{ flipped: collapsed }" /><Menu
            :size="22"
            class="toggler-mobile"
          />
        </button>
        <span class="navbar-title">{{ route.meta.title }}</span>
        <div class="navbar-right">
          <span class="navbar-chip" :title="'추천 엔진 상태: ' + aiMode"
            ><Cpu :size="15" />{{ aiMode }}</span
          >
          <div class="dropdown">
            <button
              class="navbar-icon"
              :aria-expanded="openMenu === 'alerts'"
              :aria-label="`확인할 알림 ${alerts.length}건`"
              @click="openMenu = openMenu === 'alerts' ? '' : 'alerts'"
            >
              <Bell :size="20" /><span v-if="alerts.length" class="count-dot">{{
                alerts.length
              }}</span>
            </button>
            <div v-if="openMenu === 'alerts'" class="dropdown-menu wide">
              <h6>
                확인할 일 <small>{{ alerts.length }}건</small>
              </h6>
              <RouterLink
                v-for="a in alerts.slice(0, 6)"
                :key="a.id"
                :to="a.to"
                class="dropdown-item preview"
                ><span class="preview-icon"><component :is="a.icon" :size="16" /></span
                ><span
                  ><strong>{{ a.title }}</strong
                  ><small>{{ a.detail }}</small></span
                ></RouterLink
              >
              <p v-if="!alerts.length" class="dropdown-empty">지금 확인할 일정이 없습니다.</p>
              <RouterLink to="/allocation" class="dropdown-footer"
                >투입 일정 전체 보기<ArrowRight :size="14"
              /></RouterLink>
            </div>
          </div>
          <div class="dropdown">
            <button
              class="navbar-profile"
              :aria-expanded="openMenu === 'user'"
              @click="openMenu = openMenu === 'user' ? '' : 'user'"
            >
              <span class="mini-avatar">{{ initials }}</span
              ><span class="navbar-profile-name">{{ user.username }}</span
              ><ChevronDown :size="15" />
            </button>
            <div v-if="openMenu === 'user'" class="dropdown-menu right">
              <div class="dropdown-user">
                <strong>{{ user.username }}</strong
                ><small v-if="user.username !== roleName">{{ roleName }}</small>
              </div>
              <button class="dropdown-item" @click="logout"><LogOut :size="16" />로그아웃</button>
            </div>
          </div>
        </div>
      </header>
      <main class="content-wrapper"><RouterView /></main>
      <footer class="footer">
        <span>© FIND · Project Staffing Workspace</span>
        <span>추천은 의사결정을 지원하며 배정을 확정하지 않습니다.</span>
      </footer>
    </div>
  </div>
  <Snackbar />
</template>
