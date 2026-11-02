#!/usr/bin/env python3
"""Load local env as data (never shell-source passwords), launch one dev service."""
from pathlib import Path
import os,subprocess,sys
from urllib.parse import quote
ROOT=Path(__file__).resolve().parents[1]
values={}
for line in (ROOT/'.env').read_text().splitlines():
    if '=' in line and not line.lstrip().startswith('#'):
        key,value=line.split('=',1);values[key.strip()]=value.strip()
user=values.get('POSTGRES_USER','staffing');db=values.get('POSTGRES_DB','staffing');password=values['POSTGRES_PASSWORD']
values.update(SPRING_DATASOURCE_URL=f'jdbc:postgresql://127.0.0.1:5432/{db}',
              SPRING_DATASOURCE_USERNAME=user,SPRING_DATASOURCE_PASSWORD=password,
              DATABASE_URL=f'postgresql://{quote(user,safe="")}:{quote(password,safe="")}@127.0.0.1:5432/{db}',
              AI_BASE_URL='http://127.0.0.1:8000',DATA_DIR=str(ROOT/'data'),
              APP_DEMO_ENABLED='true',APP_DEMO_DATA_PATH=str(ROOT/'data/employees.json'),SESSION_COOKIE_SECURE='false')
mode=sys.argv[1] if len(sys.argv)>1 else 'prepare'
if mode=='prepare':
    target=ROOT/'.env.local-dev';target.write_text('\n'.join(f'{k}={v}' for k,v in values.items())+'\n');target.chmod(0o600)
    print('Local debug environment prepared (ignored by Git).');sys.exit(0)
python=ROOT/'services/ai/.venv/bin/python'
commands={'api':([str(ROOT/'apps/api/gradlew'),'bootRun'],ROOT/'apps/api'),
          'ai':([str(python),'-m','uvicorn','app.main:app','--host','127.0.0.1','--port','8000','--reload'],ROOT/'services/ai'),
          'worker':([str(python),'-m','app.worker'],ROOT/'services/ai')}
if mode not in commands:raise SystemExit('Usage: run-local.py prepare|api|ai|worker')
cmd,cwd=commands[mode];raise SystemExit(subprocess.call(cmd,cwd=cwd,env={**os.environ,**values}))
