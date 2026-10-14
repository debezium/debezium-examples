#!/usr/bin/env bash
# Source PostgreSQL and destination Kafka used by the example pipeline.
set -euo pipefail

cd "$(dirname "$0")"
source ./env.sh

echo ">>> Creating namespace ${NAMESPACE}"
kubectl create ns "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -
kubectl config set-context --current --namespace="$NAMESPACE"

echo ">>> Deploying the source PostgreSQL..."
kubectl apply -n "$NAMESPACE" -f ./source-database/001_postgresql.yml

echo ">>> Installing the Strimzi operator ${STRIMZI_VERSION}..."
helm repo add strimzi https://strimzi.io/charts/
helm repo update strimzi
helm upgrade --install strimzi-operator strimzi/strimzi-kafka-operator \
  --version "$STRIMZI_VERSION" \
  --namespace "$NAMESPACE"

echo ">>> Waiting for the Strimzi operator..."
kubectl wait --for=condition=ready pod \
  -l name=strimzi-cluster-operator \
  -n "$NAMESPACE" --timeout=300s

echo ">>> Deploying the Kafka cluster..."
kubectl apply -n "$NAMESPACE" -f ./destination-kafka/001_kafka.yml

echo ">>> Waiting for PostgreSQL..."
kubectl wait --for=condition=ready pod \
  -l app=postgresql \
  -n "$NAMESPACE" --timeout=300s

echo ">>> Waiting for Kafka (this often takes a few minutes)..."
kubectl wait kafka/dbz-kafka --for=condition=Ready --timeout="$TIMEOUT" -n "$NAMESPACE"

echo ">>> PostgreSQL and Kafka are ready"
echo "Next: ./setup-platform.sh"
