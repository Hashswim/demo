#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose --env-file .env.example config --quiet
bash -n scripts/*.sh
if command -v helm >/dev/null; then helm lint infra/helm/staffing --set secrets.existingSecret=check-secret --set web.tlsSecret=check-tls; fi
if command -v terraform >/dev/null; then terraform fmt -check -recursive infra/terraform; fi
