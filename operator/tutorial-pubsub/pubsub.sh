#! /usr/bin/env bash

source env.sh

PUBSUB_SECRET_MANIFEST="k8s/debezium/001_pubsub.yml"

# Generate pubsub credentials secret
echo ""
echo "Generating $PUBSUB_SECRET_MANIFEST file"

PUBSUB_CREDENTIALS_BASE64=$(cat $PUBSUB_CREDENTIALS_FILE | base64 -w0)

cat << EOF > $PUBSUB_SECRET_MANIFEST
apiVersion: v1
kind: Secret
metadata:
  name: pubsub-credentials
type: opaque
stringData:
  PUBSUB_PROJECT_ID: $PUBSUB_PROJECT_ID
data:
  PUBSUB_CREDENTIALS: $PUBSUB_CREDENTIALS_BASE64
EOF

# Create new topic and subscription
echo ""
echo "(Re)creating pubsub topic and subscription"

gcloud pubsub topics delete $PUBSUB_TOPIC
gcloud pubsub subscriptions delete $PUBSUB_SUBSCRIPTION

gcloud pubsub topics create $PUBSUB_TOPIC
gcloud pubsub subscriptions create $PUBSUB_SUBSCRIPTION --topic $PUBSUB_TOPIC --enable-message-ordering
