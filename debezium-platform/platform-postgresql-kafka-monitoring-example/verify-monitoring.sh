#!/usr/bin/env bash
# Walk the monitoring path: pipeline -> collector -> Prometheus -> conductor API.
set -euo pipefail

cd "$(dirname "$0")"
source ./env.sh

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required" >&2
  exit 1
fi

PF_PID=""
cleanup() {
  if [[ -n "${PF_PID}" ]]; then
    kill "$PF_PID" >/dev/null 2>&1 || true
    wait "$PF_PID" 2>/dev/null || true
    PF_PID=""
  fi
}
trap cleanup EXIT

wait_port() {
  local port="$1"
  local _
  for _ in $(seq 1 40); do
    if (echo >/dev/tcp/127.0.0.1/"$port") >/dev/null 2>&1; then
      return 0
    fi
    sleep 0.25
  done
  echo "Timed out waiting for localhost:${port}" >&2
  return 1
}

start_pf() {
  local ns="$1"
  local svc="$2"
  local local_port="$3"
  local remote_port="$4"
  cleanup
  kubectl port-forward -n "$ns" "svc/${svc}" "${local_port}:${remote_port}" >/dev/null 2>&1 &
  PF_PID=$!
  wait_port "$local_port"
}

iso_minutes_ago() {
  local mins="$1"
  if date -u -v-1M '+%Y-%m-%dT%H:%M:%SZ' >/dev/null 2>&1; then
    date -u -v-"${mins}"M '+%Y-%m-%dT%H:%M:%SZ'
  else
    date -u -d "${mins} minutes ago" '+%Y-%m-%dT%H:%M:%SZ'
  fi
}

fail=0
ok() { echo "OK   $*"; }
bad() { echo "FAIL $*"; fail=1; }

echo ">>> 1. Pipeline pod"
if kubectl get pods -n "$NAMESPACE" --no-headers | grep -E "^${PIPELINE}-" | grep -q "Running"; then
  ok "pipeline pod is Running"
else
  bad "pipeline pod is not Running"
  kubectl get pods -n "$NAMESPACE" | grep "$PIPELINE" || true
fi

echo ">>> 2. ServiceMonitor release label"
label=$(kubectl get servicemonitor debezium-platform-otel-collector -n "$NAMESPACE" -o jsonpath='{.metadata.labels.release}')
if [[ "$label" == "kube-prometheus-stack" ]]; then
  ok "release=${label}"
else
  bad "ServiceMonitor release label is '${label:-empty}', expected kube-prometheus-stack. Prometheus will not scrape, and every panel stays empty."
fi

echo ">>> 3. Collector is exporting debezium_* metrics"
start_pf "$NAMESPACE" debezium-platform-otel-collector-collector 18889 8889
count=$(curl -sf http://127.0.0.1:18889/metrics | grep -c '^debezium_' || true)
cleanup
if [[ "${count:-0}" -gt 0 ]]; then
  ok "collector exposes ${count} debezium_* samples"
else
  bad "collector has no debezium_* metrics. The pipeline is not exporting, or the collector image has no Prometheus exporter."
fi

echo ">>> 4. Prometheus is scraping the collector"
start_pf "$MONITORING_NAMESPACE" kube-prometheus-stack-prometheus 19090 9090
up=$(curl -sf --get http://127.0.0.1:19090/api/v1/query \
  --data-urlencode 'query=up{job="debezium-platform-otel-collector-collector"}' \
  | jq -r '.data.result[0].value[1] // empty')
cleanup
if [[ "$up" == "1" ]]; then
  ok "Prometheus target is up"
else
  bad "Prometheus scrape is '${up:-empty}'. Check step 2, then wait one scrape interval (15s) and re-run."
fi

echo ">>> 5. Panel registry"
panels=$(curl -sf "${HOST}/api/monitoring/panels" | jq '.panels | length')
if [[ "${panels:-0}" -ge 14 ]]; then
  ok "${panels} panels registered"
else
  bad "expected at least 14 panels, got '${panels:-empty}'"
fi

echo ">>> 6. Connection status"
START=$(iso_minutes_ago 15)
END=$(date -u '+%Y-%m-%dT%H:%M:%SZ')
value=$(curl -sf --get "${HOST}/api/monitoring/panels/connection-status/query" \
  --data-urlencode "pipeline_id=${PIPELINE}" \
  --data-urlencode "start=${START}" \
  --data-urlencode "end=${END}" \
  --data-urlencode "step=15s" \
  | jq -r '[.series[].datapoints[-1][1]] | max // empty')
if [[ -n "$value" ]]; then
  ok "connection-status latest=${value} (1 means connected)"
else
  bad "connection-status has no datapoints yet. If the pipeline just started, wait a minute and re-run."
fi

if [[ "$fail" -ne 0 ]]; then
  exit 1
fi
echo ">>> Monitoring path is wired"
echo "Next: ./generate-traffic.sh"
