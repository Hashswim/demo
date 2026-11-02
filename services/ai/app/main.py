from fastapi import FastAPI, HTTPException, Query
from . import config
from .schemas import ParseRequest
from .engine import MODE_LABELS, parse_requirements
from .evaluation import evaluate

app = FastAPI(title="Staffing AI internal API", version="0.1.0",
              description="Internal only. Baseline is aliases/lexical; local uses self-hosted models.")


@app.get("/health")
def health():
    return {"status": "ok", "mode": MODE_LABELS[config.MODE]}


@app.post("/requirements/parse")
def parse(payload: ParseRequest):
    try:
        return parse_requirements(payload)
    except Exception:
        # Raw prompts, employee records, filesystem paths and model exceptions
        # are deliberately not returned to the browser or public log.
        raise HTTPException(status_code=503, detail="요건 정리 모델을 사용할 수 없습니다. 모델 설정을 확인하세요.")


@app.get("/evaluations")
def evaluations(k: int = Query(default=3, ge=1, le=10)):
    try:
        return evaluate(k=k)
    except Exception:
        raise HTTPException(status_code=503, detail="평가를 실행할 수 없습니다. 데이터와 로컬 모델 설정을 확인하세요.")
