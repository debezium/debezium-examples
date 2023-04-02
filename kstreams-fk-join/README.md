# Kafka Streams Foreign Key Joins

This example demonstrates how two Debezium change data topics can be joined via Kafka Streams,
using the new foreign key join feature in Apache Kafka 2.4 ([KIP-213](https://cwiki.apache.org/confluence/display/KAFKA/KIP-213+Support+non-key+joining+in+KTable)).
It accompanies the blog post [Understanding Non-Key Joins With the Quarkus Extension for Kafka Streams](https://debezium.io/blog/2021/03/18/understanding-non-key-joins-with-quarkus-extension-for-kafka-streams/).

The source database contains two tables, `customers` and `addresses`, with a foreign key relationship from the latter to the former,
i.e. a customer can have multiple addresses.

Using Kafka Streams, the change event topics for both tables are loaded into two ``KTable``s,
which are joined on the customer id.
Each insertion, update or deletion of a record on either side will re-trigger the join.

The stream processing application is implemented using the [Quarkus](https://quarkus.io) stack and its Kafka Streams extension.
Amongst other things, this allows building a native binary of each service, resulting in significantly less memory usage and faster start-up than the JVM-based version.

## Building

Prepare the Java components by first performing a Maven build.

```console
$ mvn clean package -f aggregator/pom.xml
```

## Environment

Setup the necessary environment variables

```console
$ export DEBEZIUM_VERSION=2.1
$ export QUARKUS_BUILD=jvm
```

The `DEBEZIUM_VERSION` specifies which version of Debezium artifacts should be used.
The `QUARKUS_BUILD` specifies whether docker-compose will build containers using Quarkus in JVM or Native modes.
The default is `jvm` for JVM mode but `native` can also be specified to build Quarkus native containers.
Please refer to the Quarkus documentation for details around building native binaries.

## Start the demo  

Start all components:

```console
$ docker-compose up --build
```

This executes all configurations set forth by the `docker-compose.yaml` file.

## Configure the Debezium connector

Register the connector that to stream outbox changes from the order service: 

```console
$ http PUT http://localhost:8083/connectors/inventory-connector/config < register-postgres.json

HTTP/1.1 201 Created
```

## Review the outcome

Examine the joined events using _kafkacat_:

```console
$ docker run --tty --rm \
    --network kstreams-fk-join-network \
    quay.io/debezium/tooling:1.2 \
    kafkacat -b kafka:9092 -C -o beginning -q \
    -t customers-with-addresses | jq .
```

## Useful Commands

Getting a session in the Postgres DB of the "order" service:

```console
$ docker run --tty --rm -i \
        --network kstreams-fk-join-network \
        quay.io/debezium/tooling:1.2 \
        bash -c 'pgcli postgresql://postgres:postgres@postgres:5432/postgres'
```

E.g. to update a customer record:

```sql
update inventory.customers set first_name = 'Sarah' where id = 1001;
```

## Running the Aggregator Service in Dev Mode

When working on the _aggregator_ application, running it directly on your host (instead of via Docker Compose)
using the Quarkus dev mode is the recommended approach.
For that

* add `- ADVERTISED_HOST_NAME=<YOUR HOST IP>` to the `environment` section of the "kafka" service in the Docker Compose file.
* run the Docker Compose set-up without the _aggregator_ service, e.g.: `docker-compose up --scale aggregator=0`
* run the *aggregator* app in dev mode, specifying your IP as advertised host, e.g.: `mvn compile quarkus:dev -Dkafka.bootstrap.servers=<YOUR HOST IP>:9092 -Dquarkus.http.port=8079`

Any code changes will immediately picked up after reloading the application in the web browser.
