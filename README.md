# FIND · 프로젝트 인력 자동 추천

Vue · Spring Boot · FastAPI · Qwen · PostgreSQL/pgvector · EKS 기반 개발 시작점입니다. 가상 인력으로 요청 입력 → 필수/우대 요건 수정 → 추천 근거·투입 현황 확인 → 후보와 선택 이유 저장을 연결합니다.

## VS Code에서 시작

`staffing-platform.code-workspace`를 VS Code로 여세요. 추천 확장과 Web/API/AI/Worker 실행·검사 작업이 들어 있습니다. Java 21, Node.js 22 이상, Python **3.11**, Docker Desktop을 사용합니다. Python 3.14는 이 프로젝트의 검증 대상이 아닙니다.

### 필요 환경

| 도구 | 버전 | 용도 |
|---|---|---|
| Python | 3.11.x (`.python-version`) | AI API·Worker, 스크립트 |
| JDK | 21 | Spring Boot API (Gradle은 `./gradlew`가 받음) |
| Node.js | 22 이상 (`.nvmrc`) | Vue 웹 |
| Docker Desktop | Compose v2 | 전체 서비스 실행 |
| Helm / Terraform | 3.17+ / 1.10+ | 선택: 인프라 검사·배포 |

Python 패키지는 루트 `requirements.txt`(→ `services/ai/requirements.txt`)에 있고, 테스트 도구(pytest)를 포함한 개발용은 `requirements-dev.txt`, 로컬 모델 모드용은 `requirements-local.txt`입니다. 아래 한 줄로 도구 확인, venv·npm 설치, 무작위 비밀번호가 든 `.env` 생성(기존 `.env`는 유지)까지 끝납니다.

```sh
make setup        # = ./scripts/setup.sh
```

### 화면만 먼저 보기

```sh
cd apps/web
npm ci
npm run dev
```

http://127.0.0.1:5173 에서 Account 또는 인력 정보 관리자 역할을 선택합니다. `.env.development`의 `VITE_API_MODE=demo`는 브라우저 미리보기이며, 데이터는 해당 브라우저 localStorage에만 저장됩니다. 별칭·키워드 검색을 사용하며 실제 LLM·의미 검색으로 표시하지 않습니다. 이 모드에서 평가 성능 수치는 임의로 만들지 않습니다.

### 전체 서비스 실행

```sh
# 최초 준비: .env가 없을 때만 복사합니다. 기존 .env는 덮어쓰지 마세요.
cp -n .env.example .env
# .env의 DB 비밀번호와 Account/관리자 비밀번호를 각각 12자 이상으로 변경
# 이번 작업 환경에는 무작위 로컬 비밀번호를 넣은 .env가 이미 생성되어 있습니다.
docker compose up --build -d
```

http://127.0.0.1:8080 로 접속합니다. `account` / `manager` 계정 비밀번호는 로컬 `.env`의 `APP_DEMO_ACCOUNT_PASSWORD` / `APP_DEMO_MANAGER_PASSWORD`입니다. 이 파일은 Git 제외 대상입니다. 최초 DB 생성 때만 계정을 seed하므로 나중에 환경 변수만 바꿔도 기존 DB의 비밀번호가 변경되지는 않습니다.

전체 서비스의 기본 `AI_MODE=baseline`은 로컬 별칭·텍스트 기준선입니다. 실제 DB·로그인·권한·Worker·평가는 동작하지만 Qwen이 실행된다는 뜻은 아닙니다. Docker UI를 쓰려면 별도 모델 다운로드와 설정이 필요하지 않습니다.

### 실제 Qwen / 의미 검색 연결

검증한 Qwen GGUF 모델을 `models/qwen.gguf`, Qwen3-Embedding-0.6B 모델·토크나이저를 `models/embedding/`에 준비하세요. 리비전·라이선스·파일 체크섬을 기록하고, `.env`의 모드를 바꿉니다.

```sh
AI_MODE=local INSTALL_LOCAL_MODELS=true docker compose --profile local-model up --build -d
```

모델은 CPU에서 실행합니다. 모델이 없거나 실패하면 오류를 반환하며 baseline으로 자동 대체하지 않습니다. 외부 LLM API는 호출하지 않습니다. 임베딩 모델을 바꾸면 차원과 캐시 스키마·리비전도 함께 관리해야 합니다. 실제 Qwen 추론 성능은 아직 검증하지 않았습니다.

### VS Code에서 서비스별 디버깅

전체 Compose와 직접 실행은 포트가 겹칩니다. 전체 Compose를 `docker compose down`으로 중지한 뒤, `Compose: dependencies` 작업으로 DB·AI 의존 서비스만 시작하세요. `API: bootRun`, `AI: Worker`, `Web: dev` 작업을 실행하면 됩니다. AI 자체를 디버깅할 때는 Compose의 ai를 먼저 중지하고 `AI: API`를 실행합니다. Java/Python 디버그 설정은 `Prepare: local env` 작업으로 만든 Git 제외 파일을 사용합니다.

Vue 개발 서버에서 실제 API를 확인하려면 `VITE_API_MODE=live npm run dev`로 실행하세요. DB 포트를 노출하는 compose.dev.yaml은 로컬 디버깅에만 사용합니다.

## 구조

```text
apps/web/                  Vue + TypeScript + Vite / NGINX
apps/api/                  Java21 / Spring Boot / Security / JPA / Flyway
services/ai/               FastAPI / Python Worker / 로컬 추론 어댑터
  app/engine.py            별칭 기준선 · 임베딩/pgvector 검색 · 사람별 순위
  app/worker.py            PostgreSQL 작업 선점 / lease / 재시도
  tests/                   검색·근거·평가 계약 테스트
data/                      가상 인력 16명 / 평가 요청 10건 / 근거 정답
infra/helm/staffing/        EKS 워크로드 · DB · 모델 · 백업 템플릿
infra/terraform/           runtime(EKS) / persistent(S3·ECR) 분리
scripts/                   통합 검사 · 백업/복원 · 환경 종료 체크
.github/workflows/         검사 / 수동 ECR 이미지 발행
.vscode/                   실행 작업 / 디버그 / 확장 추천
models/                    모델 파일 직접 준비, Git 제외
```

## 기능

- 프로젝트 내용과 필요 기술 입력, 필수·우대 요건 초안 및 직접 편집.
- 유사 경험의 사람별 추천 순위, 요건별 프로젝트 원문·이력 ID, 미확인 표시.
- 현재 투입 상태와 투입 가능 필터, 조회용 월별 투입 일정과 현황 표.
- 프로젝트별 후보 및 선택 이유 저장·삭제.
- 인력 관리자만 프로필·참여 역할/기간/기술·투입 현황 등록·수정.
- 정답 요청의 Hit@K, Recall@K, 근거 일치 검증.

담당자별 요청·후보의 접근 범위는 API에서 검증합니다. 관리자가 Account 업무를 대신 수행하지 않습니다. 추천은 배정 확정이 아닙니다.

## 검사

```sh
cd apps/web && npm run build && npm test
cd services/ai && python3.11 -m venv .venv
.venv/bin/python -m pip install -r requirements-dev.txt
.venv/bin/python -m pytest
cd apps/api && ./gradlew test bootJar
# 프로젝트 루트, Compose 실행 후
python3 scripts/smoke-api.py
```

디렉터리는 각 단계에 맞게 이동하세요. 통합 검사는 가상 요청을 저장하며 실제 인력이나 외부 서비스에 접근하지 않습니다. 테스트 현황은 `docs/verification.md`에 기록합니다.

## 다음 개발 순서

`docs/development-order.md`, `docs/requirements.md`, `docs/operations.md`, `docs/ai.md`, `docs/contracts.md`를 읽고 진행합니다. EKS에 배포하기 전 AWS 견적, TLS 인증서, IAM, 모델 성능, 백업 복구를 검증해야 합니다. Terraform apply나 AWS 리소스 생성은 이번 작업에서 실행하지 않았습니다.

월 $100은 운영 목표입니다. 기본안은 클러스터와 워커를 필요한 시간에만 운영하고 검증된 백업 후 삭제하는 방식입니다. EKS를 유지하고 노드만 0대로 줄이면 클러스터 요금은 계속 발생합니다. Budgets는 실시간 결제 차단 장치가 아닙니다.
