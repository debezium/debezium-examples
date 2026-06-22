#!/usr/bin/env bash

source env.sh

kind delete cluster --name $CLUSTER

echo ">>> Removing /etc/hosts entry for $DEBEZIUM_PLATFORM_DOMAIN..."
sudo sed -i "/$DEBEZIUM_PLATFORM_DOMAIN/d" /etc/hosts
