#!/usr/bin/env bash
# Flip the Connection Status panel to "disconnected" and back.
#
# Kills the source database's replication connection over and over. Debezium
# reconnects by itself and the pipeline pod never restarts. The loop has to run
# for at least ~70s: Prometheus samples every 15s, and a single kill reconnects
# too quickly to ever be recorded.
#
# Usage: ./break-connection.sh [kills]
set -euo pipefail

cd "$(dirname "$0")"
source ./env.sh

KILLS=${1:-14}
if [[ ! "$KILLS" =~ ^[0-9]+$ ]]; then
  echo "Usage: $0 [kills]" >&2
  exit 1
fi

echo ">>> Dropping the replication connection every 5s, ${KILLS} times"
echo "    Watch ${HOST} -> Pipelines -> ${PIPELINE} -> Monitoring -> Connection Status"
for i in $(seq 1 "$KILLS"); do
  kubectl exec -n "$NAMESPACE" deploy/postgresql -- \
    psql -U debezium -d debezium -tAc \
    "select pg_terminate_backend(pid) from pg_stat_activity where backend_type='walsender';" \
    >/dev/null 2>&1 || true
  echo "kill ${i}/${KILLS}"
  sleep 5
done

echo ">>> Done. Connection Status should return to connected in about 30s."
echo "    The pipeline pod should not have restarted:"
kubectl get pods -n "$NAMESPACE" --no-headers | grep -E "^${PIPELINE}-" || true
