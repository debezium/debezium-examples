# Streaming Postgres Database Changes to Apache pulsar Using Debezium

[Debezium](http://debezium.io/) allows to capture and stream change events from multiple databases such as MySQL and Postgres and is mostly used with Apache Kafka as the underlying messaging infrastructure.

Using [Debezium's embedded mode](http://debezium.io/docs/embedded/) it is possible though to stream database changes to arbitrary destinations and thus not be limited to Kafka as the only broker.
This demo shows how to stream changes from Postgres database running on a local machine to an Apache [Pulsar](https://pulsar.incubator.apache.org/).

## Prerequisites

* Java 8 development environment
* Setting up Postgres can be found here https://debezium.io/docs/connectors/postgresql/
 
* To configure debezium parameters refer `config.properties`. All the config can be set as environment variables. Ex:to set `database.password=password` set environment variable as `DATABASE_PASSWORD=password`
* Each table will be published with it own topic name. 

## Building
* `maven package` will produce shaded jar which can be run with `java -jar dbz.pulsar.postgres-1.0-SNAPSHOT.jar`
