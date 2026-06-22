#!/usr/bin/env bash

source env.sh

BASE_URL="http://${DEBEZIUM_PLATFORM_DOMAIN}"

echo ">>> Creating source connection..."
curl -s -X POST ${BASE_URL}/api/connections \
  -H 'Content-Type: application/json' \
  -d @payloads/connection-source.json | python3 -m json.tool

echo ""
echo ">>> Creating destination connection..."
curl -s -X POST ${BASE_URL}/api/connections \
  -H 'Content-Type: application/json' \
  -d @payloads/connection-dest.json | python3 -m json.tool

echo ""
echo ">>> Creating source..."
curl -s -X POST ${BASE_URL}/api/sources \
  -H 'Content-Type: application/json' \
  -d @payloads/source.json | python3 -m json.tool

echo ""
echo ">>> Creating destination..."
curl -s -X POST ${BASE_URL}/api/destinations \
  -H 'Content-Type: application/json' \
  -d @payloads/destination.json | python3 -m json.tool

echo ""
echo ">>> Creating pipeline..."
curl -s -X POST ${BASE_URL}/api/pipelines \
  -H 'Content-Type: application/json' \
  -d @payloads/pipeline.json | python3 -m json.tool

echo ""
echo "Pipeline 'database-migration' created successfully!"
echo "You can view it at ${BASE_URL}/"
