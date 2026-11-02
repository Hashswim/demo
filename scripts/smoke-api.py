#!/usr/bin/env python3
"""Exercise local synthetic-data workflow without printing credentials or records."""
import http.cookiejar,json,time,urllib.request,urllib.error
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
env={}
for line in (ROOT/'.env').read_text().splitlines():
 if '=' in line and not line.startswith('#'):k,v=line.split('=',1);env[k]=v
base='http://127.0.0.1:'+env.get('WEB_PORT','8080')+'/api'
def client():return urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
def call(c,path,method='GET',body=None,token=None):
 headers={'Content-Type':'application/json'}
 if token:headers[token['headerName']]=token['token']
 req=urllib.request.Request(base+path,data=json.dumps(body).encode() if body is not None else None,headers=headers,method=method)
 with c.open(req,timeout=150) as r:
  text=r.read();return json.loads(text) if text else None
def login(c,name,password):
 csrf=call(c,'/auth/csrf');identity=call(c,'/auth/login','POST',{'username':name,'password':password},csrf)
 return identity,call(c,'/auth/csrf')
c=client();identity,csrf=login(c,'account',env['APP_DEMO_ACCOUNT_PASSWORD']);assert identity['role']=='ACCOUNT'
people=call(c,'/employees');assert len(people)>=16
parsed=call(c,'/requirements/parse','POST',{'domain':'제조','description':'MES 구축 Java 개발 경험 필요','skills':['MES','Java']},csrf)
r=call(c,'/requests','POST',{'title':'통합 검증: MES Java 인력 추천','domain':'제조','description':'MES 구축 Java 개발 경험 필요','skills':['MES','Java'],'requirements':parsed['requirements'],'startDate':'2026-12-01','endDate':'2027-03-31'},csrf)
j=call(c,'/requests/'+r['id']+'/analyze','POST',{},csrf)
for _ in range(30):
 j=call(c,'/jobs/'+j['id'])
 if j['status'] in ['SUCCEEDED','FAILED']:break
 time.sleep(1)
assert j['status']=='SUCCEEDED', j['error']
assert {x['employee']['id'] for x in j['result'][:3]}=={'E001','E002','E013'}
s=call(c,'/shortlists','POST',{'requestId':r['id'],'employeeId':j['result'][0]['employee']['id'],'reason':'통합 검증: 이력의 MES Java 개발 근거를 확인함'},csrf)
assert any(x['id']==s['id'] for x in call(c,'/shortlists'))
call(c,'/shortlists/'+s['id'],'DELETE',token=csrf)
eval=call(c,'/evaluations');assert eval['requestCount']==10
m=client();manager,mc=login(m,'manager',env['APP_DEMO_MANAGER_PASSWORD']);assert manager['role']=='MANAGER'
try:
 call(c,'/employees/'+people[0]['id'],'PUT',people[0],csrf)
 raise AssertionError('ACCOUNT profile edit was allowed')
except urllib.error.HTTPError as e:assert e.code==403
p=people[0];original=p['availability'];updated={**p,'availability':'UNKNOWN','allocation':0}
call(m,'/employees/'+p['id'],'PUT',updated,mc)
fresh=call(c,'/jobs/'+j['id']);assert next(x for x in fresh['result'] if x['employee']['id']==p['id'])['employee']['availability']=='UNKNOWN'
call(m,'/employees/'+p['id'],'PUT',p,mc)
call(c,'/auth/logout','POST',{},csrf)
try:call(c,'/auth/me');raise AssertionError('logout did not clear session')
except urllib.error.HTTPError as e:assert e.code==401
print('PASS: login/CSRF, 16 profiles, request, durable job, top-3 evidence, shortlist, evaluation, RBAC, fresh availability, logout')
print('Synthetic evaluation:',json.dumps({k:eval[k] for k in ['mode','requestCount','hitAtK','recallAtK','evidenceAccuracy']}))
