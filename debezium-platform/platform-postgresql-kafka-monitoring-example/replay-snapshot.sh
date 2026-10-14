#!/usr/bin/env bash
# Make the Snapshot panels move again.
#
# A snapshot is the one-time initial copy of the captured tables, and it already
# finished when the pipeline was created. Debezium records how far it got in an
# offset table in the platform's own database (the `postgres` deployment, not the
# source `postgresql` one). Clearing that table and restarting the pipeline leaves
# it with no memory of its position, so it snapshots from scratch.
#
# This is also the fix if the pipeline ever crash-loops on a stale WAL position.
set -euo pipefail

cd "$(dirname "$0")"
source ./env.sh

OFFSET_TABLE="${PIPELINE//-/_}_offset"

echo ">>> Clearing ${OFFSET_TABLE} in the platform database..."
if ! kubectl exec -n "$NAMESPACE" deploy/postgres -- \
  sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "DELETE FROM '"${OFFSET_TABLE}"';"'; then
  echo "Could not clear ${OFFSET_TABLE}. Is ${PIPELINE} the pipeline you created?" >&2
  exit 1
fi

echo ">>> Restarting ${PIPELINE}..."
kubectl rollout restart "deploy/${PIPELINE}" -n "$NAMESPACE"
kubectl rollout status "deploy/${PIPELINE}" -n "$NAMESPACE" --timeout="$TIMEOUT"

echo ">>> Snapshot is running again. Open the Snapshot section now, it finishes fast:"
echo "    ${HOST} -> Pipelines -> ${PIPELINE} -> Monitoring -> Snapshot"
