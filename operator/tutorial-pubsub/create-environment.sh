#! /usr/bin/env bash

source env.sh

kind create cluster --name $CLUSTER
kubectl cluster-info --context kind-$CLUSTER

# Preload images
docker pull quay.io/debezium/operator:$IMAGE_TAG
docker pull quay.io/debezium/server:$IMAGE_TAG
kind load docker-image quay.io/debezium/operator:$IMAGE_TAG --name $CLUSTER
kind load docker-image quay.io/debezium/server:$IMAGE_TAG --name $CLUSTER

# Create namespace
echo ">>> Creating debezium namespace"
kubectl create namespace $NAMESPACE
kubectl config set-context --current --namespace $NAMESPACE

# Deploy PostgreSQL database
echo ">>> Deploying PostgreSQL"
kubectl create -f k8s/database/001_postgresql.yml -n $NAMESPACE

# Wait until envrionment is ready
echo ">>> Waiting for Database to be ready"
kubectl wait --for=condition=Available deployments/postgresql --timeout=$TIMEOUT -n $NAMESPACE

echo ""
echo "Kubernetes environment with PostreSQL database is ready"

# Install OLM 
echo ">>> Installing OLM to Kuberenetes cluster"
./olm/install.sh v0.26.0
