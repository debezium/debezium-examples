#!/usr/bin/env bash
# Create the PostgreSQL -> Kafka pipeline. Safe to re-run: existing names are reused.
set -euo pipefail

cd "$(dirname "$0")"
source ./env.sh

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required" >&2
  exit 1
fi

echo ">>> Waiting for the conductor API..."
ready=0
for _ in $(seq 1 60); do
  if curl -sf -o /dev/null "${HOST}/api/pipelines"; then
    ready=1
    break
  fi
  sleep 2
done
if [[ "$ready" -ne 1 ]]; then
  echo "Conductor API did not become ready at ${HOST}/api/pipelines" >&2
  exit 1
fi

id_by_name() {
  local collection="$1"
  local name="$2"
  curl -sf "${HOST}/api/${collection}" \
    | jq -r --arg name "$name" '[.[] | select(.name == $name) | .id][0] // empty'
}

require_id() {
  local label="$1"
  local id="$2"
  if [[ -z "$id" || "$id" == "null" ]]; then
    echo "Failed to resolve id for ${label}" >&2
    exit 1
  fi
}

create_named() {
  local collection="$1"
  local file="$2"
  local name="$3"
  local existing
  existing=$(id_by_name "$collection" "$name")
  if [[ -n "$existing" ]]; then
    echo "Reusing ${collection}/${name} id=${existing}" >&2
    echo "$existing"
    return
  fi
  echo "Creating ${collection}/${name}" >&2
  curl -sf -X POST "${HOST}/api/${collection}" \
    -H "Content-Type: application/json" \
    -d @"$file" | jq -r '.id'
}

DB_ID=$(create_named connections ./payloads/connection-db.json postgres-connection)
require_id postgres-connection "$DB_ID"
KAFKA_ID=$(create_named connections ./payloads/connection-kafka.json kafka-connection)
require_id kafka-connection "$KAFKA_ID"

SOURCE_FILE=$(mktemp)
DEST_FILE=$(mktemp)
PIPELINE_FILE=$(mktemp)
trap 'rm -f "$SOURCE_FILE" "$DEST_FILE" "$PIPELINE_FILE"' EXIT

jq --argjson id "$DB_ID" '.connection.id = $id' ./payloads/source.json >"$SOURCE_FILE"
jq --argjson id "$KAFKA_ID" '.connection.id = $id' ./payloads/destination.json >"$DEST_FILE"

SOURCE_ID=$(create_named sources "$SOURCE_FILE" test-source)
require_id test-source "$SOURCE_ID"
DEST_ID=$(create_named destinations "$DEST_FILE" test-destination)
require_id test-destination "$DEST_ID"

PIPELINE_ID=$(id_by_name pipelines "$PIPELINE")
if [[ -z "$PIPELINE_ID" ]]; then
  echo "Creating pipeline ${PIPELINE}"
  jq --argjson source "$SOURCE_ID" --argjson destination "$DEST_ID" \
    '.source.id = $source | .destination.id = $destination' ./payloads/pipeline.json >"$PIPELINE_FILE"
  PIPELINE_ID=$(curl -sf -X POST "${HOST}/api/pipelines" \
    -H "Content-Type: application/json" \
    -d @"$PIPELINE_FILE" | jq -r '.id')
else
  echo "Reusing pipeline ${PIPELINE} id=${PIPELINE_ID}"
fi
require_id "$PIPELINE" "$PIPELINE_ID"

echo ">>> Waiting for the pipeline deployment..."
kubectl rollout status "deploy/${PIPELINE}" -n "$NAMESPACE" --timeout="$TIMEOUT"

echo ">>> Pipeline is running"
echo "Open ${HOST}/pipeline/${PIPELINE_ID}/monitoring"
echo "Next: ./verify-monitoring.sh"
