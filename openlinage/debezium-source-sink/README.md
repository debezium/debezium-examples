# Complete Data Lineage with Debezium Source and Sink

This demo showcases how to leverage Debezium OpenLineage support for data lineage tracking in a source-to-sink data pipeline.
The setup includes a Postgres database, Kafka, Debezium Source Connector, Debezium MongoDB Sink Connector, MongoDB, and OpenLineage (Marquez) for comprehensive data lineage tracking.

## Architecture Overview

The demo creates a CDC pipeline that:
1. **Data Capture**: Debezium Source Connector captures real-time changes from Postgres tables
2. **Message Streaming**: CDC events are published to Kafka topics
3. **Data Replication**: Debezium MongoDB Sink Connector consumes CDC events and writes to MongoDB
4. **Lineage Tracking**: OpenLineage/Marquez tracks the complete data flow from source database through Kafka to sink database
5. **Output**: Data is replicated to MongoDB with full lineage visibility

### Data Flow Details

The pipeline performs the following:

1. **Source Database**: Postgres database with inventory schema containing tables like:
    - `inventory.products` - Product catalog (ID, name, description, weight)
    - Other inventory tables

2. **Change Data Capture**:
    - Debezium Postgres Connector monitors database changes
    - Captures insert, update, and delete operations
    - Publishes CDC events to Kafka topics with full Debezium envelope format

3. **Message Streaming**:
    - CDC events flow through Kafka topics
    - Topic naming follows pattern: `{topic.prefix}.{schema}.{table}`
    - Example: `inventory.inventory.products`

4. **Data Sink**:
    - Debezium MongoDB Sink Connector consumes CDC events from Kafka
    - Writes data to MongoDB collections
    - Maintains data consistency between source and sink

5. **Lineage Tracking**: Both source and sink connectors emit OpenLineage events to track:
    - Source table schemas and data versions
    - Kafka topics and message schemas
    - Sink collections and transformations
    - Complete end-to-end lineage from Postgres to MongoDB

## Prerequisites

- Docker and Docker Compose
- [kcctl](https://github.com/kcctl/kcctl) (Kafka Connect CLI)
- jq (for JSON processing)

## Components

- **Postgres**: Source database with inventory data
- **Kafka**: Message broker for CDC events
- **Debezium Postgres Connector**: Captures database changes from Postgres and publishes to Kafka
- **Debezium MongoDB Sink Connector**: Consumes CDC events from Kafka and writes to MongoDB
- **MongoDB**: Sink database for replicated data
- **OpenLineage/Marquez**: Tracks complete data lineage from source to sink

## Step-by-Step Setup

### Start OpenLineage/Marquez

First, clone the Marquez repository if you haven't already:

```bash
git clone https://github.com/MarquezProject/marquez && cd marquez
```

Then start the Marquez backend for data lineage tracking:

```bash
./docker/up.sh --db-port 5433
```

### Start the Demo Infrastructure

Launch all services using Docker Compose:

```bash
docker compose -f docker-compose-sink.yaml up
```

This starts:
- Kafka broker
- Postgres database with sample data
- MongoDB database
- Debezium Connect with OpenLineage support

### Connect Marquez to the Network

Connect the Marquez API to the demo network:

```bash
docker network connect debezium-source-sink_default marquez-api
```

### Verify Kafka Topics

Check available Kafka topics:

```bash
docker compose -f docker-compose-sink.yaml exec kafka /kafka/bin/kafka-topics.sh --list --bootstrap-server kafka:9092
```

### Start Debezium Source Connector

Apply the Postgres source connector configuration:

```bash
kcctl apply -f postgres-connector-openlineage.json
```

This connector will:
- Connect to the Postgres database
- Capture changes from all tables in the inventory schema
- Publish CDC events to Kafka topics
- Emit OpenLineage events for lineage tracking

### Verify CDC Events

Check that CDC events are being published to Kafka:

```bash
docker compose -f docker-compose-sink.yaml exec kafka ./bin/kafka-console-consumer.sh --bootstrap-server=kafka:9092 --topic inventory.inventory.products --from-beginning --max-messages 1 | jq
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
    "name": "inventory",
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

### Start Debezium MongoDB Sink Connector

Apply the MongoDB sink connector configuration:

```bash
kcctl apply -f mongodb-connector-openlineage.json
```

This connector will:
- Subscribe to the `inventory.inventory.products` Kafka topic
- Consume CDC events
- Write data to MongoDB `inventory2` database
- Emit OpenLineage events for lineage tracking

### Verify Data in MongoDB

Check that data is being replicated to MongoDB:

```bash
docker compose -f docker-compose-sink.yaml exec mongodb mongosh --quiet --eval 'db.getSiblingDB("inventory2").products.find().limit(1).pretty()' mongodb://admin:admin@localhost:27017
```

You should see the replicated product data in MongoDB.

### Accessing the UI

Now that all is set up we can check the lineage graph from the **Marquez UI**: http://localhost:3000

You should see something similar to the following:

![Lineage Graph](images/lineage-graph.png)

The lineage graph will show:
- Source: Postgres tables (`postgres://postgres:5432` namespace)
- Intermediate: Kafka topics (`kafka://kafka:9092` namespace)
- Sink: MongoDB collections (`mongodb://mongodb:27017` namespace)

## Cleanup

To clean up all resources:

```bash
docker compose -f docker-compose-sink.yaml down
docker rmi debezium/connect-openlineage
docker volume ls | grep marquez | awk '{print $2}' | xargs docker volume rm
```