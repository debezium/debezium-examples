#!/usr/bin/env bash

# Copyright Debezium Authors.
#
# Licensed under the Apache License version 2.0, available at
# http://www.apache.org/licenses/LICENSE-2.0

set -euo pipefail

# Host provisioning can install packages and pull the initial Debezium Server
# image. Match the Conductor default Ansible timeout of 30 minutes rather than
# reporting a misleading client-side timeout during a cold first run.
readonly MAX_ATTEMPTS=900

for attempt in $(seq 1 "${MAX_ATTEMPTS}"); do
    status="$(docker compose exec -T platform-db \
        psql -U conductor -d conductor -Atc \
        "SELECT provisioning_status FROM host_status WHERE ssh_alias = 'localhost';" 2>/dev/null || true)"

    case "${status}" in
        READY)
            echo "Host localhost is READY."
            exit 0
            ;;
        FAILED)
            echo "Host provisioning failed. The report follows:" >&2
            docker compose exec -T platform-db \
                psql -U conductor -d conductor -c \
                "SELECT provisioning_report FROM host_status WHERE ssh_alias = 'localhost';" >&2
            exit 1
            ;;
        *)
            echo "Waiting for localhost to become READY (${attempt}/${MAX_ATTEMPTS})..."
            sleep 2
            ;;
    esac
done

echo "Timed out waiting for localhost provisioning. Inspect the Conductor logs:" >&2
echo "  docker compose logs conductor" >&2
exit 1
