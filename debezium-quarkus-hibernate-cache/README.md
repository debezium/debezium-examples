# Debezium Quarkus Hibernate Second-Level Cache Invalidation

This example demonstrates how to use the official **`debezium-quarkus-hibernate-cache`** extension to automatically invalidate items in the Hibernate Second-Level Cache (L2C) when external data changes occur in PostgreSQL (e.g. via direct SQL updates or external services bypassing Hibernate ORM).

## Architecture & How It Works

1. **Automatic Metamodel Discovery:** The extension scans the JPA metamodel at application startup to identify `@Cacheable` entities.
2. **Built-in CDC Capture:** The extension's internal `@Capturing` handler listens for Debezium Change Data Capture (CDC) events streamed from the PostgreSQL Write-Ahead Log (WAL).
3. **Automated L2C Eviction:** When an entity is updated or deleted in the database, the extension automatically evicts the corresponding entity from Hibernate's L2 Cache without requiring any custom Java eviction code in your application.

## Prerequisites

- JDK 21+
- Apache Maven 3.9+
- Docker & Docker Compose

## Quick Start

### 1. Build and Start Services

```bash
mvn clean package -DskipTests
docker compose up -d --build
```

### 2. Manual Testing

1. Query an item using HTTP GET (loads `Notebook` into the L2 Cache):
   ```bash
   curl http://localhost:8080/items/1
   ```
   Response:
   ```json
   {"id":1,"name":"Notebook","price":10.99}
   ```

2. Update the item price directly in PostgreSQL via `psql` (bypassing Hibernate ORM):
   ```bash
   docker compose exec postgres psql -U postgres -d inventory -c "UPDATE inventory.item SET price = 15.99 WHERE id = 1;"
   ```

3. Debezium's `@Capturing` handler captures the WAL update event and automatically evicts `Item` #1 from the L2 Cache.

4. Query the item again to observe the updated price:
   ```bash
   curl http://localhost:8080/items/1
   ```
   Response:
   ```json
   {"id":1,"name":"Notebook","price":15.99}
   ```

### 3. Running Automated E2E Tests

To run the automated E2E test runner:

```bash
python scripts/run-example-test.py debezium-quarkus-hibernate-cache
```
