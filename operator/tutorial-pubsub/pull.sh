#! /usr/bin/env bash

source env.sh

gcloud pubsub subscriptions pull $PUBSUB_SUBSCRIPTION --auto-ack --format=json --limit=$1 \
| jq  -r '.[].message.data | @base64d' \
| jq .payload
