import json
from pathlib import Path
from fastapi.testclient import TestClient
from app import config
from app.main import app
from app.engine import canonical_terms, supports, recommend, parse_requirements, evidence_supported
from app.schemas import Employee, StaffingRequest, ParseRequest
from app.evaluation import evaluate

DATA = Path(__file__).resolve().parents[3] / 'data'
def employees():
    return [Employee.model_validate(e) for e in json.loads((DATA/'employees.json').read_text())]
def request(requirements):
    return StaffingRequest(id='t',title='test',domain='제조',description='MES 구축 Java 개발',requirements=[{'id':f'r{i}', 'text':t, 'kind':'MUST'} for i,t in enumerate(requirements)])

def test_alias_matches_manufacturing_execution_system():
    assert 'mes' in canonical_terms('MES') and 'mes' in canonical_terms('제조실행시스템')

def test_java_does_not_match_javascript():
    assert 'java' not in canonical_terms('JavaScript')

def test_pm_cannot_claim_java_development():
    pm=next(e for e in employees() if e.id=='E003')
    assert not supports(pm.experiences[0].model_dump(),'Java 개발 경험')

def test_person_level_ranking_uses_exact_experience_quote():
    ranked=recommend(request(['MES 경험','Java 개발 경험']),employees(),'baseline')
    assert {r['employee']['id'] for r in ranked[:3]}=={'E001','E002','E013'}
    for r in ranked:
        for evidence in r['evidence']:
            if evidence['status']=='MATCHED':
                assert evidence_supported(evidence,r['employee'])

def test_absence_is_unconfirmed_and_unknown_stays_unknown():
    ranked=recommend(request(['Rust 개발 경험']),employees(),'baseline')
    assert all(r['evidence'][0]['status']=='UNCONFIRMED' for r in ranked)
    assert any(r['employee']['availability']=='UNKNOWN' for r in ranked)

def test_duplicate_experience_does_not_duplicate_people_or_score():
    e=employees()[0];r=request(['MES 경험'])
    original=recommend(r,[e],'baseline')[0]
    duplicate=recommend(r,[e.model_copy(update={'experiences':e.experiences*2})],'baseline')
    assert len(duplicate)==1 and duplicate[0]['score']==original['score']

def test_forged_quote_is_not_valid_evidence():
    r=recommend(request(['MES 경험']),employees(),'baseline')[0]
    e=r['evidence'][0]|{'quote':'존재하지 않는 경력'}
    assert not evidence_supported(e,r['employee'])

def test_gold_ids_do_not_enter_recommender(monkeypatch):
    import app.evaluation as module
    real=module.recommend
    def check(request, people, mode):
        assert 'expectedIds' not in request.model_dump()
        assert 'expectedEvidence' not in request.model_dump()
        return real(request,people,mode)
    monkeypatch.setattr(module,'recommend',check)
    result=module.evaluate('baseline',3)
    assert result['requestCount']==10
    assert all(0<=result[k]<=1 for k in ['hitAtK','recallAtK','evidenceAccuracy'])
    assert all('actualIds' in c for c in result['cases'])

def test_parse_returns_editable_requirements():
    res=parse_requirements(ParseRequest(domain='제조',description='MES 구축 Java 개발',skills=['Java','MES']),'baseline')
    assert res['mode']=='baseline-alias-lexical'
    assert all(r['id'] and r['kind'] in ('MUST','PREFERRED') for r in res['requirements'])

def test_local_failure_never_silently_falls_back(monkeypatch):
    import app.engine as engine
    def fail(*_):raise RuntimeError('local unavailable')
    monkeypatch.setattr(engine,'semantic_scores',fail)
    import pytest
    with pytest.raises(RuntimeError): engine.recommend(request(['MES']),employees(),'local')

def test_internal_http_contract():
    client=TestClient(app)
    assert client.get('/health').status_code==200
    res=client.post('/requirements/parse',json={'domain':'제조','description':'MES 구축','skills':['Java']})
    assert res.status_code==200 and 'requirements' in res.json()
    assert client.post('/requirements/parse',json={'description':''}).status_code==422

def test_bare_java_requirement_does_not_give_pm_development_credit():
    pm=next(e for e in employees() if e.id=='E003')
    assert not supports(pm.experiences[0].model_dump(),'Java 경험')

def test_explicit_preferred_requirement_is_editable_preferred():
    payload=ParseRequest(domain='제조',description='Java 개발 경험은 필수이며 Spring 경험을 우대합니다.',skills=['Java'])
    res=parse_requirements(payload,'baseline')['requirements']
    assert next(r for r in res if 'Spring' in r['text'])['kind']=='PREFERRED'
    assert next(r for r in res if 'Java' in r['text'])['kind']=='MUST'
