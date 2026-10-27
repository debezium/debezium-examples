#!/usr/bin/env bash
# Provision a local minikube cluster large enough for the monitoring stack.
set -euo pipefail

cd "$(dirname "$0")"
source ./env.sh

echo ">>> Creating minikube cluster '${CLUSTER}' (6 CPUs, 8Gi memory, ingress addon)..."
minikube start -p "$CLUSTER" --cpus=6 --memory=8192 --addons ingress

echo ">>> Waiting for the node..."
kubectl wait --for=condition=Ready nodes --all --timeout=300s

echo ">>> Waiting for the ingress controller..."
kubectl wait --for=condition=ready pod \
  -l app.kubernetes.io/component=controller \
  -n ingress-nginx --timeout=300s

if [[ "$(uname)" == "Darwin" ]]; then
  # minikube tunnel publishes the ingress on 127.0.0.1. On the Docker driver the
  # node IP from `kubectl cluster-info` is not reachable from the Mac host.
  IP="127.0.0.1"
else
  IP=$(kubectl cluster-info | sed -n 's/.*https:\/\/\([0-9.]*\).*/\1/p' | head -n 1)
fi

if [[ -z "$IP" ]]; then
  echo "Could not determine an IP for ${DEBEZIUM_PLATFORM_DOMAIN}" >&2
  exit 1
fi

echo ">>> Pointing ${DEBEZIUM_PLATFORM_DOMAIN} at ${IP} in /etc/hosts..."
EXISTING=$(grep -E "[[:space:]]${DEBEZIUM_PLATFORM_DOMAIN}([[:space:]]|$)" /etc/hosts || true)
if [[ "$EXISTING" == "${IP} ${DEBEZIUM_PLATFORM_DOMAIN}" || "$EXISTING" == "${IP}	${DEBEZIUM_PLATFORM_DOMAIN}" ]]; then
  echo "Entry already present"
else
  if [[ -n "$EXISTING" ]]; then
    if [[ "$(uname)" == "Darwin" ]]; then
      sudo sed -i '' "/[[:space:]]${DEBEZIUM_PLATFORM_DOMAIN}$/d" /etc/hosts
    else
      sudo sed -i "/[[:space:]]${DEBEZIUM_PLATFORM_DOMAIN}$/d" /etc/hosts
    fi
  fi
  echo "${IP} ${DEBEZIUM_PLATFORM_DOMAIN}" | sudo tee -a /etc/hosts >/dev/null
  echo "Added ${IP} ${DEBEZIUM_PLATFORM_DOMAIN}"
fi

echo ">>> Cluster is ready"
if [[ "$(uname)" == "Darwin" ]]; then
  echo
  echo "On macOS, leave this running in another terminal before continuing:"
  echo "  sudo minikube tunnel -p ${CLUSTER}"
fi
