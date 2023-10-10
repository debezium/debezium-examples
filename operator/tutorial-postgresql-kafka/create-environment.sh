#! /usr/bin/env bash

source env.sh

kind create cluster --name $CLUSTER
kubectl cluster-info --context kind-$CLUSTER

# Create namespace
echo ">>> Creating debezium namespace"
kubectl create namespace $NAMESPACE
kubectl config set-context --current --namespace $NAMESPACE

# Deploy PostgreSQL database
echo ">>> Deploying PostgreSQL"
kubectl create -f infra/001_postgresql.yml -n $NAMESPACE

# Deploy Strimzi Operator
echo ">>> Deploying Strimzi"
kubectl create -f https://strimzi.io/install/latest?namespace=$NAMESPACE -n $NAMESPACE
kubectl wait --for=condition=Available deployments/strimzi-cluster-operator  --timeout=$TIMEOUT -n $NAMESPACE


# Deploy PostgreSQL database
echo ">>> Deploying Kafka Broker"
kubectl create -f infra/002_kafka-ephemeral.yml -n $NAMESPACE

# Wait until envrionment is ready
echo ">>> Waiting for Database and Kafka to be ready"
kubectl wait --for=condition=Available deployments/postgresql --timeout=$TIMEOUT -n $NAMESPACE
kubectl wait --for=condition=Ready kafkas/dbz-kafka --timeout=$TIMEOUT -n $NAMESPACE

echo ""
echo "Kubernetes environment with PostreSQL database and Kafka broker is ready"
