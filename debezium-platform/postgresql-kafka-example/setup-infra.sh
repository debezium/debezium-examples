#! /usr/bin/env bash

source env.sh

NAMESPACE="debezium-platform"

echo ">>> Creating namespace: $NAMESPACE"
kubectl create ns "$NAMESPACE" || echo "Namespace $NAMESPACE already exists"

echo ">>> Setting kubectl context to use namespace: $NAMESPACE"
kubectl config set-context --current --namespace="$NAMESPACE"

echo ">>> Deploying PostgreSQL..."
kubectl create -f ./source-database/001_postgresql.yml -n $NAMESPACE &

echo ">>> Deploying Strimzi Kafka Operator..."
(
  helm repo add strimzi https://strimzi.io/charts/
  helm repo update strimzi
  helm install strimzi-operator strimzi/strimzi-kafka-operator \
    --version 0.44.0 \
    --namespace "$NAMESPACE"

  echo ">>> Waiting for Strimzi operator pod to be ready..."
  kubectl wait pod -n "$NAMESPACE" -l name=strimzi-cluster-operator \
    --for=condition=Ready --timeout=300s

  echo ">>> Applying Kafka cluster YAML..."
  kubectl create -f ./destination-kafka/001_kafka.yml
) &

# Wait for both background jobs to finish
wait

echo ">>> Waiting for Database and Kafka to be ready"
echo ">>> Waiting for database..."
kubectl wait pod -n "$NAMESPACE" -l app=postgres \
  --for=condition=Ready --timeout=300s
  
echo ">>> Waiting for Kafka broker pod to be ready 2..."
kubectl wait pod -n "$NAMESPACE" -l strimzi.io/name=dbz-kafka-kafka --for=condition=Ready --timeout=300s

# Kafka entity operator pod
echo ">>> Waiting for Kafka broker pod to be ready 2..."
kubectl wait pod -n "$NAMESPACE" -l strimzi.io/name=dbz-kafka-entity-operator --for=condition=Ready --timeout=300s


echo "✅ Done: PostgreSQL and Kafka are up and running!"
