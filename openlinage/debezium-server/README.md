# Data Lineage with Debezium Server

This demo showcases how to leverage Debezium OpenLineage support for data lineage tracking in a standalone Debezium Server deployment.
The setup includes a Postgres database, Kafka, and Debezium Server configured to capture database changes and publish them to Kafka topics with OpenLineage metadata tracking.

## Architecture Overview

The demo creates a simple CDC pipeline that:
1. **Data Capture**: Debezium Server captures real-time changes from Postgres tables
2. **Message Streaming**: CDC events are published to Kafka topics
3. **Lineage Tracking**: OpenLineage events are emitted and logged, tracking the complete data flow from source database to Kafka
4. **Output**: CDC events are available in Kafka topics for downstream consumption

### Data Flow Details

The pipeline performs the following:

1. **Source Database**: Postgres database with inventory schema containing tables like:
    - `inventory.products` - Product catalog (ID, name, description, weight)
    - `inventory.orders` - Order transactions
    - `inventory.customers` - Customer information
    - Other inventory tables

2. **Change Data Capture**:
    - Debezium Server monitors database changes using the Postgres connector
    - Captures insert, update, and delete operations
    - Publishes CDC events to Kafka topics with full Debezium envelope format

3. **Message Streaming**:
    - CDC events flow through Kafka topics
    - Topic naming follows pattern: `{topic.prefix}.{schema}.{table}`
    - Example: `tutorial.inventory.products`

4. **Lineage Tracking**: Debezium Server emits OpenLineage events to track:
    - Source table schemas and data versions
    - Kafka topics and message schemas
    - Complete lineage from Postgres tables to Kafka topics

## Prerequisites

- Docker and Docker Compose
- jq (for JSON processing)

## Components

- **Postgres**: Source database with inventory sample data
- **Kafka**: Message broker for CDC events
- **Debezium Server**: Standalone CDC engine that captures changes and publishes to Kafka
- **OpenLineage**: Lineage events are logged to console for inspection

## Step-by-Step Setup

### Start the Demo Infrastructure

Launch all services using Docker Compose:

```bash
docker compose -f docker-compose.yaml up
```

This starts:
- Kafka broker
- Postgres database with sample inventory data
- Debezium Server with OpenLineage support enabled

The Debezium Server will automatically:
- Connect to Postgres
- Perform an initial snapshot of the `inventory` schema tables
- Start streaming changes to Kafka topics
- Emit OpenLineage events for tracking

### Verify Kafka Topics

Check that CDC topics have been created:

```bash
docker compose -f docker-compose.yaml exec kafka /kafka/bin/kafka-topics.sh --list --bootstrap-server kafka:9092
```

You should see topics like:
- `tutorial.inventory.products`
- `tutorial.inventory.orders`
- `tutorial.inventory.customers`
- And other inventory tables

### Verify CDC Events

Check that CDC events are being published to Kafka:

```bash
docker compose -f docker-compose.yaml exec kafka ./bin/kafka-console-consumer.sh --bootstrap-server=kafka:9092 --topic tutorial.inventory.products --from-beginning --max-messages 1 | jq
```

You should see Debezium CDC events with the full envelope format:

```json
{
  "before": null,
  "after": {
    "id": 101,
    "name": "scooter",
    "description": "Small 2-wheel scooter",
    "weight": 3.14
  },
  "source": {
    "version": "3.3.0.Final",
    "connector": "postgresql",
    "name": "tutorial",
    "ts_ms": 1678901234000,
    "snapshot": "true",
    "db": "postgres",
    "schema": "inventory",
    "table": "products"
  },
  "op": "r",
  "ts_ms": 1678901234567
}
```

### Verify OpenLineage Events

Check the OpenLineage events that were emitted during the snapshot:

```bash
docker compose -f docker-compose.yaml exec debezium-server cat logs/console-transport.log | jq '.message | fromjson'
```

This will show the OpenLineage events in JSON format. You should see events like:

**START event** (when snapshot begins):
```json
{
  "eventType": "START",
  "eventTime": "2024-10-09T12:00:00.000Z",
  "run": {
    "runId": "...",
    "facets": {}
  },
  "job": {
    "namespace": "tutorial",
    "name": "tutorial.0",
    "facets": {
      "jobType": {
        "processingType": "STREAMING",
        "integration": "DEBEZIUM",
        "jobType": "JOB"
      }
    }
  },
  "inputs": [
    {
      "namespace": "postgres://postgres:5432",
      "name": "inventory.products",
      "facets": {
        "schema": {
          "fields": [
            {"name": "id", "type": "INT32"},
            {"name": "name", "type": "STRING"},
            {"name": "description", "type": "STRING"},
            {"name": "weight", "type": "FLOAT32"}
          ]
        }
      }
    }
  ],
  "outputs": [
    {
      "namespace": "kafka://kafka:9092",
      "name": "tutorial.inventory.products",
      "facets": {
        "schema": {
          "fields": [
            {"name": "before", "type": "STRUCT"},
            {"name": "after", "type": "STRUCT"},
            {"name": "source", "type": "STRUCT"},
            {"name": "op", "type": "STRING"},
            {"name": "ts_ms", "type": "INT64"}
          ]
        }
      }
    }
  ]
}
```

**COMPLETE event** (when snapshot finishes):
```json
{
  "eventType": "COMPLETE",
  "eventTime": "2024-10-09T12:00:30.000Z",
  "run": {
    "runId": "...",
    "facets": {}
  },
  "job": {
    "namespace": "tutorial",
    "name": "tutorial.0"
  },
  "inputs": [...],
  "outputs": [...]
}
```

## Understanding the Configuration

### Application Properties

The Debezium Server is configured via `/debezium/config/application.properties`:

**Source Configuration** (Postgres):
```properties
debezium.source.connector.class=io.debezium.connector.postgresql.PostgresConnector
debezium.source.database.hostname=postgres
debezium.source.database.port=5432
debezium.source.database.user=postgres
debezium.source.database.password=postgres
debezium.source.database.dbname=postgres
debezium.source.topic.prefix=tutorial
debezium.source.schema.include.list=inventory
```

**Sink Configuration** (Kafka):
```properties
debezium.sink.type=kafka
debezium.sink.kafka.producer.bootstrap.servers=kafka:9092
```

**OpenLineage Configuration**:
```properties
debezium.source.openlineage.integration.enabled=true
debezium.source.openlineage.integration.config.file.path=config/openlineage.yml
debezium.source.openlineage.integration.job.description=This connector does cdc for products
debezium.source.openlineage.integration.job.tags=env=prod,team=cdc
debezium.source.openlineage.integration.job.owners=Mario=maintainer,John Doe=Data scientist
```

### OpenLineage Transport

The `config/openlineage.yml` file configures where OpenLineage events are sent:

```yaml
transport:
  type: console
```

The console transport logs events to the application logs. For production, you would configure this to send to Marquez or another OpenLineage backend:

```yaml
transport:
  type: http
  url: http://marquez-api:5000
```

## Integrating with Marquez

To send lineage events to Marquez instead of just logging them:

### 1. Start Marquez

Clone and start Marquez:

```bash
git clone https://github.com/MarquezProject/marquez && cd marquez
./docker/up.sh --db-port 5433
```

### 2. Update OpenLineage Configuration

Edit `config/openlineage.yml`:

```yaml
transport:
  type: http
  url: http://marquez-api:5000
```

### 3. Connect Networks

Connect Marquez to the Debezium Server network:

```bash
docker network connect debezium-server_default marquez-api
```

### 4. Rebuild Debezium Server

Stop the current containers, remove the old image, and rebuild to apply the new configuration:

```bash
docker compose -f docker-compose.yaml down
docker rmi debezium/debezium-server
docker compose -f docker-compose.yaml up -d
```

### 5. View Lineage in Marquez UI

Open http://localhost:3000 to see the lineage graph showing:
- Input: Postgres tables (`postgres://postgres:5432` namespace)
- Output: Kafka topics (`kafka://kafka:9092` namespace)
- Job: Debezium Server job with metadata and tags

## Cleanup

To clean up all resources:

```bash
docker compose -f docker-compose.yaml down
docker rmi debezium/debezium-server
```

If you also started Marquez:

```bash
cd marquez
./docker/down.sh
docker volume ls | grep marquez | awk '{print $2}' | xargs docker volume rm
```