# Kafka-Less Data Replication with Debezium Server Native (PostgreSQL to Redis)

## Overview

This example demonstrates how to set up a Kafka-less data replication pipeline from PostgreSQL to Redis using the native build of Debezium Server with the Redis sink. No Kafka cluster is needed — just a source database, a Redis instance, and a single Debezium Server Native container.

## Prerequisites

Before getting started, ensure you have the following prerequisites:

1. Docker
2. Docker Compose

## Project Structure

```
└── data-replication/debezium-server-native
    ├── infrastructure-compose.yaml
    ├── application.properties
    └── README.md
```

- `infrastructure-compose.yaml`: Docker Compose file to start PostgreSQL (source) and Redis (destination)
- `application.properties`: Debezium Server Native configuration with Redis sink
- `README.md`: This guide

## How to Run

### 1. Start PostgreSQL and Redis

```bash
docker compose -f infrastructure-compose.yaml up -d
```

### 2. Start Debezium Server Native

```bash
docker run --name inventory-pipeline -d \
   -v $(pwd)/application.properties:/debezium/config/application.properties:z \
   -p 8080:8080 \
   --network debezium-native-backend \
   quay.io/debezium/server-native:nightly
```

### 3. Verify data has been replicated to Redis

```bash
`docker exec -it redis redis-cli KEYS '*'`
```

To inspect the replicated stream entries for a specific topic:

```bash
docker exec -it redis redis-cli XRANGE migration.inventory.customers - +
docker exec -it redis redis-cli XRANGE migration.inventory.products - +
docker exec -it redis redis-cli XRANGE migration.inventory.orders - +
docker exec -it redis redis-cli XRANGE migration.inventory.products_on_hand - +
```

### 4. Insert a new record in the source database

```bash
docker exec -it postgres psql -U postgres -d postgres -c \
"INSERT INTO inventory.customers (first_name, last_name, email) VALUES ('John', 'Doe', 'john.doe@example.com');"
```

### 5. Verify the new record was replicated

```bash
docker exec -it redis redis-cli XRANGE migration.inventory.customers - +
```

## Cleanup

```bash
docker stop inventory-pipeline && docker rm inventory-pipeline
docker compose -f infrastructure-compose.yaml down
```