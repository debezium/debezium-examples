#!/usr/bin/env bash
set -euo pipefail

NOTEBOOK_PATH="${NOTEBOOK_PATH:-/workspace/postgres_cdc_pk_change_counts.ipynb}"
EXECUTED_NOTEBOOK_PATH="${EXECUTED_NOTEBOOK_PATH:-/workspace/postgres_cdc_pk_change_counts.executed.ipynb}"

if [[ "${EXECUTE_NOTEBOOK_ON_START:-0}" == "1" ]]; then
  echo "Executing notebook: ${NOTEBOOK_PATH}"
  jupyter nbconvert \
    --to notebook \
    --execute \
    --ExecutePreprocessor.timeout="${NOTEBOOK_EXEC_TIMEOUT_SEC:-300}" \
    "${NOTEBOOK_PATH}" \
    --output "${EXECUTED_NOTEBOOK_PATH}"
fi

exec jupyter lab \
  --ip=0.0.0.0 \
  --port=8888 \
  --no-browser \
  --allow-root \
  --ServerApp.token='' \
  --ServerApp.password='' \
  --ServerApp.root_dir=/workspace
