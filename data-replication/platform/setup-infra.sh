#!/usr/bin/env bash

source env.sh

echo ">>> Creating namespace: $NAMESPACE"
kubectl create ns "$NAMESPACE" || echo "Namespace $NAMESPACE already exists"

echo ">>> Setting kubectl context to use namespace: $NAMESPACE"
kubectl config set-context --current --namespace="$NAMESPACE"

echo ">>> Deploying source PostgreSQL..."
kubectl create -f infra/001_postgresql-source.yml -n $NAMESPACE

echo ">>> Deploying destination PostgreSQL..."
kubectl create -f infra/002_postgresql-dest.yml -n $NAMESPACE

echo ">>> Waiting for databases to be ready..."
kubectl wait --for=condition=Available deployments/postgresql --timeout=$TIMEOUT -n $NAMESPACE
kubectl wait --for=condition=Available deployments/postgresql-dest --timeout=$TIMEOUT -n $NAMESPACE

echo ">>> Installing Debezium Platform via Helm..."
helm repo add debezium https://charts.debezium.io
helm install debezium-platform debezium/debezium-platform \
  --namespace $NAMESPACE \
  --set database.enabled=true \
  --set domain.name=$DEBEZIUM_PLATFORM_DOMAIN \
  --set ingress.className=nginx

echo ">>> Waiting for platform pods to be ready..."
kubectl wait --for=condition=Available deployments/conductor --timeout=$TIMEOUT -n $NAMESPACE
kubectl wait --for=condition=Available deployments/stage --timeout=$TIMEOUT -n $NAMESPACE

echo ""
echo "Debezium Platform is ready at http://$DEBEZIUM_PLATFORM_DOMAIN/"
