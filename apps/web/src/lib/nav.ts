import type { Component } from 'vue'
import {
  LayoutDashboard,
  ClipboardList,
  Bookmark,
  Users,
  CalendarDays,
  ChartNoAxesCombined,
} from 'lucide-vue-next'
import type { Role } from './types'

// 사이드바 정보 구조: 섹션(업무 영역) > 메뉴 > 하위 메뉴. 상세·편집 화면은
// 메뉴에 두지 않고 route meta.nav 로 부모 메뉴를 활성화해 3단계를 넘지 않게 한다.
export interface NavLink {
  to: string
  label: string
  roles?: Role[]
}
export interface NavItem extends NavLink {
  icon: Component
  children?: NavLink[]
}
export interface NavSection {
  head: string
  items: NavItem[]
}

export const sections: NavSection[] = [
  {
    head: '홈',
    items: [{ to: '/', label: '대시보드', icon: LayoutDashboard }],
  },
  {
    head: '인력 추천',
    items: [
      {
        to: '/requests',
        label: '추천 요청',
        icon: ClipboardList,
        roles: ['ACCOUNT'],
        children: [
          { to: '/requests/new', label: '새 요청 작성' },
          { to: '/requests', label: '요청 목록' },
        ],
      },
      { to: '/shortlists', label: '후보 보관함', icon: Bookmark, roles: ['ACCOUNT'] },
    ],
  },
  {
    head: '인력 운영',
    items: [
      {
        to: '/people',
        label: '인력 정보 관리',
        icon: Users,
        roles: ['MANAGER'],
        children: [
          { to: '/people', label: '인력 목록' },
          { to: '/people/new', label: '인력 등록' },
        ],
      },
      { to: '/allocation', label: '투입 일정', icon: CalendarDays },
    ],
  },
  {
    head: '분석',
    items: [{ to: '/evaluation', label: '추천 품질 검증', icon: ChartNoAxesCombined }],
  },
]

export const allowed = (roles: Role[] | undefined, role: Role | undefined) =>
  !roles || (!!role && roles.includes(role))

export function visibleSections(role: Role | undefined, query = '') {
  const q = query.trim().toLowerCase()
  return sections
    .map((s) => ({
      ...s,
      items: s.items.filter(
        (i) =>
          allowed(i.roles, role) &&
          (!q ||
            i.label.toLowerCase().includes(q) ||
            i.children?.some((c) => c.label.toLowerCase().includes(q))),
      ),
    }))
    .filter((s) => s.items.length)
}
