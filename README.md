Copyright Debezium Authors. Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

# Debezium Examples

This repository contains multiple examples for using Debezium, e.g. configuration files, Docker Compose files, OpenShift templates.

## Getting Started

For getting started please check the [tutorial example](./tutorial).

## Examples

* [Apache Pulsar](./apache-pulsar): Streaming **Postgres** database changes to **Apache Pulsar**
* [Audit Logs](./auditlog): Building Audit Logs with Change Data Capture
* [Cache Invalidation](./cache-invalidation): How Debezium can be used to invalidate items in the JPA 2nd level cache after external data changes
* [Camel - pipelines](./camel-component): Building an Apache Camel pipeline that captures **Postgres** database changes
* [Camel - Kafka Connect](./camel-kafka-connect): How to use the Camel Kafka Connect component with Debezium
* [Cloud Events](./cloudevents): How to use cloud events defined in Json with Debezium
* [Database Activity Monitoring](./db-activity-monitoring): How to use Debezium for comprehensive database activity logging and analysis
* [Debezium - End-to-end demo](./end-to-end-demo): End-to-end demo using MySQL as database and Kafka Connect
* [Debezium - Failover](./failover): How Debezium recovers after a database failure
* [Debezium - Monitoring](./monitoring): Monitoring a Debezium instance
* [Debezium - Auto-Creation of Topics](./topic-auto-create): Auto-creating Debezium change data topics
* [Debezium - Outbox Pattern](./outbox): Implement the "outbox pattern", an approach for letting services communicate in an asynchronous and reliable fashion
* [Debezium Management Platform](./debezium-platform): How to use the debezium-platform to create a data pipeline to stream chnages from a PostgreSQL database to Apache Kafka broker in kubernetes cluster.
* [Debezium - Saga Pattern](./saga): How to implement the [Saga pattern](https://microservices.io/patterns/data/saga.html) for realizing distributed transactions across multiple microservices
* [Debezium - Testing](./testcontainers): How to implement an integration test for your CDC set-up using [Testcontainers](https://www.testcontainers.org/)
* [Debezium Connect - Using Kafka with SSL enabled](./kafka-ssl): How to configure Debezium Connect to use a Kafka instance with SSL enabled
* [Debezium Server - Using Google Cloud Pub/Sub](./debezium-server/debezium-server-sink-pubsub): How to deploy [Debezium Server](https://debezium.io/documentation/reference/stable/operations/debezium-server.html) using Postgres, MongoDB, and MySQL as data sources and [Google Cloud Pub/Sub](https://cloud.google.com/pubsub/docs) as a destination
* [Debezium Server - Using storage for offset and schema history](./debezium-server/debezium-server-mysql-redis-pubsub): How to deploy [Debezium Server](https://debezium.io/documentation/reference/stable/operations/debezium-server.html) using MySQL as data sources, Redis and storage for offset and schema history, and [Google Cloud Pub/Sub emulator](https://cloud.google.com/pubsub/docs) as a destination
* [Debezium Server - Using custom topic naming policy](./debezium-server-name-mapper): How to deploy [Debezium Server](https://debezium.io/documentation/reference/stable/operations/debezium-server.html) using a custom topic naming policy
* [Debezium Server - Operator - Using Kafka](./operator/tutorial-postgresql-kafka): How to stream changes from a PostgreSQL database into Apache Kafka with Debezium Server deployed in a Kubernetes cluster
* [Debezium Server - Operator - Using Kafka](./operator/tutorial-pubsub): How to stream changes from a PostgreSQL database into [Google Cloud Pub/Sub](https://cloud.google.com/pubsub/docs) with Debezium Server deployed in a Kubernetes cluster
* [Graphql](./graphql): How to build a GraphQL Subscription on top of Debezium Change Events
* [HTTP Signaling and Notification](./http-signaling-notification): How to create custom signaling and notification channels for Debezium connectors
* [Infinispan - Standalone](./infinispan-standalone): How to use a standalone **Infinispan** cluster that will be used for buffering in-progress transactions by the Debezium Oracle connector
* [Infinispan - Distributed Caching](./distributed-caching): How to combine Debezium and **Infinispan** for an CQRS-style application design
* [JPA Aggregations](./jpa-aggregations): How to materialize consistent aggregates using a PoC-level Hibernate ORM extension
* [Using multiple databases](./engine-wasm): How to capture and stream change events from multiple databases such as MySQL and PostgreSQL
* [JSON Logging](./json-logging): This example uses **Logstash** json_event pattern for log4j
* [Kafka KStreams - Using Kafka Connect MongoDB](./kstreams): How to use kstreams topologies and the [Kafka Connect MongoDB sink connector](https://github.com/hpgrahsl/kafka-connect-mongodb)
* [Kafka KStreams - Foreign Key Joins](./kstreams-fk-join): How two Debezium change data topics can be joined via Kafka Streams
* [Kafka KStreams - WebSockets](./kstreams-live-update): How to use KStreams and stream the merged events to a client using WebSockets
* [Machine Learning - TensorFlow](./machine-learning/tensorflow-mnist): Image classification with Debezium and TensorFlow
* [Machine Learning - K-means](./machine-learning/tensorflow-mnist): Iris classification using streaming k-means and **Apache Flink**
* [Kinesis](./kinesis): How to stream changes from MySQL database running on a local machine to an Amazon [Kinesis](https://aws.amazon.com/kinesis/data-streams/) stream
* [KSQL](./ksql): Querying Debezium change data events with KSQL
* [Postgres - Failover slots](./postgres-failover-slots): How to use Postgres 17 failover replication slots with Debezium
* [Postgres - Kafka signal](./postgres-kafka-signal): How to stream data into the signal topic
* [Postgres - TOAST Column Values](./postgres-toast): Dealing With Postgres TOAST Column Values
* [SQL Server - Replication](./sql-server-read-replica): How to deploy the topology of services to stream from SQL Server read-only replica
* [MongoDB - Streaming to PostgresSQL](./unwrap-mongodb-smt): How to capture events from a MongoDB database and stream them to a PostgresSQL database
* [MySQL - Streaming to PostgresSQL](./unwrap-smt): How to capture events from a MySQL database and stream them to a PostgresSQL database
* [Quarkus Native Image with Debezium](./quarkus-native): superfast CDC with Debezium and Quarkus