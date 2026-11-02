import { describe, it, expect } from 'vitest'
import { recommend } from './demo-engine'
import type { Employee, StaffingRequest } from './types'
const request: StaffingRequest = {
  id: 'r',
  title: 'MES',
  domain: '제조',
  description: '',
  skills: ['MES', 'Java'],
  requirements: [
    { id: 'q', text: 'MES', kind: 'MUST' },
    { id: 'q2', text: 'Java', kind: 'MUST' },
  ],
  startDate: '',
  endDate: '',
}
const person: Employee = {
  id: 'p',
  name: '가상인력',
  title: '개발자',
  department: '제조',
  skills: ['Java'],
  availability: 'UNKNOWN',
  availableFrom: null,
  allocation: 0,
  experiences: [
    {
      id: 'exp',
      title: '제조실행시스템 구축',
      domain: '제조',
      role: 'Java 개발',
      startDate: '2024-01-01',
      endDate: '2024-12-31',
      technologies: ['Java'],
      description: '제조실행시스템의 Java API를 개발했습니다.',
    },
  ],
}
describe('browser preview baseline', () => {
  it('uses source experience for alias match and preserves unknown availability', () => {
    const [r] = recommend(request, [person])
    expect(r.matchedRequirements).toBe(2)
    expect(r.evidence[0].experienceId).toBe('exp')
    expect(r.employee.availability).toBe('UNKNOWN')
  })
  it('does not credit project technology to PM development skills', () => {
    const [r] = recommend(request, [
      { ...person, experiences: [{ ...person.experiences[0], role: 'PM' }] },
    ])
    expect(r.evidence[1].status).toBe('UNCONFIRMED')
  })
  it('aggregates per person rather than per chunk', () => {
    expect(
      recommend(request, [
        { ...person, experiences: [...person.experiences, ...person.experiences] },
      ]),
    ).toHaveLength(1)
  })
})

it('avoids JavaScript false positives', () => {
  const [r] = recommend({ ...request, requirements: [{ id: 'j', text: 'Java', kind: 'MUST' }] }, [
    {
      ...person,
      experiences: [
        {
          ...person.experiences[0],
          description: 'JavaScript 개발',
          technologies: ['JavaScript'],
          title: '포털',
          role: '프런트 개발자',
        },
      ],
    },
  ])
  expect(r.matchedRequirements).toBe(0)
})
