#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${1:?Usage: scripts/restore-local.sh backups/file.dump}"
test -f "$1"
if [[ "${RESTORE_CONFIRM:-}" != "I_HAVE_A_SEPARATE_EMPTY_DATABASE" ]]; then
  echo 'Restore only into a separate empty test database. Set RESTORE_CONFIRM=I_HAVE_A_SEPARATE_EMPTY_DATABASE after checking target.' >&2
  exit 1
fi
: "${RESTORE_DATABASE:?Set a separate target database name (never staffing)}"
if [[ "$RESTORE_DATABASE" == "staffing" ]]; then echo 'Refusing default database target.' >&2; exit 1; fi
docker compose exec -T -e RESTORE_DATABASE="$RESTORE_DATABASE" postgres sh -ec 'createdb -U "$POSTGRES_USER" "$RESTORE_DATABASE"'
docker compose exec -T -e RESTORE_DATABASE="$RESTORE_DATABASE" postgres sh -ec 'pg_restore -U "$POSTGRES_USER" -d "$RESTORE_DATABASE" --no-owner --exit-on-error' < "$1"
echo 'Restored to separate database. Verify row counts, login/request/candidate workflow, and schema version before declaring recovery passed.'
