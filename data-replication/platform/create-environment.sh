#!/usr/bin/env bash

source env.sh

echo ">>> Creating Kind cluster with ingress port mappings..."
cat <<EOF | kind create cluster --name $CLUSTER --config=-
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
- role: control-plane
  extraPortMappings:
    - containerPort: 80
      hostPort: 80
      listenAddress: "${DEBEZIUM_PLATFORM_IP}"
      protocol: TCP
    - containerPort: 443
      hostPort: 443
      listenAddress: "${DEBEZIUM_PLATFORM_IP}"
      protocol: TCP
EOF

kubectl cluster-info --context kind-$CLUSTER

echo ">>> Installing ingress-nginx..."
helm upgrade --install ingress-nginx ingress-nginx \
  --repo https://kubernetes.github.io/ingress-nginx \
  --namespace ingress-nginx \
  --create-namespace \
  --set controller.hostPort.enabled=true

echo ">>> Waiting for ingress controller to be ready..."
kubectl wait --namespace ingress-nginx \
  --for=condition=Available deployment/ingress-nginx-controller \
  --timeout=$TIMEOUT

echo ">>> Updating /etc/hosts..."
HOSTNAME=${DEBEZIUM_PLATFORM_DOMAIN}
EXPECTED="${DEBEZIUM_PLATFORM_IP} ${HOSTNAME}"

if grep -q "$EXPECTED" /etc/hosts; then
    echo "Entry for $HOSTNAME already exists with correct IP"
else
    sudo sed -i "/$HOSTNAME/d" /etc/hosts
    echo "$EXPECTED" | sudo tee -a /etc/hosts
    echo "Added entry: $EXPECTED"
fi

echo ""
echo "Kubernetes environment with ingress is ready"
