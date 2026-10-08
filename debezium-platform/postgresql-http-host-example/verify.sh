#!/usr/bin/env bash

# Copyright Debezium Authors.
#
# Licensed under the Apache License version 2.0, available at
# http://www.apache.org/licenses/LICENSE-2.0

set -euo pipefail

readonly STATE_FILE=.host-pipeline-state

if [[ ! -f "${STATE_FILE}" ]]; then
    echo "Run ./create-pipeline.sh first." >&2
    exit 1
fi

event_id="host-example-$(date +%s)"
email="${event_id}@example.com"

echo "Inserting a source row with ${email}..."
docker compose exec -T -e PGPASSWORD=postgres source-postgres \
    psql -h 127.0.0.1 -p 5433 -U postgres -d inventory -v ON_ERROR_STOP=1 -c \
    "INSERT INTO inventory.customers (first_name, last_name, email) VALUES ('Host', 'Example', '${email}');"

for attempt in $(seq 1 30); do
    if docker compose exec -T receiver sh -c "test -f /events/events.ndjson && grep -F '${email}' /events/events.ndjson"; then
        echo "The HTTP receiver recorded the change. The host-based pipeline is working."
        exit 0
    fi
    echo "Waiting for the HTTP event (${attempt}/30)..."
    sleep 2
done

echo "The HTTP receiver did not receive ${email}. Inspect these logs:" >&2
echo "  docker compose logs receiver conductor" >&2
exit 1
