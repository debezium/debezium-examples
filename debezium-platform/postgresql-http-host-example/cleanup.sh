#!/usr/bin/env bash

# Copyright Debezium Authors.
#
# Licensed under the Apache License version 2.0, available at
# http://www.apache.org/licenses/LICENSE-2.0

set -euo pipefail

readonly BASE_URL="${CONDUCTOR_URL:-http://localhost:8081}/api"
readonly STATE_FILE=.host-pipeline-state
readonly EXAMPLE_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

# Docker Compose resolves every variable in compose.yml even for `down`.
# The example owns its SSH material, so a fresh terminal can safely use the
# directory beside this script without exposing the user's normal SSH files.
export HOST_EXAMPLE_SSH_DIR="${HOST_EXAMPLE_SSH_DIR:-${EXAMPLE_DIR}/.ssh}"

if [[ -f "${STATE_FILE}" ]]; then
    # shellcheck disable=SC1090
    source "${STATE_FILE}"
    echo "Undeploying pipeline ${PIPELINE_ID}..."
    curl --fail --show-error --silent -X DELETE "${BASE_URL}/pipelines/${PIPELINE_ID}"
    rm -f "${STATE_FILE}"
fi

docker compose down --volumes
