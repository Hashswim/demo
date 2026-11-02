# 검증 기록 · 2026-11-02

## 통과

| 범위 | 확인 내용 |
|---|---|
| Vue | TypeScript 검사와 프로덕션 빌드 통과, Vitest 4개 통과 |
| Spring Boot | Gradle test/bootJar 통과, 인증·권한·소유권·최신 상태 반영 테스트 9개 |
| FastAPI/추천 | Python 3.11 pytest 13개 통과(`requirements-dev.txt`, 런타임 이미지에는 pytest 미포함), 별칭·역할·미확인·원문·평가 분리 확인 |
| 컨테이너 | 웹·API·AI·Worker·PostgreSQL 5개 서비스 로컬 실행 |
| API 통합 | 로그인·CSRF 갱신, 16개 프로필, 요건 파싱, 요청 저장, durable 작업 완료, 상위 인력, 후보 저장/삭제, 평가, 역할 제한, 현재 투입 상태 갱신, 로그아웃 통과 |
| 화면 | 브라우저에서 Account/관리자 역할 전환, 요건 수정과 추천, 후보 이유 저장, 월별 달력, 프로필 수정·저장 확인 |
| 화면 개편(live) | Compose 실제 API로 두 역할 로그인, 요청 작성→요건 정리→추천 완료, 후보 저장·삭제, 인력 수정 저장, 역할별 접근 제한(다른 역할 메뉴 주소 → 대시보드), 1440px·390px 화면 캡처 확인 |
| 레이아웃 | 데스크톱과 390px 너비 브라우저 에뮬레이션에서 확인. 모바일 실기기 검증은 미수행 |
| DB 백업 | 로컬 dump 생성, 별도 DB staffing_restore_check_20261102 복원, 인력 16행 확인. 복원된 DB를 사용하는 전체 앱 전환 시험은 미수행 |
| Helm | lint 및 기본/로컬 모델·백업 활성화 template 렌더링 통과 |
| Terraform | runtime / persistent 두 스택 init -backend=false + validate 통과, fmt 통과. plan/apply 미수행 |
| 설정 | Compose config, shell 문법, VS Code JSON 확인 |

실행한 API 통합 검사는 `scripts/smoke-api.py`로 다시 확인할 수 있다. 가상 검증 요청이 DB에 남는다. pytest에서 Starlette/AnyIO deprecation 경고 1개가 있으나 테스트 실패는 없다.

## 성능 수치 해석

16개 가상 프로필과 10개 소규모 정답 요청을 사용했다. 기준선은 이 합성 세트에서 Hit@3/Recall@3/라벨 기반 근거 일치가 모두 1.0으로 계산됐다. 이는 로컬 Qwen 성능·일반화 성능·실제 고객사 자료에 대한 정확도가 아니다. 팀원 검토와 별도의 더 어려운 holdout 세트가 필요하다. 브라우저 미리보기에서는 임의의 성능 수치를 표시하지 않는다.

## 남은 구현·검증

- 실제 Qwen GGUF와 임베딩 파일 준비, 로컬 모드 실행, 모델 품질/한국어 표현 차이·메모리·지연 측정.
- EKS 실배포, TLS·도메인·IAM·플러그인·백업 S3 연결, 실제 월 비용 검증.
- Fluent Bit 수집기·최소권한 역할 배포. 예시 ConfigMap은 제공하지만 자동 설치되지 않는다.
- 감사 이벤트 실제 적재, 운영 계정 관리, 목적별 DB 계정, 문서 변경에 대한 버전 경고.
- API 요건 정리와 Worker 추론을 아우르는 단일 추론 큐/동시성 제어. 현재 Worker 1개와 모델 서버 자원 제한이 기본이다.
- 백업 복원 후 앱 전체 전환·운영 중단·오래 걸리는 작업 lease 회수의 장애 주입 시험.
- 외부 사내 HR 연동·배정 확정·급여·청구·자격증은 현재 범위가 아니다.

## 미리보기

- dashboard-preview.jpg: 브라우저 데모 대시보드
- recommendation-preview.jpg: 근거를 확인하는 추천 결과
- allocation-preview.jpg: 조회용 월별 투입 일정

화면 캡처는 가상 데이터 미리보기다. 로그인·DB 통합 테스트는 별도로 실제 로컬 서비스에서 수행했다.
