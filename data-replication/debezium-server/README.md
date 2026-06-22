# Kafka-Less Data Replication with Debezium Server (PostgreSQL to MySQL)

## Overview

This example demonstrates how to set up a Kafka-less data replication pipeline from PostgreSQL to MySQL using Debezium Server with the JDBC sink. No Kafka cluster is needed — just a source database, a destination database, and a single Debezium Server container.

## Prerequisites

Before getting started, ensure you have the following prerequisites:

1. Docker
2. Docker Compose

## Project Structure

```
└── data-replication/debezium-server
    ├── infrastructure-compose.yaml
    ├── application.properties
    └── README.md
```

- `infrastructure-compose.yaml`: Docker Compose file to start PostgreSQL (source) and MySQL (destination)
- `application.properties`: Debezium Server configuration with JDBC sink
- `README.md`: This guide

## How to Run

### 1. Start the source and destination databases

```bash
docker compose -f infrastructure-compose.yaml up -d
```

### 2. Start Debezium Server

```bash
docker run --name inventory-pipeline -d \
   -v $(pwd)/application.properties:/debezium/config/application.properties:z \
   -p 8080:8080 \
   --network debezium-jdbc-backend \
   quay.io/debezium/server:nightly
```

### 3. Verify data has been replicated to MySQL

```bash
docker exec -it mysql mysql -uroot -pdebezium target_db -e "SELECT * FROM migration_inventory_customers;"
docker exec -it mysql mysql -uroot -pdebezium target_db -e "SELECT * FROM migration_inventory_products;"
docker exec -it mysql mysql -uroot -pdebezium target_db -e "SELECT * FROM migration_inventory_orders;"
docker exec -it mysql mysql -uroot -pdebezium target_db -e "SELECT * FROM migration_inventory_products_on_hand;"
```

### 4. Insert a new record in the source database

```bash
docker exec -it postgres psql -U postgres -d postgres -c \
"INSERT INTO inventory.customers (first_name, last_name, email) VALUES ('John', 'Doe', 'john.doe@example.com');"
```

### 5. Verify the new record was replicated

```bash
docker exec -it mysql mysql -uroot -pdebezium target_db -e "SELECT * FROM migration_inventory_customers;"
```

## Cleanup

```bash
docker stop inventory-pipeline && docker rm inventory-pipeline
docker compose -f infrastructure-compose.yaml down
```
