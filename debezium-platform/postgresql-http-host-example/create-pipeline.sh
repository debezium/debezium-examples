#!/usr/bin/env bash

# Copyright Debezium Authors.
#
# Licensed under the Apache License version 2.0, available at
# http://www.apache.org/licenses/LICENSE-2.0

set -euo pipefail

readonly BASE_URL="${CONDUCTOR_URL:-http://localhost:8081}/api"
readonly STATE_FILE=.host-pipeline-state

if [[ -z "${HOST_IP:-}" ]]; then
    echo "Set HOST_IP to this Linux machine's non-loopback IPv4 address." >&2
    exit 1
fi

if [[ "${HOST_IP}" == "localhost" || "${HOST_IP}" == "127.0.0.1" ]]; then
    echo "HOST_IP must be reachable from the deployed container; do not use localhost or 127.0.0.1." >&2
    exit 1
fi

./wait-for-host.sh

post_json() {
    local resource="$1"
    local payload="$2"
    curl --fail --show-error --silent \
        -X POST "${BASE_URL}/${resource}" \
        -H 'Content-Type: application/json' \
        --data "${payload}"
}

json_id() {
    python3 -c 'import json, sys; print(json.load(sys.stdin)["id"])'
}

echo "Creating the PostgreSQL connection..."
source_connection_id="$(post_json connections "$(python3 - "${HOST_IP}" <<'PY'
import json
import sys
print(json.dumps({
    "type": "POSTGRESQL",
    "name": "host-example-postgresql",
    "config": {
        "hostname": sys.argv[1], "port": 5433,
        "username": "debezium", "password": "dbz", "database": "inventory"
    }
}))
PY
)" | json_id)"

echo "Creating the HTTP connection..."
destination_connection_id="$(post_json connections "$(python3 - "${HOST_IP}" <<'PY'
import json
import sys
print(json.dumps({
    "type": "HTTP",
    "name": "host-example-http",
    "config": {"url": "http://%s:9900/" % sys.argv[1]}
}))
PY
)" | json_id)"

echo "Creating the PostgreSQL source..."
source_id="$(post_json sources "$(python3 - "${source_connection_id}" <<'PY'
import json
import sys
print(json.dumps({
    "name": "host-example-source",
    "type": "io.debezium.connector.postgresql.PostgresConnector",
    "schema": "dummy",
    "connection": {"id": int(sys.argv[1])},
    "config": {
        "plugin.name": "pgoutput", "publication.name": "dbz_host_example",
        "publication.autocreate.mode": "disabled",
        "slot.name": "dbz_host_example", "topic.prefix": "host-example",
        "schema.include.list": "inventory"
    }
}))
PY
)" | json_id)"

echo "Creating the HTTP destination..."
destination_id="$(post_json destinations "$(python3 - "${destination_connection_id}" <<'PY'
import json
import sys
print(json.dumps({
    "name": "host-example-destination",
    "type": "io.debezium.server.http.HttpChangeConsumer",
    "schema": "dummy",
    "connection": {"id": int(sys.argv[1])},
    "config": {}
}))
PY
)" | json_id)"

echo "Creating the host-based pipeline..."
pipeline_id="$(post_json pipelines "$(python3 - "${source_id}" "${destination_id}" <<'PY'
import json
import sys
print(json.dumps({
    "name": "host-example-pipeline",
    "source": {"id": int(sys.argv[1])},
    "destination": {"id": int(sys.argv[2])},
    "logLevel": "INFO"
}))
PY
)" | json_id)"

printf 'PIPELINE_ID=%s\n' "${pipeline_id}" > "${STATE_FILE}"
echo "Pipeline ${pipeline_id} was created. Waiting for the host deployment to run..."

for attempt in $(seq 1 180); do
    deployment_status="$(docker compose exec -T platform-db \
        psql -U conductor -d conductor -Atc \
        "SELECT deployment_status FROM host_deployment WHERE pipeline_id = ${pipeline_id};" 2>/dev/null || true)"
    case "${deployment_status}" in
        RUNNING)
            echo "Host deployment for pipeline ${pipeline_id} is RUNNING."
            echo "Run ./verify.sh to capture a new PostgreSQL change."
            exit 0
            ;;
        FAILED)
            echo "Host deployment for pipeline ${pipeline_id} FAILED." >&2
            exit 1
            ;;
        *)
            echo "Waiting for host deployment for pipeline ${pipeline_id} (status=${deployment_status:-not created}, ${attempt}/180)..."
            sleep 2
            ;;
    esac
done

echo "Timed out waiting for pipeline ${pipeline_id}. Inspect its logs with:" >&2
echo "  curl ${BASE_URL}/pipelines/${pipeline_id}/logs" >&2
exit 1
