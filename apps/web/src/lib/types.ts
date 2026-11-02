export type Role = 'ACCOUNT' | 'MANAGER'
export type Availability = 'AVAILABLE' | 'ASSIGNED' | 'UNKNOWN'
export interface Experience {
  id: string
  title: string
  domain: string
  role: string
  startDate: string
  endDate: string
  technologies: string[]
  description: string
}
export interface Employee {
  id: string
  name: string
  title: string
  department: string
  skills: string[]
  availability: Availability
  availableFrom: string | null
  allocation: number
  experiences: Experience[]
  assignment?: { projectTitle: string; startDate: string; endDate: string } | null
}
export interface Requirement {
  id: string
  text: string
  kind: 'MUST' | 'PREFERRED'
}
export interface StaffingRequest {
  id: string
  title: string
  domain: string
  description: string
  skills: string[]
  requirements: Requirement[]
  startDate: string
  endDate: string
  createdAt?: string
}
export interface Evidence {
  requirementId: string
  requirementText: string
  status: 'MATCHED' | 'UNCONFIRMED'
  experienceId: string | null
  projectTitle: string | null
  quote: string
  explanation: string
}
export interface Recommendation {
  employee: Employee
  score: number
  matchedRequirements: number
  totalRequirements: number
  evidence: Evidence[]
}
export interface Job {
  id: string
  requestId: string
  status: 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED'
  result: Recommendation[] | null
  error: string | null
}
export interface Shortlist {
  id: string
  requestId: string
  employeeId: string
  reason: string
  createdAt: string
  employee: Employee
}
export interface Evaluation {
  mode: string
  k: number
  requestCount: number
  hitAtK: number
  recallAtK: number
  evidenceAccuracy: number
  cases: {
    id: string
    title: string
    expectedIds: string[]
    actualIds: string[]
    hit: boolean
    recall: number
    evidenceAccuracy: number
  }[]
}
export interface User {
  username: string
  role: Role
}
