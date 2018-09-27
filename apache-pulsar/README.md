# Streaming Postgres Database Changes to Apache pulsar Using Debezium

[Debezium](http://debezium.io/) allows to capture and stream change events from multiple databases such as MySQL and Postgres and is mostly used with Apache Kafka as the underlying messaging infrastructure.

Using [Debezium's embedded mode](http://debezium.io/docs/embedded/) it is possible to stream database changes to arbitrary destinations and thus not be limited to Kafka as the only broker.
This demo shows how to stream changes from a Postgres database to Apache [Pulsar](https://pulsar.incubator.apache.org/).

## Prerequisites

* Java 8 development environment
* Docker installation

## Starting Apache Pulsar and Postgres

* Start up a single Pulsar node via Docker:

    docker run -it -p 6650:6650 -p 8080:8080 --rm --name pulsar apachepulsar/pulsar:2.1.1-incubating bin/pulsar standalone

* Start up Postgres via Docker, with the example database used in the Debezium tutorial:

    docker run -it --rm --name postgres -p 5432:5432 -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres debezium/example-postgres:0.8

* Launch psql to run some SQL queries:

    docker run -it --rm -e PGOPTIONS="--search_path=inventory" -e PGPASSWORD=postgres --link postgres:postgres debezium/example-postgres:0.8 psql -h postgres -U postgres

## Building the Source Code and Running Debezium Embedded

    mvn clean install
    java -jar target/apache-pulsar-1.0-SNAPSHOT-jar-with-dependencies.jar

To configure Debezium parameters refer to `config.properties`.
All the config.properties keys can be overridden using environment variables.
For example, in order to set `database.password=password` set this environment variable: `DATABASE_PASSWORD=password`.

Each table will be published to its own topic, named like so: _persistent://public/default/<server>.<schema>.<table>_.

## Testing

Modify a record in psql, e.g. like this:

    update customers set first_name = 'Sarah' where id = 1001;

Consume the corresponding change event from the Pulsar topic, e.g. using the command line client
(by default it will consume one event and then return):

    docker exec -i pulsar bin/pulsar-client consume -s my-subscription -n 0 persistent://public/default/test.inventory.customers
