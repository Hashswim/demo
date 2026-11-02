#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p backups
file="backups/staffing-$(date -u +%Y%m%dT%H%M%SZ).dump"
docker compose exec -T postgres sh -ec 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc --no-owner' > "$file"
# Catalog validity is not a full restore drill.
docker compose exec -T postgres pg_restore --list < "$file" >/dev/null
printf 'Archive saved and catalog checked: %s\n' "$file"
