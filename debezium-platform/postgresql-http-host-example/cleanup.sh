#!/usr/bin/env bash

# Copyright Debezium Authors.
#
# Licensed under the Apache License version 2.0, available at
# http://www.apache.org/licenses/LICENSE-2.0

set -euo pipefail

readonly BASE_URL="${CONDUCTOR_URL:-http://localhost:8081}/api"
readonly STATE_FILE=.host-pipeline-state

# Docker Compose resolves every variable in compose.yml even for `down`.
# Cleanup only needs the value to parse the bind mount, so make a fresh
# terminal work without requiring the user to repeat the startup export.
export HOST_SSH_DIR="${HOST_SSH_DIR:-${HOME}/.ssh}"

if [[ -f "${STATE_FILE}" ]]; then
    # shellcheck disable=SC1090
    source "${STATE_FILE}"
    echo "Undeploying pipeline ${PIPELINE_ID}..."
    curl --fail --show-error --silent -X DELETE "${BASE_URL}/pipelines/${PIPELINE_ID}"
    rm -f "${STATE_FILE}"
fi

docker compose down --volumes
