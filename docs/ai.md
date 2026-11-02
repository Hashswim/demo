# AI 서비스와 검증

FastAPI는 내부 API이며 브라우저는 Spring Boot만 호출한다. Worker는 분석 작업을 PostgreSQL에서 선점하고 lease heartbeat와 시도 번호로 오래된 Worker의 완료 쓰기를 차단한다. DB 장애 시 재시도한다. 현재 개발 편의를 위해 DB 자격증명을 공유하는 Compose 구성이고, 실제 배포 전 목적별 계정 분리가 필요하다.

## 모드

- baseline: 별칭·텍스트 지지 검사, 사람별 요건 가중치(필수2/우대1). 외부·로컬 LLM을 호출하지 않는다. 자유 문장의 모든 의미를 이해하지 못하므로 요건 직접 수정을 제공한다.
- local: 로컬 sentence-transformers 임베딩 → PostgreSQL pgvector 정확 검색/벡터 캐시 → 사람별 근거 커버리지와 의미 유사도 결합 → 상위 후보의 근거 설명에 로컬 llama.cpp 호출. 모델 실패 시 자동 기준선 대체 없음.

Qwen3-Embedding-0.6B의 1024차원을 현재 캐시 스키마에 사용한다. 모델 교체 시 EMBEDDING_MODEL_REVISION과 차원 마이그레이션을 관리한다. 모델 파일은 자동 다운로드하지 않도록 LOCAL_FILES_ONLY=true를 기본으로 둔다. 생성 서버 호스트도 허용된 로컬 서비스로 제한한다. Qwen GGUF를 models/qwen.gguf, 임베딩 모델을 models/embedding에 넣고 README의 local-model 프로필을 실행한다. 실제 모델 다운로드/추론 검증은 수행 전이다.

## 추천과 안전한 설명

기술 별칭·정확 단어 경계(Java와 JavaScript 구분), 개발 경험과 PM 역할을 구분한다. 프로젝트 전체 기술 목록만으로 개인이 개발했다고 확정하지 않는다. 근거 없음은 UNCONFIRMED. 투입 상태는 역량 점수에 숨겨 넣지 않고 화면에서 필터링한다. 현재 비투입을 미래 가용으로 단정하지 않는다.

근거에는 requirementId, experienceId, projectTitle, 원문 quote를 고정한다. local 설명은 원문 ID와 quote가 같은 경우만 수용한다. 설명에 없는 사실이 없는지는 별도 사람 평가가 필요하다. 현재 의미 검색은 후보 점수 보조이며, 자동 확정 근거는 보수적인 지지 검사에 의존한다. 표현이 매우 다른 한국어 경험은 추가 평가·검색 개선이 필요하다.

## 평가

data/evaluation_requests.json의 expectedIds와 독립적으로 작성한 expectedEvidence를 평가에서만 읽는다. Hit@K는 정답을 하나 이상 찾은 요청 비율, Recall@K는 요청별 정답 인력 회수율의 평균이다. 근거 일치 비율은 상위K 결과의 MATCHED 주장 중 정확 원문·ID와 정답 근거 라벨이 일치한 비율이다. 생성 설명 문장 자체의 의미 정확도나 실고객 데이터 성능을 보장하지 않는다. 가상 정답은 팀원이 검토하고 별도 검증 세트를 확대해야 한다.

CPU 컨테이너는 llama.cpp 공식 `server` 태그를 사용한다. 모델 파일과 이미지 digest를 검증한 뒤 고정해야 한다. 공식 배포 안내: https://github.com/ggml-org/llama.cpp/blob/master/docs/docker.md
