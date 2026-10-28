#!/usr/bin/env bash
# Install the operators the platform chart needs before monitoring can be enabled.
set -euo pipefail

cd "$(dirname "$0")"
source ./env.sh

echo ">>> Installing the OpenTelemetry Operator..."
helm repo add open-telemetry https://open-telemetry.github.io/opentelemetry-helm-charts
helm repo update open-telemetry
helm upgrade --install opentelemetry-operator open-telemetry/opentelemetry-operator \
  -n "$OTEL_NAMESPACE" --create-namespace \
  --set admissionWebhooks.certManager.enabled=false \
  --set admissionWebhooks.autoGenerateCert.enabled=true

echo ">>> Waiting for the OpenTelemetry Operator..."
# Wait on the Deployment, not on pods by label. Helm creates the Deployment before
# it returns, but its pods appear a moment later, and `kubectl wait` fails outright
# ("no matching resources found") when the selector matches nothing yet.
kubectl rollout status deploy/opentelemetry-operator -n "$OTEL_NAMESPACE" --timeout=300s

echo ">>> Installing kube-prometheus-stack..."
echo "    Keep this release name. values.yaml labels the ServiceMonitor release: kube-prometheus-stack"
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update prometheus-community
helm upgrade --install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  -n "$MONITORING_NAMESPACE" --create-namespace

echo ">>> Waiting for Prometheus (this often takes a few minutes)..."
# The Prometheus pod is created by the Prometheus Operator reconciling this CR, which
# happens seconds *after* helm returns. Waiting on the pod by label loses that race on
# a first install. The CR exists as soon as helm is done, and its Available condition
# only flips true once the pods behind it are ready.
kubectl wait prometheus/kube-prometheus-stack-prometheus \
  -n "$MONITORING_NAMESPACE" --for=condition=Available --timeout=600s

echo ">>> Prometheus ServiceMonitor selector:"
kubectl get prometheus -n "$MONITORING_NAMESPACE" -o jsonpath='{.items[0].spec.serviceMonitorSelector}'; echo
echo "    values.yaml sets release: kube-prometheus-stack so this selector matches."

echo ">>> Monitoring stack is ready"
echo "Next: ./setup-infra.sh"
