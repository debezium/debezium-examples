#!/usr/bin/env bash
# Install Debezium Platform with the monitoring values in values.yaml.
set -euo pipefail

cd "$(dirname "$0")"
source ./env.sh

echo ">>> Installing debezium-platform ${DEBEZIUM_PLATFORM_CHART_VERSION}..."
helm repo add debezium https://charts.debezium.io
helm repo update debezium

HELM_ARGS=(
  upgrade --install debezium-platform debezium/debezium-platform
  --version "$DEBEZIUM_PLATFORM_CHART_VERSION"
  --namespace "$NAMESPACE"
  --create-namespace
  -f ./values.yaml
)
if [[ "${CUSTOM_PANELS:-}" == "true" ]]; then
  HELM_ARGS+=(-f ./values-custom-panels.yaml)
fi

helm "${HELM_ARGS[@]}"

echo ">>> Waiting for conductor, stage, and the OpenTelemetry collector..."
kubectl rollout status deploy/conductor -n "$NAMESPACE" --timeout=300s
kubectl rollout status deploy/stage -n "$NAMESPACE" --timeout=300s
kubectl rollout status deploy/debezium-platform-otel-collector-collector -n "$NAMESPACE" --timeout=300s

echo ">>> ServiceMonitor release label (must be kube-prometheus-stack):"
kubectl get servicemonitor debezium-platform-otel-collector -n "$NAMESPACE" \
  -o jsonpath='{.metadata.labels.release}'; echo

echo ">>> Conductor Prometheus URL:"
kubectl get deploy conductor -n "$NAMESPACE" \
  -o jsonpath='{.spec.template.spec.containers[0].env[?(@.name=="MONITORING_PROMETHEUS_URL")].value}'; echo

echo ">>> Platform is ready at ${HOST}/"
echo "Next: ./seed-pipeline.sh"
