#!/usr/bin/env bash
# Mount custom-panels.yml and upgrade the platform so the extra panels survive helm upgrades.
set -euo pipefail

cd "$(dirname "$0")"
source ./env.sh

echo ">>> Creating ConfigMap custom-panels from custom-panels.yml"
kubectl create configmap custom-panels -n "$NAMESPACE" \
  --from-file=panels.yml=./custom-panels.yml \
  --dry-run=client -o yaml | kubectl apply -f -

echo ">>> Upgrading the platform with the panels volume mounted"
CUSTOM_PANELS=true ./setup-platform.sh

echo ">>> Custom panels are loaded. The conductor re-reads the file about every 30s."
echo "Edit a title with: kubectl edit configmap custom-panels -n ${NAMESPACE}"
echo "Then refresh the Monitoring tab. The conductor pod does not need to restart."
