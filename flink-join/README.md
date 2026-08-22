# Flink Foreign Key Joins

This example demonstrates how two Debezium change data topics can be joined via Flink.

The source database contains two tables, `customers` and `addresses`, with a foreign key relationship from the latter to the former,
i.e. a customer can have multiple addresses.

Using Flink the change event for the parent customers are represented as a dynamic table defined with CREATE TABLE, while the child addresses are represented as a stream of pojos that are first aggregated by the foreign key.
Each insertion, update or deletion of a record on either side will re-trigger the join.

## Building

Prepare the Java components by first performing a Maven build.

```console
$ mvn clean install
```

## Environment

Setup the necessary environment variables

```console
$ export DEBEZIUM_VERSION=1.2

```

The `DEBEZIUM_VERSION` specifies which version of Debezium artifacts should be used.

## Start the demo  

Start all Debezium components:

```console
$ docker-compose up connect
```

This creates the kafka connect service and all dependent services defined in the `docker-compose.yaml` file.

## Configure the Debezium connector

Register the connector to stream outbox changes from the order service: 

```console
$ http PUT http://localhost:8083/connectors/inventory-connector/config < register-postgres.json
HTTP/1.1 201 Created
```
## Run the Flink Job

To run the Flink job in local mode, simply compile and start the job class: 

```console
$ mvn clean install
$ mvn exec:java \
      -Dexec.mainClass="io.debezium.examples.flink.join.FlinkJoinTableStream" \
      -Dexec.classpathScope=compile
```

To run the Flink job against a remote cluster is a little more involved.  The simplest approach is to create a docker compose session cluster (docker-compose up jobmanager) then copy the Flink Kafka dependency to the lib and follow the instructions for submitting this project jar as a job - see the [Flink docs](https://ci.apache.org/projects/flink/flink-docs-stable/ops/deployment/docker.html#session-cluster-with-docker-compose).

## Review the outcome

Examine the joined events using _kafkacat_:

```console
$ docker run --tty --rm \
    --network kstreams-fk-join-network \
    debezium/tooling:1.1 \
    kafkacat -b kafka:9092 -C -o beginning -q \
    -t customers-with-addresses | jq .
```

## Useful Commands

Getting a session in the Postgres DB of the "order" service:

```console
$ docker run --tty --rm -i \
        --network kstreams-fk-join-network \
        debezium/tooling:1.1 \
        bash -c 'pgcli postgresql://postgres:postgres@postgres:5432/postgres'
```

E.g. to update a customer record:

```sql
update inventory.customers set first_name = 'Sarah' where id = 1001;
```
