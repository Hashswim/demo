import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import Dashboard from './views/Dashboard.vue'
import RequestNew from './views/RequestNew.vue'
import RequestList from './views/RequestList.vue'
import Results from './views/Results.vue'
import People from './views/People.vue'
import PersonEdit from './views/PersonEdit.vue'
import Allocation from './views/Allocation.vue'
import Shortlists from './views/Shortlists.vue'
import Evaluation from './views/Evaluation.vue'
import { api, user } from './lib/api'
import { allowed } from './lib/nav'
import type { Role } from './lib/types'
import './style.css'

export interface Crumb {
  label: string
  to?: string
}
declare module 'vue-router' {
  interface RouteMeta {
    title: string
    crumbs: Crumb[]
    nav: string
    roles?: Role[]
  }
}
const ACCOUNT: Role[] = ['ACCOUNT'],
  MANAGER: Role[] = ['MANAGER']
const requests = { label: '추천 요청', to: '/requests' },
  people = { label: '인력 정보 관리', to: '/people' }

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    { path: '/', component: Dashboard, meta: { title: '대시보드', crumbs: [], nav: '/' } },
    {
      path: '/requests/new',
      component: RequestNew,
      meta: { title: '새 추천 요청', crumbs: [requests], nav: '/requests/new', roles: ACCOUNT },
    },
    {
      path: '/requests',
      component: RequestList,
      meta: { title: '요청 목록', crumbs: [requests], nav: '/requests', roles: ACCOUNT },
    },
    {
      path: '/requests/:id',
      component: Results,
      meta: { title: '추천 결과', crumbs: [requests], nav: '/requests', roles: ACCOUNT },
    },
    { path: '/results/:id', redirect: (to) => `/requests/${to.params.id}` },
    {
      path: '/shortlists',
      component: Shortlists,
      meta: {
        title: '후보 보관함',
        crumbs: [{ label: '인력 추천' }],
        nav: '/shortlists',
        roles: ACCOUNT,
      },
    },
    {
      path: '/people',
      component: People,
      meta: { title: '인력 목록', crumbs: [people], nav: '/people', roles: MANAGER },
    },
    {
      path: '/people/new',
      component: PersonEdit,
      meta: { title: '인력 등록', crumbs: [people], nav: '/people/new', roles: MANAGER },
    },
    {
      path: '/people/:id',
      component: PersonEdit,
      meta: { title: '인력 정보 수정', crumbs: [people], nav: '/people', roles: MANAGER },
    },
    {
      path: '/allocation',
      component: Allocation,
      meta: { title: '투입 일정', crumbs: [{ label: '인력 운영' }], nav: '/allocation' },
    },
    {
      path: '/evaluation',
      component: Evaluation,
      meta: { title: '추천 품질 검증', crumbs: [{ label: '분석' }], nav: '/evaluation' },
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})
router.beforeEach(async (to) => {
  if (!user.value) await api.me()
  // 로그인 전에는 App이 로그인 화면을 그리고, 로그인 후 같은 주소로 다시 진입해 권한을 확인한다.
  if (user.value && !allowed(to.meta.roles, user.value.role)) return '/'
})
router.afterEach((to) => {
  document.title = `${to.meta.title || '대시보드'} · FIND`
})
createApp(App).use(router).mount('#app')
