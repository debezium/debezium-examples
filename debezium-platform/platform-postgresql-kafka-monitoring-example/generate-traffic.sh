#!/usr/bin/env bash
# Steady inserts so the Streaming Event Count chart climbs and holds.
# Usage: ./generate-traffic.sh [batches] [rows-per-batch]
set -euo pipefail

cd "$(dirname "$0")"
source ./env.sh

BATCHES=${1:-60}
ROWS=${2:-20}
if [[ ! "$BATCHES" =~ ^[0-9]+$ || ! "$ROWS" =~ ^[0-9]+$ ]]; then
  echo "Usage: $0 [batches] [rows-per-batch]" >&2
  exit 1
fi

echo ">>> Inserting ${ROWS} products every 5s, ${BATCHES} times"
echo "    Watch ${HOST} → pipeline ${PIPELINE} → Monitoring → Streaming Event Count Rate"
for i in $(seq 1 "$BATCHES"); do
  kubectl exec -n "$NAMESPACE" deploy/postgresql -- \
    psql -U debezium -d debezium -q -c \
    "INSERT INTO inventory.products (name, description, weight) SELECT 'tick-${i}-'||g, 'tick', g FROM generate_series(1, ${ROWS}) g;"
  echo "batch ${i} sent"
  sleep 5
done
