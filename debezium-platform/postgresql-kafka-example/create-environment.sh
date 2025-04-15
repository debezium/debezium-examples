#! /usr/bin/env bash

source env.sh

# Update /etc/hosts to resolve the domain
echo ">>> Checking and updating /etc/hosts entry for platform domain..."

IP=$(kubectl cluster-info | sed -n 's/.*https:\/\/\([0-9.]*\).*/\1/p' | head -n 1)
HOSTNAME=${DEBEZIUM_PLATFORM_DOMAIN:-platform.debezium.io}
EXISTING=$(grep "$HOSTNAME" /etc/hosts)

if [ -z "$EXISTING" ]; then
    echo "$IP $HOSTNAME" | sudo tee -a /etc/hosts
    echo "Added new entry: $IP $HOSTNAME"
else
    EXISTING_IP=$(echo "$EXISTING" | awk '{print $1}')
    if [ "$EXISTING_IP" != "$IP" ]; then
        echo "WARNING: $HOSTNAME is already associated with IP $EXISTING_IP"
        echo "Current kubectl IP is $IP"
        read -p "Do you want to update the entry? (y/n) " -r
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            sudo sed -i "/$HOSTNAME/d" /etc/hosts
            echo "$IP $HOSTNAME" | sudo tee -a /etc/hosts
            echo "Updated hosts file with new IP"
        else
            echo "Hosts file not modified"
        fi
    else
        echo "Entry for $HOSTNAME already exists with the same IP"
    fi
fi

echo ">>> Creating minikube cluster 'debezium'..."
minikube start -p $CLUSTER --addons ingress

echo ">>> Waiting for minikube components to be ready..."
# Wait for all minikube components to show expected status
MAX_ATTEMPTS=30
ATTEMPT=1
while [[ $ATTEMPT -le $MAX_ATTEMPTS ]]; do
  STATUS=$(minikube status --profile "$CLUSTER")

  if echo "$STATUS" | grep -q "host: Running" &&
     echo "$STATUS" | grep -q "kubelet: Running" &&
     echo "$STATUS" | grep -q "apiserver: Running" &&
     echo "$STATUS" | grep -q "kubeconfig: Configured"; then
    echo ">>> Minikube components are ready"
    break
  else
    echo "Waiting... ($ATTEMPT/$MAX_ATTEMPTS)"
    sleep 5
    ((ATTEMPT++))
  fi
done

if [[ $ATTEMPT -gt $MAX_ATTEMPTS ]]; then
  echo "❌ Timed out waiting for minikube to be ready"
  exit 1
fi

echo ">>> Waiting for Kubernetes environment to be ready..."
kubectl wait --for=condition=Ready nodes --all --timeout=300s

echo ">>> Kubernetes environment is ready"


# Only run minikube tunnel on macOS
if [[ "$(uname)" == "Darwin" ]]; then
  echo ">>> Starting 'minikube tunnel' in the background (macOS only)..."
  minikube tunnel -p "$CLUSTER"
fi