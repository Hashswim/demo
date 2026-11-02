# Integration contract v1
All JSON camelCase. Same-origin browser `/api`. Frontend demo adapter uses identical types, visibly marked demo. Spring server is source of truth for authentication, role, CRUD, jobs, shortlist.

Employee: `{id:string,name,title,department,skills:string[],availability:"AVAILABLE"|"ASSIGNED"|"UNKNOWN",availableFrom:string|null,allocation:number,experiences:[{id,title,domain,role,startDate,endDate,technologies:string[],description}]}`.
Requirement: `{id:string,text:string,kind:"MUST"|"PREFERRED"}`.
StaffingRequest: `{id,title,domain,description,skills:string[],requirements:Requirement[],startDate,endDate,createdAt}`.
Recommendation: `{employee:Employee,score:number,matchedRequirements:number,totalRequirements:number,evidence:[{requirementId,requirementText,status:"MATCHED"|"UNCONFIRMED",experienceId:string|null,projectTitle:string|null,quote:string,explanation:string}]}`.
Job: `{id,requestId,status:"QUEUED"|"RUNNING"|"SUCCEEDED"|"FAILED",result:Recommendation[]|null,error:string|null}`.
Shortlist: `{id,requestId,employeeId,reason,createdAt,employee:Employee}`.
Evaluation: `{mode:string,k:number,requestCount:number,hitAtK:number,recallAtK:number,evidenceAccuracy:number,cases:[{id,title,expectedIds:string[],actualIds:string[],hit:boolean,recall:number,evidenceAccuracy:number}]}`. Metrics use fraction 0..1, computed, never hardcoded success.

GET /api/auth/csrf -> {token,headerName}; POST /api/auth/login JSON {username,password} -> {username,role:"ACCOUNT"|"MANAGER"}; GET /api/auth/me same response; POST /api/auth/logout. Session + CSRF.
GET /api/employees -> Employee[]; POST /api/employees -> Employee; PUT /api/employees/{id} -> Employee (MANAGER only writes).
GET /api/requests -> StaffingRequest[]; POST /api/requests -> StaffingRequest (accept same object without id/createdAt).
POST /api/requirements/parse {domain,description,skills:string[]} -> {requirements:Requirement[],mode:string}.
POST /api/requests/{id}/analyze -> Job; GET /api/jobs/{id} -> Job.
GET /api/shortlists?requestId= -> Shortlist[]; POST /api/shortlists {requestId,employeeId,reason} -> Shortlist; DELETE /api/shortlists/{id} -> 204.
GET /api/evaluations -> Evaluation (proxy AI service).

AI internal: GET /health; POST /requirements/parse same payload; GET /evaluations same output. No browser-direct AI access. `AI_MODE=baseline` local aliases/lexical baseline explicitly labeled, `AI_MODE=local` actual local embedding + model. No external LLM calls. Worker DB jobs payload holds request + employees snapshot.

DB integration shared between API and AI worker: analysis_jobs(id varchar primary key, request_id varchar, payload jsonb NOT NULL, status varchar, result jsonb, error text, attempts int default0, lease_until timestamptz, created_at timestamptz default now(),updated_at timestamptz default now()). payload={request:StaffingRequest,employees:Employee[]}. Worker claims with FOR UPDATE SKIP LOCKED, stores Recommendation[] in result. Backend owns migration; discuss any schema changes with AI agent. Backend seed reads data/employees.json (array). AI owns shared data directory. Request ownership enforced backend; no assignment confirmation.

GET /api/requests/{id}/latest-analysis -> Job|null (empty body allowed when no job); restores existing result without re-running analysis. Job result employee status is refreshed from current DB while evidence remains from analysis time.

Employee may include assignment:{projectTitle,startDate,endDate}|null for the read-only calendar. Dates crossing JSONB/Python use ISO-8601 strings. GET /api/system/status returns internal AI health/mode for a visible baseline/local badge.
