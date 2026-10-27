#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"
source ./env.sh

echo ">>> Deleting minikube profile ${CLUSTER}"
minikube delete -p "$CLUSTER"

echo "The cluster is gone. To drop the hosts entry as well:"
echo "  sudo sed -i'' -e '/[[:space:]]${DEBEZIUM_PLATFORM_DOMAIN}$/d' /etc/hosts"
