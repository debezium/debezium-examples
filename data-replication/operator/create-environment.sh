#!/usr/bin/env bash

source env.sh

kind create cluster --name $CLUSTER
kubectl cluster-info --context kind-$CLUSTER

# Create namespace
echo ">>> Creating $NAMESPACE namespace"
kubectl create namespace $NAMESPACE
kubectl config set-context --current --namespace $NAMESPACE

# Deploy PostgreSQL source database
echo ">>> Deploying PostgreSQL (source)"
kubectl create -f infra/001_postgresql.yml -n $NAMESPACE

# Deploy MySQL destination database
echo ">>> Deploying MySQL (destination)"
kubectl create -f infra/002_mysql.yml -n $NAMESPACE

# Deploy Debezium Operator via Helm
echo ">>> Deploying Debezium Operator"
helm repo add debezium https://charts.debezium.io
helm install debezium-operator debezium/debezium-operator -n $NAMESPACE

# Wait until environment is ready
echo ">>> Waiting for databases to be ready"
kubectl wait --for=condition=Available deployments/postgresql --timeout=$TIMEOUT -n $NAMESPACE
kubectl wait --for=condition=Available deployments/mysql --timeout=$TIMEOUT -n $NAMESPACE

echo ">>> Waiting for Debezium Operator to be ready"
kubectl wait --for=condition=Available deployments/debezium-operator --timeout=$TIMEOUT -n $NAMESPACE

echo ""
echo "Kubernetes environment with PostgreSQL (source), MySQL (destination) and Debezium Operator is ready"
