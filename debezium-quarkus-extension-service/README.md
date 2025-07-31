# Quarkus Debezium Extension Quick Start

This project provides a Quarkus application that demonstrates how to use Debezium Extensions for Quarkus

This quick start is designed to help you:
- discover features of the Debezium Quarkus extension
- use Debezium inside Quarkus
- build a native application that uses Debezium

## Prerequisite

- JDK 21+ installed with `JAVA_HOME` configu    red appropriately
- Apache maven 3.9.9
- Quarkus version 3.25.0
- Docker or Podman
- Optionally Mandrel or GraalVM installed and configured appropriately if you want to build a native executable
- Optionally curl for demo purposes

## Development mode

You can use Quarkus development mode with `mvn quarkus:dev` that automatically start a docker image with `postgres`
and initialize the database with `init.sql`.

## Production mode

You can generate a jar file with `mvn clean install`. Before execute the quarkus jar with `java -jar  ./target/quarkus-app/quarkus-run.jar`, it's necessary a running postgres. You can use the docker compose with: `docker compose up`

## Native mode

To build a native image use: `mvn clean install -Dnative` and run it with `./target/debezium-quarkus-extension-service-1.0.0-SNAPSHOT-runner`. Please be sure to have followed [guideline](https://quarkus.io/guides/building-native-image) to prepare your environment for a native build.

## Demo

So if you start the project in development mode without changing the configuration in `application.properties` in this way:

```bash 
mvn quarkus:dev
```

You should see Quarkus starting a docker container and upload the sql script `init.sql` with some demo tables. Debezium starts snapshotting them:

```text
INFO  [io.quarkus] (Quarkus Main Thread) debezium-quarkus-extension-service 1.0.0-SNAPSHOT on JVM (powered by Quarkus 3.25.0) started in 3.479s. Listening on: http://localhost:8080
INFO  [io.quarkus] (Quarkus Main Thread) Profile dev activated. Live Coding activated.
INFO  [io.quarkus] (Quarkus Main Thread) Installed features: [agroal, cdi, compose, debezium-postgresql, jdbc-postgresql, narayana-jta, rest, smallrye-context-propagation, vertx]
```

```text
INFO  [io.deb.pip.sou.AbstractSnapshotChangeEventSource] (debezium-postgresconnector-dbz-change-event-source-coordinator) Snapshot completed
```

In `InventoryListener` there are different `@Capturing` hook that you will see in the logs:

```text
INFO  [org.acm.InventoryListener] (pool-8-thread-1) capturing product read (snapshot) from dbz.inventory.products with data Product[id=1, name=Laptop, description=High-performance ultrabook, weight=1250]
```

to get the products captured from the database, you can make a `curl` to the `http` endpoint:

```bash
curl http://localhost:8080/products
```

to monitor the status of the snapshot:

```bash
curl http://localhost:8080/monitor/snapshot
```

You can insert/update some row in the database and check it in the log and in the endpoint:

1. get the `container id`
```bash
container_id=$(docker ps --filter "ancestor=quay.io/debezium/postgres:15" --format "{{.ID}}") && echo $container_id
```

2. update the product name from Laptop to Notebook
```bash
docker exec "$container_id" bash -c "PGPASSWORD=quarkus psql -U quarkus quarkus -c \"UPDATE inventory.products SET name = 'Notebook' where id = 1\""
```

3. check the log:
```text
INFO  [org.acm.InventoryListener] (pool-8-thread-1) capturing product update from dbz.inventory.products with data Product[id=1, name=Notebook, description=High-performance ultrabook, weight=1250]
```

4. check the http endpoint:
```bash
curl http://localhost:8080/products
```

result:
```json
[
  {
    "id": 1,
    "name": "Laptop",
    "description": "High-performance ultrabook",
    "weight": 1250
  },
  {
    "id": 2,
    "name": "Smartphone",
    "description": "Latest model with AMOLED display",
    "weight": 180
  },
  {
    "id": 3,
    "name": "Coffee Mug",
    "description": "Ceramic mug with lid",
    "weight": 350
  },
  {
    "id": 1,
    "name": "Notebook",
    "description": "High-performance ultrabook",
    "weight": 1250
  }
]
```

as you can see there are two products with the same id but different names. If you insert a product:

```bash
docker exec "$container_id" bash -c "PGPASSWORD=quarkus psql -U quarkus quarkus -c \"INSERT INTO inventory.products (id, name, description, weight) VALUES (4, 'Sketchbook', 'A ruled paper notebook', 300);\""
```

you will see in the log:

```bash
INFO  [org.acm.InventoryListener] (pool-8-thread-1) capturing product creation from dbz.inventory.products with data Product[id=4, name=Sketchbook, description=A ruled paper notebook, weight=300]
```

and in the `http` endpoint:

```json
[
  {
    "id": 1,
    "name": "Laptop",
    "description": "High-performance ultrabook",
    "weight": 1250
  },
  {
    "id": 2,
    "name": "Smartphone",
    "description": "Latest model with AMOLED display",
    "weight": 180
  },
  {
    "id": 3,
    "name": "Coffee Mug",
    "description": "Ceramic mug with lid",
    "weight": 350
  },
  {
    "id": 1,
    "name": "Notebook",
    "description": "High-performance ultrabook",
    "weight": 1250
  },
  {
    "id": 4,
    "name": "Sketchbook",
    "description": "A ruled paper notebook",
    "weight": 300
  }
]
```