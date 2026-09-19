#!/bin/sh
# Creates the products collection in Milvus and inserts four rows, through the Milvus REST API.
set -e
MILVUS=${MILVUS_URL:-http://localhost:19530}

curl -s -X POST $MILVUS/v2/vectordb/collections/create -H 'Content-Type: application/json' -d '{
  "collectionName": "products",
  "schema": {
    "autoId": false,
    "fields": [
      {"fieldName": "pk", "dataType": "Int64", "isPrimary": true},
      {"fieldName": "name", "dataType": "VarChar", "elementTypeParams": {"max_length": "255"}},
      {"fieldName": "category", "dataType": "VarChar", "elementTypeParams": {"max_length": "64"}},
      {"fieldName": "price", "dataType": "Float"},
      {"fieldName": "embedding", "dataType": "FloatVector", "elementTypeParams": {"dim": "4"}}
    ]
  },
  "indexParams": [{"fieldName": "embedding", "indexName": "embedding_idx", "metricType": "COSINE"}]
}'
echo

curl -s -X POST $MILVUS/v2/vectordb/collections/load -H 'Content-Type: application/json' \
    -d '{"collectionName": "products"}'
echo

curl -s -X POST $MILVUS/v2/vectordb/entities/insert -H 'Content-Type: application/json' -d '{
  "collectionName": "products",
  "data": [
    {"pk": 1001, "name": "trail running shoes", "category": "outdoor",     "price": 89.99,  "embedding": [0.1, 0.2, 0.3, 0.4]},
    {"pk": 1002, "name": "cast iron skillet",   "category": "kitchen",     "price": 34.50,  "embedding": [0.9, 0.1, 0.2, 0.3]},
    {"pk": 1003, "name": "wireless headphones", "category": "electronics", "price": 129.00, "embedding": [0.2, 0.8, 0.1, 0.5]},
    {"pk": 1004, "name": "building blocks",     "category": "toys",        "price": 24.99,  "embedding": [0.4, 0.4, 0.9, 0.1]}
  ]
}'
echo
