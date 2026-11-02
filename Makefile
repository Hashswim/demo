.PHONY: help setup dev up down local-model test web-test api-test ai-test lint-infra backup
help:
	@echo "setup: install deps + .env | dev: frontend only | up: baseline compose | local-model: CPU local models | test: code checks"
setup:
	./scripts/setup.sh
dev:
	cd apps/web && npm run dev
up:
	docker compose up --build -d
down:
	docker compose down
local-model:
	AI_MODE=local INSTALL_LOCAL_MODELS=true docker compose --profile local-model up --build -d
test: web-test ai-test api-test
web-test:
	cd apps/web && npm run build && npm test
ai-test:
	cd services/ai && .venv/bin/python -m pytest
api-test:
	cd apps/api && ./gradlew test
lint-infra:
	docker compose --env-file .env.example config --quiet
	helm lint infra/helm/staffing --set secrets.existingSecret=lint-secrets --set web.tlsSecret=lint-tls
	terraform fmt -check -recursive infra/terraform
backup:
	./scripts/backup-local.sh
