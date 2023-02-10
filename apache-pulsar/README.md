# Streaming Postgres Database Changes to Apache Pulsar Using Debezium

This example shows how to consume change events programmatically using the [Debezium's embedded mode](https://debezium.io/documentation/reference/stable/development/engine.html),
This approach allows to stream database changes to arbitrary destinations.
The demo shows how to stream changes from a Postgres database to Apache [Pulsar](https://pulsar.apache.org/).
However, if you want to stream change events into Apache Pulsar in a production scenario, take a look at the ready-made sink for [Debezium Server](https://debezium.io/documentation/reference/stable/operations/debezium-server.html#_apache_pulsar).

Note: An alternative approach for ingesting change events from Debezium into Apache Pulsar is to use Pulsar IO,
which comes with [support for Debezium's connectors](https://pulsar.apache.org/docs/en/io-cdc-debezium/) as of Pulsar 2.3.

## Prerequisites

* Java 11 development environment
* Docker installation

## Starting Apache Pulsar and Postgres

* Start up a single Pulsar node via Docker:

```
docker run -it -p 6650:6650 -p 8080:8080 --rm --name pulsar apachepulsar/pulsar:2.9.2 bin/pulsar standalone
```

* Start up Postgres via Docker, with the example database used in the Debezium tutorial:

```
docker run -it --rm --name postgres -p 5432:5432 -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres quay.io/debezium/example-postgres:2.0
```

* Launch psql to run some SQL queries:

```
docker run -it --rm -e PGOPTIONS="--search_path=inventory" -e PGPASSWORD=postgres --link postgres:postgres quay.io/debezium/example-postgres:2.0 psql -h postgres -U postgres
```

## Building the Source Code and Running Debezium Embedded

```
mvn clean package
java -jar target/apache-pulsar-1.0-SNAPSHOT-jar-with-dependencies.jar
```

To configure Debezium parameters refer to `config.properties`.
All the `config.properties` keys can be overridden using environment variables.
For example, in order to set `database.password=password` set this environment variable: `DATABASE_PASSWORD=password`.

Each table will be published to its own topic, named like so: _persistent://public/default/\<server\>.\<schema\>.\<table\>_.

## Testing

Modify a record in psql, e.g. like this:

```
update customers set first_name = 'Sarah' where id = 1001;
```

Consume the corresponding change event from the Pulsar topic, e.g. using the command line client
(by default it will consume one event and then return):

```
docker exec -i pulsar bin/pulsar-client consume -s my-subscription -n 0 persistent://public/default/test.inventory.customers
```
