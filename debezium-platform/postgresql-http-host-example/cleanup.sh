#!/usr/bin/env bash

# Copyright Debezium Authors.
#
# Licensed under the Apache License version 2.0, available at
# http://www.apache.org/licenses/LICENSE-2.0

set -euo pipefail

readonly BASE_URL="${CONDUCTOR_URL:-http://localhost:8081}/api"
readonly STATE_FILE=.host-pipeline-state
readonly EXAMPLE_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly MAX_UNDEPLOY_ATTEMPTS=900
pipeline_id=""

# Docker Compose resolves every variable in compose.yml even for `down`.
# The example owns its SSH material, so a fresh terminal can safely use the
# directory beside this script without exposing the user's normal SSH files.
export HOST_EXAMPLE_SSH_DIR="${HOST_EXAMPLE_SSH_DIR:-${EXAMPLE_DIR}/.ssh}"

if [[ -f "${STATE_FILE}" ]]; then
    # shellcheck disable=SC1090
    source "${STATE_FILE}"
    pipeline_id="${PIPELINE_ID}"
    if ! [[ "${pipeline_id}" =~ ^[0-9]+$ ]]; then
        echo "Invalid pipeline id in ${STATE_FILE}." >&2
        exit 1
    fi
    echo "Undeploying pipeline ${PIPELINE_ID}..."
    curl --fail --show-error --silent -X DELETE "${BASE_URL}/pipelines/${PIPELINE_ID}"
    echo

    for attempt in $(seq 1 "${MAX_UNDEPLOY_ATTEMPTS}"); do
        deployment_count="$(docker compose exec -T platform-db \
            psql -U conductor -d conductor -Atc \
            "SELECT count(*) FROM host_deployment WHERE pipeline_id = ${PIPELINE_ID};" 2>/dev/null || true)"
        if [[ "${deployment_count}" == "0" ]]; then
            echo "Host deployment for pipeline ${PIPELINE_ID} is removed."
            rm -f "${STATE_FILE}"
            break
        fi
        echo "Waiting for host deployment to be removed (${attempt}/${MAX_UNDEPLOY_ATTEMPTS})..."
        sleep 2
    done

    if [[ -f "${STATE_FILE}" ]]; then
        echo "Timed out waiting for pipeline ${PIPELINE_ID} to undeploy. Compose will remain running." >&2
        exit 1
    fi
fi

# After pipeline undeployment, remove only this example pipeline's persisted
# configuration and offsets from the disposable target VM. A fresh Compose run
# creates a new PostgreSQL instance with a new WAL history, so retaining old
# offsets could make the next Debezium Server container start from an invalid
# position. The runtime names both directories with its container name.
readonly SSH_CONFIG="${HOST_EXAMPLE_SSH_DIR}/config"
readonly SSH_KEY="${HOST_EXAMPLE_SSH_DIR}/debezium-host-example"
readonly SSH_KNOWN="${HOST_EXAMPLE_SSH_DIR}/known_hosts"
readonly HOST_ALIAS="${HOST_ALIAS:-host-example}"

if [[ -n "${pipeline_id}" && -f "${SSH_CONFIG}" && -f "${SSH_KEY}" ]]; then
    echo "Cleaning Debezium data on ${HOST_ALIAS}..."
    if ssh -F "${SSH_CONFIG}" \
           -o "IdentityFile=${SSH_KEY}" \
           -o "UserKnownHostsFile=${SSH_KNOWN}" \
           -o ConnectTimeout=10 \
           -o BatchMode=yes \
           "${HOST_ALIAS}" \
           "sudo rm -rf -- \
                /opt/debezium/configs/debezium-pipeline-${pipeline_id} \
                /opt/debezium/data/debezium-pipeline-${pipeline_id}" \
           2>/dev/null; then
        echo "Remote Debezium data cleaned."
    else
        echo "Warning: could not clean remote Debezium data." \
             "Remove the debezium-pipeline-${pipeline_id} directories" \
             "under /opt/debezium/data and /opt/debezium/configs" \
             "on ${HOST_ALIAS} manually before the next run." >&2
    fi
else
    echo "Skipping remote data cleanup (no example pipeline state or SSH material)."
fi

docker compose down --volumes
