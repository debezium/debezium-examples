# Tutorial: Running Debezium with Plain/Official Kafka Container Images

This tutorial demonstrates how to run Debezium connectors using the official `apache/kafka` container image (instead of pre-baked Debezium images like `quay.io/debezium/connect`).

Using official base images allows you to align Kafka and Connect versions, bypass firewalls restricting access to `quay.io`, and adapt easily to standard enterprise environments.

---

## Prerequisites

Before starting, ensure you have:
* **Docker** and **Docker Compose** (or **Podman** / **Podman Compose**) installed.
* `curl` and `jq` installed on your host system.

---

## Directory Structure

```text
plain-kafka-containers/
├── connect/
│   ├── Dockerfile.connect               # Bakes Debezium plugin into the apache/kafka image
│   └── connect-distributed.properties  # Kafka Connect worker configuration
├── postgres/
│   └── init.sql                        # Database initialization script
├── docker-compose.yaml              # Multicontainer setup (Kafka Broker, Kafka Connect, Postgres)
└── register-postgres.json          # Connector registration payload
```

---

## How It Works

The `docker-compose.yaml` uses the official `apache/kafka` image for both the Kafka broker and the Kafka Connect worker. The `kafka-connect` service has a `build:` block pointing to the `connect/` directory, which:

1. Extends `apache/kafka`
2. Downloads and bakes the Debezium PostgreSQL connector plugin into `/opt/kafka/plugins`
3. Copies `connect-distributed.properties` to configure the Connect worker
4. Runs `connect-distributed.sh` on startup

Docker Compose automatically builds this image when you first run `docker compose up`.

---

## Running the Tutorial

### Step 1: Start the Stack

```bash
docker compose --env-file ../../.env up -d
```

Docker Compose will build the custom Connect image on first run and start all three services: `postgres`, `kafka-broker`, and `kafka-connect`.

### Step 2: Verify Plugins are Loaded

Wait about 30–60 seconds for the Kafka Connect worker to fully start. Query the Connect REST API to verify the Debezium PostgreSQL connector is discovered:

```bash
curl -s http://localhost:8083/connector-plugins | jq
```

You should see `io.debezium.connector.postgresql.PostgresConnector` in the response:
```json
[
  {
    "class": "io.debezium.connector.postgresql.PostgresConnector",
    "type": "source",
    "version": "3.6.1.Final"
  }
]
```

### Step 3: Register the Debezium Postgres Connector

Submit the configuration payload from `register-postgres.json` to the Connect REST API:

```bash
curl -i -X POST \
  -H "Accept:application/json" \
  -H "Content-Type:application/json" \
  http://localhost:8083/connectors/ \
  -d @register-postgres.json
```

Verify the connector is running:
```bash
curl -s http://localhost:8083/connectors/postgres-connector/status | jq
```

### Step 4: Verify CDC Events in Kafka Topics

1. Start a consumer to read messages from the `dbserver1.public.customers` topic:
   ```bash
   docker exec -it kafka-broker /opt/kafka/bin/kafka-console-consumer.sh \
     --bootstrap-server localhost:9092 \
     --topic dbserver1.public.customers \
     --from-beginning
   ```

2. Open another terminal and insert a row into PostgreSQL:
   ```bash
   docker exec -it postgres psql -U postgres -d inventory -c \
     "INSERT INTO customers (first_name, last_name, email) VALUES ('John', 'Doe', 'john.doe@example.com');"
   ```

3. The consumer terminal will show a CDC event:
   ```json
   {
     "before": null,
     "after": {
       "id": 4,
       "first_name": "John",
       "last_name": "Doe",
       "email": "john.doe@example.com"
     },
     "op": "c"
   }
   ```

### Step 5: Shut Down

```bash
docker compose --env-file ../../.env down -v
```
