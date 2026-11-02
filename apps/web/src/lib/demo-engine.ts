import type { Employee, Requirement, Recommendation, StaffingRequest } from './types'
const aliases: Record<string, string[]> = {
  MES: ['MES', '제조실행시스템'],
  Java: ['Java', '자바'],
  Spring: ['Spring', '스프링', 'Spring Boot'],
  Python: ['Python', '파이썬'],
  React: ['React', '리액트'],
  ERP: ['ERP', '전사적자원관리'],
  AWS: ['AWS', '아마존웹서비스'],
  SQL: ['SQL', 'PostgreSQL', '데이터베이스'],
  Vue: ['Vue', 'Vue.js', '뷰'],
}
function has(text: string, alias: string) {
  const escaped = alias.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return new RegExp(
    (/^[a-z0-9]/i.test(alias) ? '(?<![a-z0-9])' : '') +
      escaped +
      (/[a-z0-9]$/i.test(alias) ? '(?![a-z0-9])' : ''),
    'i',
  ).test(text)
}
export function tokens(text: string): string[] {
  const found = Object.entries(aliases)
    .filter(([, a]) => a.some((s) => has(text, s)))
    .map(([k]) => k)
  return found.length ? found : text.split(/[\s,·]+/).filter((s) => s.length > 1)
}
export function parseRequirements(description: string, skills: string[]): Requirement[] {
  const all = [...new Set([...skills, ...tokens(description)])].filter(Boolean)
  return all.map((text, i) => ({
    id: `req-${i + 1}`,
    text,
    kind: (aliases[text] || [text]).some((a) => {
      const at = description.toLowerCase().indexOf(a.toLowerCase())
      if (at < 0) return false
      const part = description.slice(at + a.length, at + a.length + 18)
      const p = part.search(/우대|선호/),
        m = part.search(/필수|반드시/)
      return p >= 0 && (m < 0 || p < m)
    })
      ? 'PREFERRED'
      : 'MUST',
  }))
}
export function recommend(request: StaffingRequest, employees: Employee[]): Recommendation[] {
  return employees
    .map((employee) => {
      const evidence = request.requirements.map((req) => {
        const keywords = tokens(req.text)
        const experience = employee.experiences.find((ex) => {
          if (
            /PM|관리자|기획/.test(ex.role) &&
            keywords.some((k) => ['Java', 'Python', 'Spring', 'React', 'Vue'].includes(k))
          )
            return false
          const txt = JSON.stringify(ex).toLowerCase()
          return keywords.every((k) => (aliases[k] || [k]).some((a) => has(txt, a)))
        })
        return {
          requirementId: req.id,
          requirementText: req.text,
          status: experience ? ('MATCHED' as const) : ('UNCONFIRMED' as const),
          experienceId: experience?.id ?? null,
          projectTitle: experience?.title ?? null,
          quote: experience?.description ?? '',
          explanation: experience
            ? `${experience.role} 참여 이력에서 확인했습니다.`
            : '등록된 참여 이력에서 확인되지 않음',
        }
      })
      const matched = evidence.filter((e) => e.status === 'MATCHED').length
      const totalWeight = request.requirements.reduce((v, r) => v + (r.kind === 'MUST' ? 2 : 1), 0)
      const matchedWeight = request.requirements.reduce(
        (v, r, i) => v + (evidence[i].status === 'MATCHED' ? (r.kind === 'MUST' ? 2 : 1) : 0),
        0,
      )
      return {
        employee,
        score: Math.round((100 * matchedWeight) / Math.max(1, totalWeight)),
        matchedRequirements: matched,
        totalRequirements: evidence.length,
        evidence,
      }
    })
    .sort((a, b) => b.score - a.score || a.employee.id.localeCompare(b.employee.id))
}
