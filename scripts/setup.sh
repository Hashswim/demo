#!/usr/bin/env bash
# One-shot local setup: tool check, Python venv, npm deps, and a .env with random passwords.
set -euo pipefail
cd "$(dirname "$0")/.."

need() { command -v "$1" >/dev/null || { echo "필수 도구가 없습니다: $1 ($2)" >&2; exit 1; }; }
need python3.11 "Python 3.11"
need java "JDK 21"
need node "Node.js 22+"
need npm "Node.js 22+"
java -version 2>&1 | grep -qE 'version "21' || echo "경고: JDK 21이 아닙니다. Gradle toolchain이 21을 찾지 못하면 빌드가 실패합니다." >&2
[ "$(node -p 'process.versions.node.split(".")[0]')" -ge 22 ] || { echo "Node.js 22 이상이 필요합니다." >&2; exit 1; }
command -v docker >/dev/null || echo "참고: Docker가 없어 전체 서비스(docker compose)는 실행할 수 없습니다." >&2

if [ ! -f .env ]; then
  python3.11 - <<'PY'
import secrets, pathlib
text = pathlib.Path(".env.example").read_text()
for key in ("CHANGE_ME_LOCAL_DB_PASSWORD", "CHANGE_ME_LOCAL_ACCOUNT_PASSWORD", "CHANGE_ME_LOCAL_MANAGER_PASSWORD"):
    text = text.replace(key, secrets.token_urlsafe(18))
target = pathlib.Path(".env")
target.write_text(text)
target.chmod(0o600)
PY
  echo ".env 생성: 무작위 비밀번호를 넣었습니다. 로그인 비밀번호는 .env의 APP_DEMO_*_PASSWORD 입니다."
else
  echo ".env가 이미 있어 그대로 둡니다."
fi

[ -x services/ai/.venv/bin/python ] || python3.11 -m venv services/ai/.venv
services/ai/.venv/bin/python -m pip install --upgrade pip -q
services/ai/.venv/bin/python -m pip install -r requirements-dev.txt -q
(cd apps/web && npm ci)
python3.11 scripts/run-local.py prepare
echo "준비 완료. 'make up'(전체 Docker) 또는 'make dev'(화면만)로 시작하세요."
